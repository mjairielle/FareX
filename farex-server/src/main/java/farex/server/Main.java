package farex.server;

import com.sun.net.httpserver.*;
import farex.shared.card.CardPayload;
import farex.shared.data.*;
import farex.shared.fare.*;
import farex.shared.plan.TripPlanner;
import farex.shared.reward.Rewards;
import farex.shared.txn.Txn;
import farex.shared.user.*;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.*;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.locks.ReentrantLock;
import java.util.zip.GZIPOutputStream;

/**
 * HTTP door to the shared Store. No business logic here, Store and TapDesk own it.
 * Requests run on a small thread pool, but only one at a time holds LOCK and touches the shared state, so Store
 * needs no locks of its own. The slow parts run outside it: reading the body, sending the reply, PayMongo and
 * password checks (outsideLock), so one slow phone or payment no longer holds up every tap.
 */
public final class Main {
    static final ReentrantLock LOCK = new ReentrantLock();

    /** Runs slow work that touches no shared state (network, password hashing) with LOCK released. */
    static <T> T outsideLock(Callable<T> slow) throws Exception {
        LOCK.unlock();
        try { return slow.call(); } finally { LOCK.lock(); }
    }

    static Store store;
    static Path dir;
    static TripPlanner planner;
    static Rewards rewards;
    static RouteBook book;
    static RouteShapes shapes;                               // road paths for the map; see FetchRouteShapes
    static final Map<String, User> tokens = new HashMap<>();
    static final Map<String, Long> issued = new HashMap<>();      // token to the time it was made
    static final long TOKEN_TTL_MS = 12L * 3600 * 1000;           // log in again after 12 hours
    static final Map<String, long[]> fails = new HashMap<>();     // key to {failed tries, first fail time}
    static final long LOCK_MS = 15L * 60 * 1000;
    static final int MAX_BODY = 64 * 1024;                        // biggest request body, forms are tiny
    // shortcut: behind a reverse proxy every client shows the proxy address, so the per IP limit is shared. Read X-Forwarded-For then.

    static User tokenUser(String token) {
        Long at = issued.get(token);
        if (at == null || System.currentTimeMillis() - at > TOKEN_TTL_MS) { tokens.remove(token); issued.remove(token); return null; }
        return tokens.get(token);
    }

    static boolean locked(String key, int max, long now) {
        long[] f = fails.get(key);
        return f != null && f[0] >= max && now - f[1] < LOCK_MS;
    }

    static void fail(String key, long now) {
        long[] f = fails.get(key);
        if (f == null || now - f[1] >= LOCK_MS) fails.put(key, new long[]{1, now}); else f[0]++;
        if (fails.size() > 10000) fails.values().removeIf(x -> now - x[1] >= LOCK_MS);
    }

    // A password check runs outside the lock, so its try is counted before (fail) and taken back when it was right
    static void unfail(String key) {
        long[] f = fails.get(key);
        if (f != null && f[0] > 0) f[0]--;
    }
    static final Map<String, TapDesk> desks = new HashMap<>();

    // ----- Email codes (OTP): sign-up and lost card prove the person owns the email -----
    record Otp(String code, long expires, int[] wrong) {}
    static final Map<String, Otp> otps = new HashMap<>();                 // "SIGNUP|email" -> the code sent
    static final Map<String, String[]> verified = new HashMap<>();        // pass -> {"SIGNUP|email", expires millis}
    static final long OTP_MS = 5L * 60 * 1000, PASS_MS = 15L * 60 * 1000;
    static final java.security.SecureRandom RANDOM = new java.security.SecureRandom();
    // Emails go out on their own thread, so a slow mail server never holds up taps
    static final java.util.concurrent.ExecutorService MAILER = java.util.concurrent.Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "farex-mail");
        t.setDaemon(true);
        return t;
    });

    static String otpKey(String purpose, String email) {
        if (!purpose.equals("SIGNUP") && !purpose.equals("LOST")) throw new IllegalArgumentException("Bad purpose");
        return purpose + "|" + email.trim().toLowerCase();
    }

    /** The verified-email pass from /otp/verify must match; consume = use it up (after the action worked). */
    static void checkPass(Map<String, String> p, String purpose, String email, boolean consume) {
        String pass = p.getOrDefault("otp", "");
        String[] v = verified.get(pass);
        if (v == null || !v[0].equals(otpKey(purpose, email)) || System.currentTimeMillis() > Long.parseLong(v[1])) {
            verified.remove(pass);
            throw new IllegalArgumentException("Email not verified. Request a new code.");
        }
        if (consume) verified.remove(pass);
    }
    static final String SUSPENDED_H = "userId,action,time";
    // shortcut: dev default key, set CARD_KEY in .env before any real card is written
    static final byte[] CARD_KEY = Env.get("CARD_KEY", "farex-dev-card-key").getBytes(java.nio.charset.StandardCharsets.UTF_8);
    static final String PENDING_H = "sessionId,accountId,cardUid,cents,time,method";

    interface Act { String run(User u, Map<String, String> p) throws Exception; }

    static String json(String... kv) {
        StringBuilder b = new StringBuilder("{");
        for (int i = 0; i < kv.length; i += 2) {
            if (i > 0) b.append(',');
            b.append('"').append(kv[i]).append("\":\"").append(kv[i + 1].replace("\\", "\\\\").replace("\"", "\\\"")).append('"');
        }
        return b.append('}').toString();
    }

    static Map<String, String> form(String s) {
        Map<String, String> m = new HashMap<>();
        for (String kv : s.split("&")) {
            int i = kv.indexOf('=');
            if (i > 0) m.put(URLDecoder.decode(kv.substring(0, i), StandardCharsets.UTF_8), URLDecoder.decode(kv.substring(i + 1), StandardCharsets.UTF_8));
        }
        return m;
    }

    /**
     * role null means public, "ANY" means any logged in user. A public route still sees who is
     * logged in when a token is sent (u is null otherwise), e.g. /plan quotes that passenger's fare.
     */
    static void route(HttpServer s, String path, String method, String role, Act act) {
        s.createContext(path, ex -> {
            int code = 200;
            String out;
            try {
                if (!ex.getRequestMethod().equals(method)) { code = 405; out = json("error", "use " + method); }
                else {
                    byte[] rawBytes = ex.getRequestBody().readNBytes(MAX_BODY + 1);
                    if (rawBytes.length > MAX_BODY) throw new IllegalArgumentException("Request too large");
                    String raw = new String(rawBytes, StandardCharsets.UTF_8);
                    String q = ex.getRequestURI().getRawQuery();
                    Map<String, String> p = form((q == null ? "" : q + "&") + raw);
                    p.put("_ip", ex.getRemoteAddress().getAddress().getHostAddress());   // set here so a client cannot fake it
                    String h = ex.getRequestHeaders().getFirst("Authorization");
                    p.put("_token", h != null && h.startsWith("Bearer ") ? h.substring(7) : "");
                    LOCK.lock();
                    try {
                        User u = h != null && h.startsWith("Bearer ") ? tokenUser(h.substring(7)) : null;
                        if (role != null && u == null) { code = 401; out = json("error", "Please log in again"); }
                        else if (role != null && !role.equals("ANY") && !role.equals(u.role())) { code = 403; out = json("error", "Not allowed for your account"); }
                        else if (u != null && suspended(u.getId())) { code = 403; out = json("error", "Account suspended"); }
                        else { fares(); out = act.run(u, p); }
                    } finally {
                        LOCK.unlock();
                    }
                }
            } catch (IllegalArgumentException | IllegalStateException e) { code = 400; out = json("error", String.valueOf(e.getMessage()));
            } catch (Throwable e) { code = 500; out = json("error", String.valueOf(e.getMessage())); }   // an Error too: the phone must get an answer
            byte[] b = out.getBytes(StandardCharsets.UTF_8);
            ex.getResponseHeaders().set("Content-Type", out.startsWith("{") ? "application/json" : out.startsWith("<!") ? "text/html; charset=utf-8" : "text/plain; charset=utf-8");
            String accept = ex.getRequestHeaders().getFirst("Accept-Encoding");
            if (b.length > 1024 && accept != null && accept.contains("gzip")) {
                b = gzip(path, out);
                ex.getResponseHeaders().set("Content-Encoding", "gzip");
            }
            ex.sendResponseHeaders(code, b.length);
            try (OutputStream os = ex.getResponseBody()) { os.write(b); }
        });
    }

    /**
     * Last row for the user wins: SUSPEND (admin, whole account), RETIRE (admin, one vehicle),
     * PENDING (new operator role waiting for approval), REINSTATE. "" = never listed.
     */
    static String status(String id) {
        for (String[] r : suspendedTail.poll(2, lastStatus::clear)) lastStatus.put(r[0], r[1]);
        return lastStatus.getOrDefault(id, "");
    }
    static TailFile suspendedTail;                                    // suspended.csv is append only: read new rows only
    static final Map<String, String> lastStatus = new HashMap<>();

    // The fare table is read again only when fares.csv changes, or a new day may start a published table
    static Object faresStamp;
    static void fares() {
        Object now = List.of(Csv.stamp(dir.resolve("fares.csv")), LocalDate.now(ZoneOffset.UTC));
        if (!now.equals(faresStamp)) { Fares.load(dir); faresStamp = now; }
    }

    // Big replies are sent gzipped (about 4x smaller). Cached replies are the same String each time, zipped once.
    static final Map<String, Object[]> ZIPPED = new ConcurrentHashMap<>();
    static byte[] gzip(String path, String out) throws IOException {
        Object[] c = ZIPPED.get(path);
        if (c != null && c[0] == out) return (byte[]) c[1];
        ByteArrayOutputStream b = new ByteArrayOutputStream();
        try (GZIPOutputStream z = new GZIPOutputStream(b)) { z.write(out.getBytes(StandardCharsets.UTF_8)); }
        ZIPPED.put(path, new Object[]{out, b.toByteArray()});
        return b.toByteArray();
    }

    // Built once, cleared when the admin adds a route: ~2 MB of text the map downloads on every app start
    static String routesReply, shapesReply, roadKmReply;

    static boolean suspended(String id) {
        return List.of("SUSPEND", "RETIRE", "PENDING").contains(status(id));
    }

    static String userJson(User n, String... more) {
        List<String> kv = new ArrayList<>(List.of("id", n.getId(), "name", n.getName(), "email", n.getEmail(), "role", n.role(),
                "passengerType", n instanceof Passenger x ? x.getPassengerType() : "",
                "plate", n instanceof Operator o ? o.getVehiclePlate() : "", "phone", store.phoneOf(n.getId())));
        kv.addAll(List.of(more));
        return json(kv.toArray(String[]::new));
    }

    static String clean(String s) { return s.replace('|', ' ').replace('\n', ' '); }

    static String req(Map<String, String> p, String k) {
        String v = p.get(k);
        if (v == null || v.isBlank()) throw new IllegalArgumentException(k + " is required");
        return v;
    }

    // A map label goes back inside the "|~;" plan lines, so those characters must not get through
    static String label(Map<String, String> p, String k, String dflt) {
        String v = clean(p.getOrDefault(k, "")).replaceAll("[~;]", " ").trim();
        return v.isEmpty() ? dflt : v.substring(0, Math.min(v.length(), 80));
    }

    // Route segments, plus the operator jeep's stops.csv route ("STOPS" rows in stops_km.csv), which the
    // OSM route import does not cover: its max fare would fall back to straight lines
    static void loadRoadKm() {
        Map<String, Double> km = new HashMap<>(RoadDistances.load(dir, book.routes()));
        km.putAll(RoadDistances.load(dir, "stops_km.csv", Map.of("STOPS", store.stops())));
        Fares.setRoadKm(km);
    }

    /**
     * Admin dashboard for the last `days` days, by Manila calendar day. Lines:
     *   K|key|value                              totals (money in centavos)
     *   D|yyyy-mm-dd|loadCents|fareCents|trips   one per day, oldest first
     *   A|time|Load or Fare|name|card|cents|detail  newest 15 loads and fares
     *   O|name|plate|fareCents|trips             top 5 operators by fares collected
     *   P|id|name|email|plate                    operators waiting for approval
     * Lost-card transfers, write-offs and reward credits are not counted as money in or out.
     */
    // Dashboard totals, updated from the ledger's new rows only, so the 30 s auto refresh never rereads the whole ledger
    static final ZoneId MANILA = ZoneId.of("Asia/Manila");
    static TailFile statsTail;
    static final Map<LocalDate, long[]> statsDay = new HashMap<>();              // Manila day -> {loadCents, loads, fareCents, trips}
    static final Map<LocalDate, Map<String, long[]>> statsOp = new HashMap<>();   // Manila day -> operator id -> {cents, trips}
    static final ArrayDeque<String[]> statsRecent = new ArrayDeque<>();           // newest 15 load and fare rows, newest first
    static long loadAll, fareAll;

    static void syncStats() {
        for (String[] r : statsTail.poll(8, () -> { statsDay.clear(); statsOp.clear(); statsRecent.clear(); loadAll = fareAll = 0; })) {
            try {
                String extra = r.length > 8 ? r[8] : "";
                boolean load = r[1].equals("CARD_LOAD") && !extra.equals("Replacement Transfer") && !extra.equals("Rewards");
                boolean fare = r[1].equals("FARE_PAYMENT") && !extra.equals("LOST_REPLACEMENT") && !extra.equals("ACCOUNT_DELETED");
                if (!load && !fare) continue;
                long cents = Long.parseLong(r[4]);
                LocalDate day = Instant.parse(r[7]).atZone(MANILA).toLocalDate();
                long[] d = statsDay.computeIfAbsent(day, k -> new long[4]);
                if (load) { loadAll += cents; d[0] += cents; d[1]++; }
                else {
                    fareAll += cents;
                    d[2] += cents;
                    d[3]++;
                    long[] op = statsOp.computeIfAbsent(day, k -> new HashMap<>())
                            .computeIfAbsent(extra, k -> new long[2]);
                    op[0] += cents;
                    op[1]++;
                }
                statsRecent.addFirst(r);
                if (statsRecent.size() > 15) statsRecent.removeLast();
            } catch (RuntimeException badRow) {
                // a damaged ledger row is skipped here, as everywhere else
            }
        }
    }

    static String stats(int days) {
        days = Math.max(1, Math.min(90, days));
        LocalDate first = LocalDate.now(MANILA).minusDays(days - 1);

        Map<String, String[]> users = new HashMap<>();
        int passengers = 0, operators = 0, suspendedN = 0;
        StringBuilder out = new StringBuilder(), waiting = new StringBuilder();
        for (String[] r : Csv.read(dir.resolve("users.csv"), 7)) {
            users.put(r[0], r);
            if (r[4].equals("PASSENGER")) passengers++;
            if (r[4].equals("OPERATOR")) operators++;
            String st = status(r[0]);
            if (st.equals("PENDING")) waiting.append(String.join("|", "P", clean(r[0]), clean(r[1]), clean(r[2]), clean(r[6]),
                    r.length > 8 && !r[8].isEmpty() ? clean(book.name(r[8])) : "")).append('\n');
            else if (st.equals("SUSPEND") || st.equals("RETIRE")) suspendedN++;
        }

        syncStats();
        long[] loadDay = new long[days], fareDay = new long[days];
        int[] tripDay = new int[days];
        int loads = 0;
        Map<String, long[]> byOperator = new HashMap<>();             // operator id -> {cents, trips}
        for (int i = 0; i < days; i++) {
            long[] d = statsDay.get(first.plusDays(i));
            if (d != null) { loadDay[i] = d[0]; loads += (int) d[1]; fareDay[i] = d[2]; tripDay[i] = (int) d[3]; }
            statsOp.getOrDefault(first.plusDays(i), Map.of()).forEach((op, v) -> {
                long[] sum = byOperator.computeIfAbsent(op, k -> new long[2]);
                sum[0] += v[0];
                sum[1] += v[1];
            });
        }
        List<String> recent = new ArrayList<>();
        for (String[] r : statsRecent) {
            boolean load = r[1].equals("CARD_LOAD");
            String extra = r.length > 8 ? r[8] : "";
            String who = users.containsKey(r[2]) ? users.get(r[2])[1] : "(deleted account)";
            recent.add(String.join("|", "A", r[7], load ? "Load" : "Fare", clean(who), clean(r[3]), r[4],
                    load ? clean(extra) : (r.length > 9 ? clean(r[9]) + " km" : "")));
        }
        long loadP = 0, fareP = 0;
        int trips = 0;
        for (int i = 0; i < days; i++) { loadP += loadDay[i]; fareP += fareDay[i]; trips += tripDay[i]; }

        long earned = 0, redeemed = 0;
        for (String[] r : Csv.read(dir.resolve("rewards.csv"), 5)) {
            try {
                if (Instant.parse(r[1]).atZone(MANILA).toLocalDate().isBefore(first)) continue;
                long pts = Long.parseLong(r[4]);
                if (r[3].equals("EARN")) earned += pts;
                if (r[3].equals("REDEEM")) redeemed -= pts;
                if (r[3].equals("REFUND")) redeemed -= pts;
            } catch (RuntimeException badRow) { }
        }

        String[][] totals = {{"passengers", "" + passengers}, {"operators", "" + operators}, {"pending", "" + waiting.toString().lines().count()},
                {"suspended", "" + suspendedN}, {"cards", "" + store.allCards().size()}, {"loadCents", "" + loadP}, {"loads", "" + loads},
                {"fareCents", "" + fareP}, {"trips", "" + trips}, {"loadAll", "" + loadAll}, {"fareAll", "" + fareAll},
                {"pointsEarned", "" + earned}, {"pointsRedeemed", "" + redeemed}, {"days", "" + days}};
        for (String[] t : totals) out.append("K|").append(t[0]).append('|').append(t[1]).append('\n');
        for (int i = 0; i < days; i++)
            out.append("D|").append(first.plusDays(i)).append('|').append(loadDay[i]).append('|').append(fareDay[i]).append('|').append(tripDay[i]).append('\n');
        recent.forEach(a -> out.append(a).append('\n'));
        byOperator.entrySet().stream().sorted((a, b) -> Long.compare(b.getValue()[0], a.getValue()[0])).limit(5).forEach(e -> {
            String[] op = users.get(e.getKey());
            out.append(String.join("|", "O", op == null ? "(deleted operator)" : clean(op[1]), op == null ? "" : clean(op[6]),
                    "" + e.getValue()[0], "" + e.getValue()[1])).append('\n');
        });
        return out.append(waiting).toString();
    }

    static String backToApp(String title, String text) {
        return "<!doctype html><html><head><meta charset=\"utf-8\"><meta name=\"viewport\" content=\"width=device-width,initial-scale=1\">"
                + "<title>FareX</title></head><body style=\"font-family:system-ui,sans-serif;background:#111;color:#eee;text-align:center;padding:48px 20px\">"
                + "<h1 style=\"color:#c6ff3d\">" + title + "</h1><p>" + text + "</p></body></html>";
    }

    // The ledger row id of a PayMongo load, from its checkout session id: the ledger itself says if it was credited
    static String loadRowId(String sessionId) {
        return UUID.nameUUIDFromBytes(("load:" + sessionId).getBytes(StandardCharsets.UTF_8)).toString();
    }

    // After an admin changes routes: the planner, fares on road km and the app's cached route replies see it at once
    static void reloadRoutes() {
        book = new RouteBook(dir);
        shapes = new RouteShapes(dir);
        loadRoadKm();
        planner = new TripPlanner(book, shapes);
        routesReply = shapesReply = roadKmReply = null;
        desks.clear();                                     // operators on an edited or deleted route get its new stops
    }

    static double num(Map<String, String> p, String k) {
        try { return Double.parseDouble(req(p, k)); } catch (NumberFormatException e) { throw new IllegalArgumentException(k + " must be a number"); }
    }

    static long cents(Map<String, String> p) {
        try { return Long.parseLong(req(p, "cents")); } catch (NumberFormatException e) { throw new IllegalArgumentException("bad amount"); }
    }

    /** A passenger may touch only their own cards. Operators may read any card, as the tap screen needs. */
    static void mayUse(User u, String uid) {
        if (u.role().equals("PASSENGER") && !u.getId().equals(store.ownerOf(uid))) throw new IllegalArgumentException("Card is not yours");
    }

    static final String JEEP_QR = "FAREX:JEEP:";                     // + operator id: what the jeep's QR holds
    static final Map<String, Integer> jeepStop = new ConcurrentHashMap<>();   // operator id -> stop the jeep is at, while on shift

    static TapDesk desk(User op) { return desks.computeIfAbsent(op.getId(), id -> new TapDesk(store, id, routeStopsOf(id))); }

    // The stops an operator's jeep runs: the route they chose, or the default line (none chosen, or that route was deleted)
    static List<RouteStop> routeStopsOf(String operatorId) {
        List<RouteStop> r = book.routes().get(store.routeOf(operatorId));
        return r != null && r.size() >= 2 ? r : store.stops();
    }

    // An operator's route from a form: "" (none) or a route id that exists. Passengers have none.
    static String routeParam(String role, Map<String, String> p) {
        String r = p.getOrDefault("route", "").trim();
        if (!role.equals("OPERATOR")) return "";
        if (!r.isEmpty() && !book.routes().containsKey(r)) throw new IllegalArgumentException("Unknown route " + r);
        return r;
    }

    /** Finishing a trip also earns reward points. Trip id is the txn uuid, so a retry cannot earn twice. */
    static String tapResult(Txn t, TapDesk d, String... extra) {
        String[] row = t.toRow();
        long pts = rewards.earn(t.getAccountId(), row[0], Double.parseDouble(row[9]), Instant.now());
        List<String> kv = new ArrayList<>(List.of("describe", t.describe(), "balance", String.valueOf(t.getBalanceAfter()), "onBoard", String.valueOf(d.onBoard()),
                "points", String.valueOf(pts), "fare", String.valueOf(t.getAmount()), "id", row[0]));
        kv.addAll(List.of(extra));
        return json(kv.toArray(new String[0]));
    }

    public static void main(String[] a) throws Exception {
        dir = Path.of(Env.get("DATA_DIR", "data"));
        suspendedTail = new TailFile(dir.resolve("suspended.csv"));
        statsTail = new TailFile(dir.resolve("ledger.csv"));
        boolean dev = "1".equals(Env.get("DEV", "0"));
        String adminPw = Env.get("ADMIN_PASSWORD", dev ? "admin123" : null);
        if (!dev && Pay.FAKE) throw new IllegalStateException("FAKE_PAY gives free money, it needs DEV=1");
        if (!java.nio.file.Files.exists(dir.resolve("users.csv")) && (adminPw == null || adminPw.length() < 8))
            throw new IllegalStateException("First run: set ADMIN_PASSWORD (8+ characters) in .env, or DEV=1 for a demo");
        store = new Store(dir, adminPw, dev);
        // ADMIN_PASSWORD in .env is always the admin login, set on every start (demo default admin123)
        if (adminPw != null) store.setPassword("a1", adminPw);
        if (dev) System.out.println("Admin page: http://localhost:" + Env.get("PORT", "8080") + "/admin  login admin@mail.com / " + adminPw);
        else System.out.println("Admin page: http://localhost:" + Env.get("PORT", "8080") + "/admin  login admin@mail.com, password from "
                + Env.source("ADMIN_PASSWORD") + (adminPw == null ? "" : " (" + adminPw.length() + " characters)"));
        if (!dev) {
            if (store.hasDefaultLogins()) throw new IllegalStateException("Demo passwords (1234, admin123) only run with DEV=1 in .env. Public run: change them or delete those rows in data/users.csv");
            if (Env.get("CARD_KEY", null) == null) throw new IllegalStateException("CARD_KEY is required");
        }
        book = new RouteBook(dir);
        shapes = new RouteShapes(dir);
        loadRoadKm();                                              // fares on road km where known
        planner = new TripPlanner(book, shapes);
        rewards = new Rewards(dir);

        int port = Integer.parseInt(Env.get("PORT", "8080"));
        HttpServer s;
        try {
            s = HttpServer.create(new InetSocketAddress(port), 0);
        } catch (java.net.BindException e) {
            throw new IllegalStateException("Port " + port + " is busy: another FareX server is still running. Close it first.");
        }
        s.setExecutor(Executors.newFixedThreadPool(8));

        // Sends a 6-digit code to the email. SIGNUP: the email typed at sign-up; LOST: the logged-in account's email.
        // Without SMTP settings the code is printed in this server window instead (laptop demo).
        route(s, "/otp/send", "POST", null, (u, p) -> {
            String purpose = req(p, "purpose").toUpperCase();
            if (purpose.equals("LOST") && u == null) throw new IllegalStateException("Please log in again");
            String email = purpose.equals("LOST") ? u.getEmail() : req(p, "email").trim().toLowerCase();
            if (!email.matches("[^\\s,<>@]+@[^\\s,<>@]+")) throw new IllegalArgumentException("Email is not valid");
            long now = System.currentTimeMillis();
            String key = otpKey(purpose, email), ipKey = "otp|" + p.get("_ip");
            Otp last = otps.get(key);
            if (last != null && now < last.expires() - OTP_MS + 30_000) throw new IllegalStateException("Wait 30 seconds before asking for another code");
            if (locked(ipKey, 20, now)) throw new IllegalStateException("Too many codes requested. Wait 15 minutes");
            fail(ipKey, now);                                        // counts sends: 20 per IP per 15 minutes
            String code = String.format("%06d", RANDOM.nextInt(1_000_000));
            otps.put(key, new Otp(code, now + OTP_MS, new int[1]));
            if (Mail.configured()) {
                String to = email;
                MAILER.submit(() -> {
                    try {
                        Mail.send(to, "Your FareX code: " + code, "Your FareX verification code is " + code
                                + ".\nIt expires in 5 minutes. Never share it; FareX will never ask for it.\n\nIf you did not ask for this code, ignore this email.");
                    } catch (Exception e) {
                        System.err.println("Could not email the code to " + to + ": " + e.getMessage());
                    }
                });
            } else {
                System.out.println("OTP for " + email + ": " + code + "  (SMTP_USER and SMTP_PASSWORD not set, so it was not emailed)");
            }
            return json("sent", "1");
        });
        // Right code -> a one-time pass for 15 minutes that /register or /cards/freeze needs. 5 wrong tries kill the code.
        route(s, "/otp/verify", "POST", null, (u, p) -> {
            String purpose = req(p, "purpose").toUpperCase();
            if (purpose.equals("LOST") && u == null) throw new IllegalStateException("Please log in again");
            String key = otpKey(purpose, purpose.equals("LOST") ? u.getEmail() : req(p, "email"));
            Otp o = otps.get(key);
            if (o == null || System.currentTimeMillis() > o.expires()) { otps.remove(key); throw new IllegalArgumentException("Code expired. Tap Resend code."); }
            if (!java.security.MessageDigest.isEqual(o.code().getBytes(StandardCharsets.UTF_8), req(p, "code").trim().getBytes(StandardCharsets.UTF_8))) {
                if (++o.wrong()[0] >= 5) { otps.remove(key); throw new IllegalArgumentException("Too many wrong codes. Tap Resend code."); }
                throw new IllegalArgumentException("Wrong code. Try again.");
            }
            otps.remove(key);
            String pass = UUID.randomUUID().toString();
            if (verified.size() > 5000) verified.values().removeIf(v -> System.currentTimeMillis() > Long.parseLong(v[1]));
            verified.put(pass, new String[]{key, String.valueOf(System.currentTimeMillis() + PASS_MS)});
            return json("otp", pass);
        });
        route(s, "/register", "POST", null, (u, p) -> {
            String role = req(p, "role").toUpperCase();
            if (!role.equals("PASSENGER") && !role.equals("OPERATOR")) throw new IllegalArgumentException("Bad role");
            String type = p.getOrDefault("passengerType", "REGULAR").toUpperCase();
            if (!List.of("REGULAR", "STUDENT", "SENIOR", "PWD").contains(type)) type = "REGULAR";
            String phone = p.getOrDefault("phone", "").trim();
            if (!phone.isEmpty() && !phone.matches("09\\d{9}")) throw new IllegalArgumentException("Phone must be 11 digits starting with 09");
            checkPass(p, "SIGNUP", req(p, "email"), false);                  // proves the email is theirs
            User n = store.register(req(p, "name"), req(p, "email"), req(p, "password"), role, type, p.getOrDefault("plate", ""), routeParam(role, p));
            checkPass(p, "SIGNUP", req(p, "email"), true);
            if (!phone.isEmpty()) store.setPhone(n.getId(), phone);
            // An operator can write fare rows, so admin must approve first. Admin page Reinstate lets them in.
            if (role.equals("OPERATOR")) Csv.append(dir.resolve("suspended.csv"), SUSPENDED_H, n.getId(), "PENDING", Instant.now().toString());
            return json("id", n.getId(), "status", role.equals("OPERATOR") ? "pending" : "active");
        });
        route(s, "/login", "POST", null, (u, p) -> {
            long now = System.currentTimeMillis();
            String who = req(p, "email").trim().toLowerCase() + "|" + p.get("_ip"), anyone = "*|" + p.get("_ip");
            if (locked(who, 5, now) || locked(anyone, 30, now)) throw new IllegalStateException("Too many tries. Wait 15 minutes");
            fail(who, now);
            fail(anyone, now);
            List<User> all = outsideLock(() -> store.loginAll(req(p, "email"), req(p, "password")));   // ~0.2 s of hashing
            if (all.isEmpty()) throw new IllegalArgumentException("Wrong email or password");
            fails.remove(who);
            unfail(anyone);
            // roles = every role of the account, pending = those not usable yet. The token starts on the first usable one.
            List<String> roles = new ArrayList<>(), pending = new ArrayList<>();
            User n = null;
            for (User x : all) {
                roles.add(x.role());
                if (suspended(x.getId())) pending.add(x.role());
                else if (n == null) n = x;
            }
            if (n == null) throw new IllegalArgumentException(all.size() == 1 && status(all.get(0).getId()).equals("PENDING")
                    ? "Your operator account is waiting for admin approval" : "Account suspended. Contact FareX support");
            String t = UUID.randomUUID().toString();
            if (tokens.size() > 5000) tokens.keySet().removeIf(k -> tokenUser(k) == null);
            tokens.put(t, n);
            issued.put(t, now);
            return userJson(n, "token", t, "roles", String.join(",", roles), "pending", String.join(",", pending));
        });
        // Same login, other role of the account: no password again. The token now acts as that role.
        route(s, "/role/switch", "POST", "ANY", (u, p) -> {
            String want = req(p, "role").toUpperCase();
            User t = store.siblings(u.getId()).stream().filter(x -> x.role().equals(want)).findFirst()
                    .orElseThrow(() -> new IllegalArgumentException("This account has no " + want.toLowerCase() + " role"));
            if (suspended(t.getId())) throw new IllegalArgumentException(status(t.getId()).equals("PENDING")
                    ? "Your operator role is waiting for admin approval" : "This role is suspended");
            tokens.put(p.get("_token"), t);
            return userJson(t);
        });
        // A passenger becomes an operator too (admin approves first), or an operator becomes a passenger (no approval).
        route(s, "/role/add", "POST", "ANY", (u, p) -> {
            String role = req(p, "role").toUpperCase();
            User n = store.addRole(u.getId(), role, p.getOrDefault("plate", ""), routeParam(role, p));
            if (role.equals("OPERATOR")) Csv.append(dir.resolve("suspended.csv"), SUSPENDED_H, n.getId(), "PENDING", Instant.now().toString());
            return json("status", role.equals("OPERATOR") ? "pending" : "active");
        });

        route(s, "/profile/update", "POST", "ANY", (u, p) -> {
            String name = req(p, "name");
            store.updateUserName(u.getId(), name);
            if (p.containsKey("phone")) store.setPhone(u.getId(), p.get("phone"));
            tokens.replaceAll((t, x) -> x.getId().equals(u.getId()) ? store.findUser(u.getId()).orElse(x) : x);
            return json("name", name);
        });
        // Needs the current password; wrong guesses count like login, so a stolen token cannot brute force it
        route(s, "/profile/password", "POST", "ANY", (u, p) -> {
            long now = System.currentTimeMillis();
            String key = "pw|" + u.getId();
            if (locked(key, 5, now)) throw new IllegalStateException("Too many tries. Wait 15 minutes");
            fail(key, now);
            if (outsideLock(() -> store.loginAll(u.getEmail(), req(p, "current"))).isEmpty()) throw new IllegalArgumentException("Your current password is not correct.");
            unfail(key);
            store.setPassword(u.getId(), req(p, "password"));
            return json("ok", "1");
        });
        // "uid,cents" per active card, then "uid,cents,frozen" for frozen ones that still hold money, so a lost-card
        // transfer can be finished later. The balance comes along so the app needs no /balance call per card.
        route(s, "/cards", "GET", "PASSENGER", (u, p) -> {
            List<String> out = new ArrayList<>();
            for (String uid : store.cardsOf(u.getId())) out.add(uid + "," + store.balance(uid));
            for (String uid : store.allCardsOf(u.getId()))
                if (store.isCardFrozen(uid) && store.balance(uid) > 0) out.add(uid + "," + store.balance(uid) + ",frozen");
            return String.join("\n", out);
        });
        // Deletes the whole account (every role); the card balance is written off. Needs the password.
        route(s, "/profile/delete", "POST", "ANY", (u, p) -> {
            long now = System.currentTimeMillis();
            String key = "pw|" + u.getId();
            if (locked(key, 5, now)) throw new IllegalStateException("Too many tries. Wait 15 minutes");
            fail(key, now);
            if (outsideLock(() -> store.loginAll(u.getEmail(), req(p, "password"))).isEmpty()) throw new IllegalArgumentException("Your password is not correct.");
            unfail(key);
            store.deleteAccount(u.getId());
            tokens.values().removeIf(x -> x.getEmail().equals(u.getEmail()));
            return json("ok", "1");
        });
        route(s, "/cards/add", "POST", "PASSENGER", (u, p) -> json("uid", store.addCard(req(p, "uid"), u.getId())));
        // Report lost: the card stops paying at once; the balance moves later with /cards/lost
        route(s, "/cards/freeze", "POST", "PASSENGER", (u, p) -> {
            String uid = req(p, "uid");
            mayUse(u, uid);
            checkPass(p, "LOST", u.getEmail(), true);                          // the owner confirmed by email code
            store.freezeCard(uid);
            return json("ok", "1");
        });
        route(s, "/cards/lost", "POST", "PASSENGER", (u, p) -> {
            String oldUid = req(p, "oldUid");
            String newTagUid = req(p, "newUid");
            String newUid = store.replaceLostCard(oldUid, newTagUid, u.getId());
            return json("newUid", newUid, "cents", String.valueOf(store.balance(newUid)));
        });
        // "routeId|stop~lat~lng;stop~lat~lng;...|display name" per line (older apps read the first two fields)
        route(s, "/routes", "GET", null, (u, p) -> {
            if (routesReply != null) return routesReply;
            StringBuilder b = new StringBuilder();
            book.routes().forEach((id, stops) -> {
                b.append(clean(id)).append('|');
                List<String> stopStrs = new ArrayList<>();
                for (RouteStop st : stops) stopStrs.add(st.name() + "~" + st.lat() + "~" + st.lng());
                b.append(String.join(";", stopStrs)).append('|').append(clean(book.name(id))).append('\n');
            });
            return routesReply = b.toString();
        });
        // Road path per segment (stop i -> i+1): "routeId|segment|lat~lng;lat~lng;..." per line
        route(s, "/route-shapes", "GET", null, (u, p) -> {
            if (shapesReply != null) return shapesReply;
            StringBuilder b = new StringBuilder();
            shapes.all().forEach((id, segs) -> segs.forEach((seg, pts) -> {
                b.append(clean(id)).append('|').append(seg).append('|');
                for (int i = 0; i < pts.size(); i++) {
                    if (i > 0) b.append(';');
                    b.append(String.format(Locale.ROOT, "%.6f~%.6f", pts.get(i).lat(), pts.get(i).lng()));
                }
                b.append('\n');
            }));
            return shapesReply = b.toString();
        });
        // Road km between consecutive stops, so the app's fare tables match what is charged:
        // "fromLat,fromLng>toLat,toLng|km" per line (RoadDistances.key)
        route(s, "/road-km", "GET", null, (u, p) -> {
            if (roadKmReply != null) return roadKmReply;
            StringBuilder b = new StringBuilder();
            Fares.roadKm().forEach((k, km) -> b.append(k).append('|').append(String.format(Locale.ROOT, "%.3f", km)).append('\n'));
            return roadKmReply = b.toString();
        });
        route(s, "/vehicles", "GET", "ANY", (u, p) -> {
            StringBuilder b = new StringBuilder();
            for (String[] v : store.allVehicles()) b.append(String.join("|", v)).append('\n');
            return b.toString();
        });
        route(s, "/fares/current", "GET", null, (u, p) -> json("base", String.valueOf(Fares.BASE), "rate", String.valueOf(Fares.RATE_PER_KM), "baseKm", String.valueOf(Fares.BASE_KM)));
        route(s, "/cards/all", "GET", "OPERATOR", (u, p) -> {
            StringBuilder b = new StringBuilder();
            // uid,passengerType,name,cents: the tap screen shows the rider's real discount and balance in one call
            // (names never hold a comma, Csv.clean removes them)
            for (String uid : store.allCards()) {
                User owner = store.findUser(store.ownerOf(uid)).orElse(null);
                b.append(uid).append(',').append(owner instanceof Passenger x ? x.getPassengerType() : "REGULAR")
                        .append(',').append(owner == null ? "?" : owner.getName()).append(',').append(store.balance(uid)).append('\n');
            }
            return b.toString();
        });
        route(s, "/balance", "GET", "ANY", (u, p) -> { mayUse(u, req(p, "uid")); return json("cents", String.valueOf(store.balance(p.get("uid")))); });
        route(s, "/history", "GET", "PASSENGER", (u, p) -> {
            StringBuilder b = new StringBuilder();
            for (Txn t : store.history(u.getId())) b.append(String.join(",", t.toRow())).append('\n');
            return b.toString();
        });
        route(s, "/stops", "GET", "ANY", (u, p) -> {
            StringBuilder b = new StringBuilder();
            for (RouteStop r : store.stops()) b.append(r.name()).append(',').append(r.lat()).append(',').append(r.lng()).append('\n');
            return b.toString();
        });

        route(s, "/rewards", "GET", "PASSENGER", (u, p) ->
                json("points", String.valueOf(rewards.balance(u.getId())), "multiplier", String.valueOf(rewards.multiplier(Instant.now()))));
        route(s, "/rewards/perks", "GET", "ANY", (u, p) -> {
            StringBuilder b = new StringBuilder();
            for (Rewards.Perk k : Rewards.PERKS) b.append(k.id()).append('|').append(k.name()).append('|').append(k.points()).append('|').append(k.cents()).append('\n');
            return b.toString();
        });
        // card credit first, points deducted after, so a failed credit never costs points
        route(s, "/rewards/redeem", "POST", "PASSENGER", (u, p) -> {
            String uid = req(p, "uid");
            mayUse(u, uid);
            Rewards.Perk k = rewards.perk(req(p, "perk"));
            rewards.check(u.getId(), k);
            rewards.spend(u.getId(), k, Instant.now());                 // points first, a crash then never gives free money
            try {
                store.load(uid, u.getId(), k.cents(), "Rewards");
            } catch (RuntimeException e) {
                rewards.refund(u.getId(), k, Instant.now());            // card refused the credit, points come back
                throw e;
            }
            return json("points", String.valueOf(rewards.balance(u.getId())), "cents", String.valueOf(store.balance(uid)));
        });

        // Routes, stops, fares and trip plans are public transit information: no login needed.
        // A logged-in passenger is still quoted their own (discounted) fare; anyone else the regular one.
        route(s, "/places", "GET", null, (u, p) -> String.join("\n", book.places()));
        // Jeeps passing a point (lat, lng): routes whose road is within TripPlanner.PASSING_M, nearest first.
        // "routeId|name|meters|lat~lng|firstStop|lastStop|oneway" per line; lat~lng = where its road passes closest
        route(s, "/passing", "GET", null, (u, p) -> {
            StringBuilder b = new StringBuilder();
            for (TripPlanner.Passing x : planner.passing(num(p, "lat"), num(p, "lng"), TripPlanner.PASSING_M)) {
                b.append(String.format(Locale.ROOT, "%s|%s|%d|%.6f~%.6f|%s|%s|%b", x.routeId(), clean(x.name()), x.meters(),
                        x.at().lat(), x.at().lng(), x.firstStop(), x.lastStop(), x.oneway())).append('\n');
            }
            return b.toString();
        });
        // Between two stops (from, to) or two points (fromLat, fromLng, toLat, toLng, optional fromLabel/toLabel).
        // line per plan: label|km|transfers|fareCents|minutes|route~from~to~km~fromLat,fromLng~toLat,toLng;...
        // (route WALK = walking). A ride can start or end at "the roadside near X": a jeep flagged down
        // on its road, not at a stop, hence the coordinates. Older apps read the first four fields.
        route(s, "/plan", "GET", null, (u, p) -> {
            FareRule rule = u instanceof Passenger x ? x.getFareRule() : new RegularFare();
            StringBuilder b = new StringBuilder();
            List<TripPlanner.Plan> plans = p.containsKey("fromLat")
                    ? planner.plan(num(p, "fromLat"), num(p, "fromLng"), label(p, "fromLabel", "Start"),
                                   num(p, "toLat"), num(p, "toLng"), label(p, "toLabel", "Destination"), rule)
                    : planner.plan(req(p, "from"), req(p, "to"), rule);
            for (TripPlanner.Plan pl : plans) {
                List<String> legs = new ArrayList<>();
                for (TripPlanner.Leg l : pl.legs()) legs.add(String.format(Locale.ROOT, "%s~%s~%s~%.2f~%.6f,%.6f~%.6f,%.6f",
                        l.route(), l.from(), l.to(), l.km(), l.fromAt().lat(), l.fromAt().lng(), l.toAt().lat(), l.toAt().lng()));
                b.append(String.format(Locale.ROOT, "%s|%.1f|%d|%d|%d|", pl.label(), pl.km(), pl.transfers(), pl.fareCents(), pl.minutes()))
                 .append(String.join(";", legs)).append('\n');
            }
            return b.toString();
        });

        route(s, "/load/start", "POST", "PASSENGER", (u, p) -> {
            String uid = req(p, "uid");
            long c = cents(p);
            mayUse(u, uid);
            if (c < 500 || c > 500000) throw new IllegalArgumentException("Amount must be P5.00 to P5,000.00");
            String[] r = outsideLock(() -> Pay.start(c / 100.0));
            Csv.append(dir.resolve("pending.csv"), PENDING_H, r[0], u.getId(), uid, String.valueOf(c),
                    Instant.now().toString(), clean(p.getOrDefault("method", "")));
            return json("sessionId", r[0], "checkoutUrl", r[1]);
        });
        // Loads paid at PayMongo and not fetched yet: "sessionId,cardUid,cents,method" per line. Fetch Load shows only
        // these, so a checkout left unpaid never appears. A payment seen paid is kept in paid.csv (not asked again).
        // shortcut: an unpaid checkout is asked about for 2 days, then dropped; one paid later than that needs the admin.
        route(s, "/load/paid", "GET", "PASSENGER", (u, p) -> {
            Set<String> seen = new HashSet<>(), done = new HashSet<>();
            Csv.read(dir.resolve("paid.csv"), 1).forEach(r -> seen.add(r[0]));
            Csv.read(dir.resolve("credited.csv"), 1).forEach(r -> done.add(r[0]));
            Instant since = Instant.now().minus(java.time.Duration.ofDays(2));
            StringBuilder b = new StringBuilder();
            for (String[] r : Csv.read(dir.resolve("pending.csv"), 4)) {
                if (!r[1].equals(u.getId()) || done.contains(r[0]) || store.hasTxn(loadRowId(r[0]))) continue;
                if (!seen.contains(r[0])) {
                    boolean recent;
                    try { recent = r.length > 4 && Instant.parse(r[4]).isAfter(since); } catch (RuntimeException old) { recent = false; }
                    boolean paid;
                    try { paid = recent && outsideLock(() -> Pay.paid(r[0])); } catch (Exception offline) { paid = false; }   // ask again next time
                    if (!paid) continue;
                    Csv.append(dir.resolve("paid.csv"), "sessionId", r[0]);
                    seen.add(r[0]);
                }
                b.append(String.join(",", r[0], r[2], r[3], r.length > 5 ? r[5] : "")).append('\n');
            }
            return b.toString();
        });
        // credited.csv remembers paid sessions, so confirming twice credits once
        route(s, "/load/confirm", "POST", "PASSENGER", (u, p) -> {
            String id = req(p, "sessionId");
            String[] pd = Csv.read(dir.resolve("pending.csv"), 4).stream().filter(r -> r[0].equals(id)).findFirst()
                    .orElseThrow(() -> new IllegalArgumentException("Unknown payment"));
            if (!pd[1].equals(u.getId())) throw new IllegalArgumentException("Unknown payment");
            // The ledger row id comes from the session id, so the ledger itself says if this payment was credited.
            // credited.csv is only read for payments made before this change.
            String rowId = loadRowId(id);
            if (store.hasTxn(rowId) || Csv.read(dir.resolve("credited.csv"), 1).stream().anyMatch(r -> r[0].equals(id)))
                return json("status", "paid", "cents", String.valueOf(store.balance(pd[2])));
            if (!outsideLock(() -> Pay.paid(id))) return json("status", "unpaid");
            // a second confirm may have credited it while PayMongo answered; store.load would refuse it, so report paid
            if (!store.hasTxn(rowId)) store.load(pd[2], u.getId(), Long.parseLong(pd[3]), "PayMongo", rowId);
            return json("status", "paid", "cents", String.valueOf(store.balance(pd[2])));
        });

        // Signed bytes the app writes to the card after a load, and the check an operator runs on a tap.
        route(s, "/card/payload", "GET", "ANY", (u, p) -> {
            String uid = req(p, "uid");
            mayUse(u, uid);
            byte[] sealed = CardPayload.seal(uid, store.balance(uid), store.txnCount(uid), CARD_KEY);
            return json("payload", HexFormat.of().formatHex(sealed));
        });
        route(s, "/card/verify", "POST", "OPERATOR", (u, p) -> {
            String uid = req(p, "uid");
            byte[] bytes;
            try { bytes = HexFormat.of().parseHex(req(p, "payload")); } catch (IllegalArgumentException e) { throw new IllegalArgumentException("bad payload"); }
            var d = CardPayload.open(uid, bytes, CARD_KEY);
            if (d.isEmpty()) return json("valid", "false", "status", "INVALID");
            // The server ledger is the balance. The chip is a sealed receipt of the last write, so older is normal and
            // only a chip that claims more than the server knows is wrong.
            long serverCents = store.balance(uid);
            int serverCounter = store.txnCount(uid);
            long chipCounter = d.get().counter();
            String status = chipCounter > serverCounter || (chipCounter == serverCounter && d.get().cents() != serverCents) ? "FORGED"
                    : chipCounter < serverCounter ? "STALE" : "CURRENT";
            return json("valid", "true", "status", status, "cents", String.valueOf(d.get().cents()), "counter", String.valueOf(chipCounter),
                    "serverCents", String.valueOf(serverCents), "serverCounter", String.valueOf(serverCounter));
        });

        route(s, "/tap/in", "POST", "OPERATOR", (u, p) -> {
            String cardUid = desk(u).resolveCard(req(p, "uid"));
            if (suspended(store.ownerOf(cardUid))) throw new IllegalStateException("Card owner account is suspended");
            TapDesk.BoardedInfo info = desk(u).tapIn(cardUid, Integer.parseInt(req(p, "stop")));
            return json("onBoard", String.valueOf(desk(u).onBoard()), "uid", info.cardUid(), "name", info.passengerName(), "stop", info.stopName());
        });
        route(s, "/tap/out", "POST", "OPERATOR", (u, p) -> tapResult(desk(u).tapOut(req(p, "uid"), Integer.parseInt(req(p, "stop"))), desk(u)));
        route(s, "/tap/missed", "POST", "OPERATOR", (u, p) -> tapResult(desk(u).missedTapOut(req(p, "uid")), desk(u)));
        // The route this operator's jeep runs: "#routeId|name" then "stop,lat,lng" per stop in riding order.
        // routeId "" = no route chosen: the default line.
        route(s, "/operator/route", "GET", "OPERATOR", (u, p) -> {
            String id = book.routes().containsKey(store.routeOf(u.getId())) ? store.routeOf(u.getId()) : "";
            StringBuilder b = new StringBuilder("#").append(clean(id)).append('|').append(id.isEmpty() ? "Default route" : clean(book.name(id))).append('\n');
            for (RouteStop x : desk(u).stops()) b.append(clean(x.name())).append(',').append(x.lat()).append(',').append(x.lng()).append('\n');
            return b.toString();
        });
        // The operator's phone tells where the jeep is (each stop change); -1 = shift over, QR scans are refused
        route(s, "/operator/stop", "POST", "OPERATOR", (u, p) -> {
            int stop = Integer.parseInt(req(p, "stop"));
            if (stop < 0) jeepStop.remove(u.getId());
            else if (stop >= desk(u).stops().size()) throw new IllegalArgumentException("Invalid stop index");
            else jeepStop.put(u.getId(), stop);
            return json("ok", "1");
        });
        // QR pay: the passenger scans the jeep's QR ("FAREX:JEEP:<operatorId>") with one of their cards.
        // First scan boards at the jeep's current stop, the next one gets off there and pays, like a card tap in and out.
        // The stop comes from the operator's phone, never from the QR, so an old photo of the QR cannot fake a shorter ride.
        route(s, "/qr/pay", "POST", "PASSENGER", (u, p) -> {
            String qr = req(p, "qr").trim();
            if (!qr.startsWith(JEEP_QR)) throw new IllegalArgumentException("Not a FareX jeepney QR");
            User op = store.findUser(qr.substring(JEEP_QR.length())).filter(x -> x.role().equals("OPERATOR"))
                    .orElseThrow(() -> new IllegalArgumentException("Unknown jeepney QR"));
            if (suspended(op.getId())) throw new IllegalStateException("This jeepney cannot take fares right now");
            Integer stop = jeepStop.get(op.getId());
            if (stop == null || stop >= desk(op).stops().size()) throw new IllegalStateException("This jeepney is not on a shift");
            String uid = desk(op).resolveCard(req(p, "uid"));
            mayUse(u, uid);
            if (suspended(u.getId())) throw new IllegalStateException("Card owner account is suspended");
            String plate = op instanceof Operator o ? o.getVehiclePlate() : "";
            if (desk(op).boardedPassengers().stream().anyMatch(b -> b.cardUid().equals(uid)))
                return tapResult(desk(op).tapOut(uid, stop), desk(op), "action", "out", "plate", plate, "stop", desk(op).stops().get(stop).name());
            TapDesk.BoardedInfo info = desk(op).tapIn(uid, stop);
            return json("action", "in", "plate", plate, "stop", info.stopName(), "uid", info.cardUid(), "balance", String.valueOf(store.balance(uid)));
        });
        route(s, "/operator/boarded", "GET", "OPERATOR", (u, p) -> {
            StringBuilder b = new StringBuilder();
            for (TapDesk.BoardedInfo info : desk(u).boardedPassengers()) {
                b.append(info.cardUid()).append('|')
                 .append(info.passengerName()).append('|')
                 .append(info.stopIdx()).append('|')
                 .append(info.stopName()).append('|')
                 .append(info.time().toString()).append('\n');
            }
            return b.toString();
        });
        route(s, "/operator/history", "GET", "OPERATOR", (u, p) -> {
            StringBuilder b = new StringBuilder();
            for (Txn t : store.operatorHistory(u.getId())) b.append(String.join(",", t.toRow())).append('\n');
            return b.toString();
        });
        // Server is the only writer of balances. Every tap already wrote its ledger row, so an operator upload
        // never adds rows. shortcut: true offline taps need a server side replay check, build that before offline mode.
        route(s, "/sync/txns", "POST", "OPERATOR", (u, p) -> {
            int n = 0;
            for (String line : p.getOrDefault("batch", "").split("\n")) if (!line.isBlank()) n++;
            return json("accepted", "0", "skipped", String.valueOf(n));
        });

        route(s, "/admin", "GET", null, (u, p) -> AdminPage.HTML);
        // Where PayMongo sends the browser after checkout (PUBLIC_URL + /paid or /cancelled)
        route(s, "/paid", "GET", null, (u, p) -> backToApp("Payment received", "Go back to the FareX app and tap Fetch Load to add it to your card."));
        route(s, "/cancelled", "GET", null, (u, p) -> backToApp("Payment cancelled", "Nothing was charged. Go back to the FareX app to try again."));

        route(s, "/admin/api/users", "GET", "ADMIN", (u, p) -> {
            StringBuilder b = new StringBuilder();
            for (String[] r : Csv.read(dir.resolve("users.csv"), 7))
                b.append(String.join("|", clean(r[0]), clean(r[1]), clean(r[2]), r[4], clean(r[5]), clean(r[6]),
                        status(r[0]).equals("PENDING") ? "pending" : suspended(r[0]) ? "suspended" : "active", r.length > 7 ? clean(r[7]) : "",
                        r.length > 8 ? clean(r[8]) : "")).append('\n');
            return b.toString();
        });
        route(s, "/admin/api/user/type", "POST", "ADMIN", (u, p) -> {
            String id = req(p, "id");
            store.setPassengerType(id, req(p, "type"));
            tokens.replaceAll((t, x) -> x.getId().equals(id) ? store.findUser(id).orElse(x) : x);   // live sessions see the new discount
            return json("ok", "1");
        });
        route(s, "/admin/api/user/status", "POST", "ADMIN", (u, p) -> {
            User t = store.findUser(req(p, "id")).orElseThrow(() -> new IllegalArgumentException("No such user"));
            if (t.role().equals("ADMIN")) throw new IllegalArgumentException("Cannot suspend an admin");
            String act = req(p, "action").toUpperCase();
            if (!List.of("SUSPEND", "REINSTATE", "RETIRE").contains(act)) throw new IllegalArgumentException("Bad action");
            // SUSPEND blocks every role of the email; RETIRE only this operator row. REINSTATE lifts this row,
            // plus the account's other suspended rows, but never approves a pending operator or un-retires a jeep by accident.
            for (User x : act.equals("RETIRE") ? List.of(t) : store.siblings(t.getId())) {
                boolean self = x.getId().equals(t.getId());
                if (act.equals("REINSTATE") && !self && !status(x.getId()).equals("SUSPEND")) continue;
                Csv.append(dir.resolve("suspended.csv"), SUSPENDED_H, x.getId(), act, Instant.now().toString());
            }
            return json("ok", "1");
        });
        route(s, "/admin/api/routes", "GET", "ADMIN", (u, p) -> {
            StringBuilder b = new StringBuilder();
            // id|stop > stop > ...|display name
            book.routes().forEach((id, stops) -> b.append(clean(id)).append('|').append(String.join(" > ", stops.stream().map(x -> clean(x.name())).toList()))
                    .append('|').append(clean(book.name(id))).append('\n'));
            return b.toString();
        });
        // One route's stops for the edit form: "name,lat,lng" per line, then "#name|oneway"
        route(s, "/admin/api/route", "GET", "ADMIN", (u, p) -> {
            String id = req(p, "id");
            List<RouteStop> stops = book.routes().get(id);
            if (stops == null) throw new IllegalArgumentException("No route " + id);
            StringBuilder b = new StringBuilder();
            for (RouteStop x : stops) b.append(clean(x.name())).append(',').append(x.lat()).append(',').append(x.lng()).append('\n');
            return b.append('#').append(clean(book.name(id))).append('|').append(book.isOneway(id)).append('\n').toString();
        });
        // Add (new id) or replace a route. replace=1 is required to overwrite, so a typo in a new id never wipes a route.
        route(s, "/admin/api/route/add", "POST", "ADMIN", (u, p) -> {
            List<String[]> stops = new ArrayList<>();
            for (String line : req(p, "stops").split(";")) stops.add(line.split(",", -1));
            String id = req(p, "id").trim();
            if (!"1".equals(p.get("replace")) && book.routes().containsKey(id)) throw new IllegalArgumentException("Route " + id + " already exists");
            RouteBook.saveRoute(dir, id, p.getOrDefault("name", ""), "true".equals(p.get("oneway")), stops);
            reloadRoutes();
            return json("ok", "1");
        });
        route(s, "/admin/api/route/delete", "POST", "ADMIN", (u, p) -> {
            RouteBook.deleteRoute(dir, req(p, "id").trim());
            reloadRoutes();
            return json("ok", "1");
        });
        route(s, "/admin/api/fares", "GET", "ADMIN", (u, p) -> {
            StringBuilder b = new StringBuilder("Current|" + Fares.BASE + "|" + Fares.RATE_PER_KM + "|-|" + Fares.BASE_KM + "\n");
            for (String[] r : Csv.read(dir.resolve("fares.csv"))) b.append(String.join("|", r)).append('\n');
            return b.toString();
        });
        route(s, "/admin/api/fares/publish", "POST", "ADMIN", (u, p) -> {
            LocalDate eff;
            try { eff = LocalDate.parse(req(p, "effective")); } catch (java.time.format.DateTimeParseException e) { throw new IllegalArgumentException("Bad date"); }
            String baseKm = p.get("baseKm");                // optional; older clients keep the current band
            if (baseKm == null || baseKm.isBlank()) Fares.publish(dir, eff, Long.parseLong(req(p, "base")), Long.parseLong(req(p, "rate")));
            else Fares.publish(dir, eff, Long.parseLong(req(p, "base")), Long.parseLong(req(p, "rate")), Double.parseDouble(baseKm));
            return json("ok", "1");
        });
        route(s, "/admin/api/promos", "GET", "ADMIN", (u, p) -> {
            StringBuilder b = new StringBuilder();
            // name|multiplier|start|end|id (id last, so older pages reading 4 columns still work)
            for (Rewards.Promo x : rewards.promotions()) b.append(clean(x.name())).append('|').append(x.multiplier()).append('|').append(x.start()).append('|').append(x.end())
                    .append('|').append(clean(x.id())).append('\n');
            return b.toString();
        });
        // Creates a promotion, or edits the one with this id
        route(s, "/admin/api/promo", "POST", "ADMIN", (u, p) -> {
            LocalDate from, to;
            try { from = LocalDate.parse(req(p, "start")); to = LocalDate.parse(req(p, "end")); } catch (java.time.format.DateTimeParseException e) { throw new IllegalArgumentException("Bad date"); }
            double mult = num(p, "multiplier");
            Instant first = from.atStartOfDay().toInstant(ZoneOffset.UTC), last = to.plusDays(1).atStartOfDay().minusSeconds(1).toInstant(ZoneOffset.UTC);
            String id = p.getOrDefault("id", "");
            if (id.isBlank()) rewards.addPromotion(clean(req(p, "name")), mult, first, last);
            else rewards.updatePromotion(id, clean(req(p, "name")), mult, first, last);
            return json("ok", "1");
        });
        route(s, "/admin/api/promo/delete", "POST", "ADMIN", (u, p) -> {
            rewards.deletePromotion(req(p, "id"));
            return json("ok", "1");
        });
        route(s, "/admin/api/fares/delete", "POST", "ADMIN", (u, p) -> {
            Fares.delete(dir, req(p, "effective"), req(p, "published"));
            return json("ok", "1");
        });
        // Cards for the admin's Cards page: "uid|owner name|owner email|balance cents|frozen" per line
        route(s, "/admin/api/cards", "GET", "ADMIN", (u, p) -> {
            StringBuilder b = new StringBuilder();
            for (String uid : store.allCards()) {
                User owner = store.findUser(store.ownerOf(uid)).orElse(null);
                b.append(String.join("|", clean(uid), owner == null ? "?" : clean(owner.getName()), owner == null ? "" : clean(owner.getEmail()),
                        String.valueOf(store.balance(uid)), String.valueOf(store.isCardFrozen(uid)))).append('\n');
            }
            return b.toString();
        });
        // Cash load at the counter: the admin takes the money and adds it to the card (same ledger and limits as an
        // app load: P5 to P5,000, not on a frozen card). Shows as "Cash at admin" in the rider's history.
        route(s, "/admin/api/load", "POST", "ADMIN", (u, p) -> {
            String uid = req(p, "uid").trim();
            String owner = store.ownerOf(uid);
            if (owner == null) throw new IllegalArgumentException("No account has this card yet. The rider adds it in the app first.");
            store.load(uid, owner, cents(p), "Cash at admin");
            return json("uid", uid, "cents", String.valueOf(store.balance(uid)));
        });
        // Deletes the whole account (every role of the email) like /profile/delete; Store refuses an admin
        route(s, "/admin/api/user/delete", "POST", "ADMIN", (u, p) -> {
            User t = store.findUser(req(p, "id")).orElseThrow(() -> new IllegalArgumentException("No such user"));
            store.deleteAccount(t.getId());
            tokens.values().removeIf(x -> x.getEmail().equals(t.getEmail()));   // logged in phones are logged out
            return json("ok", "1");
        });
        // The card belongs to nobody again, so someone else can add it (a card linked to the wrong account)
        route(s, "/admin/api/card/unlink", "POST", "ADMIN", (u, p) -> {
            store.unlinkCard(req(p, "uid").trim());
            return json("ok", "1");
        });
        // Links a card to a passenger, found by email (a card nobody has, e.g. after an unlink or a new card at the counter)
        route(s, "/admin/api/card/add", "POST", "ADMIN", (u, p) -> {
            String email = req(p, "email").trim().toLowerCase();
            String owner = Csv.read(dir.resolve("users.csv"), 7).stream().filter(r -> r[2].equals(email) && r[4].equals("PASSENGER")).map(r -> r[0])
                    .findFirst().orElseThrow(() -> new IllegalArgumentException("No passenger account with email " + email));
            return json("uid", store.addCard(req(p, "uid"), owner));
        });
        // frozen=true stops the card riding (lost or stolen); false lets it ride again
        route(s, "/admin/api/card/freeze", "POST", "ADMIN", (u, p) -> {
            String uid = req(p, "uid").trim();
            if (store.ownerOf(uid) == null && !store.allCards().contains(uid)) throw new IllegalArgumentException("No card " + uid);
            if ("true".equals(p.get("frozen"))) store.freezeCard(uid); else store.unfreezeCard(uid);
            return json("ok", "1");
        });
        // Admin creates an account directly: no email code, and an operator is approved at once
        route(s, "/admin/api/user/add", "POST", "ADMIN", (u, p) -> {
            String role = req(p, "role");
            if (!role.equals("PASSENGER") && !role.equals("OPERATOR")) throw new IllegalArgumentException("Bad role");
            User n = store.register(req(p, "name"), req(p, "email"), req(p, "password"), role,
                    p.getOrDefault("type", "REGULAR"), p.getOrDefault("plate", ""), routeParam(role, p));
            return json("id", n.getId());
        });
        // Edits an account. Blank fields keep their value; name, email, phone and password cover every role of the email.
        route(s, "/admin/api/user/edit", "POST", "ADMIN", (u, p) -> {
            String id = req(p, "id");
            User t = store.findUser(id).orElseThrow(() -> new IllegalArgumentException("No such user"));
            if (t.role().equals("ADMIN")) throw new IllegalArgumentException("Edit the admin in .env (ADMIN_PASSWORD)");
            String name = p.getOrDefault("name", "").trim(), email = p.getOrDefault("email", "").trim(), plate = p.getOrDefault("plate", "").trim();
            String pw = p.getOrDefault("password", "");
            if (!pw.isEmpty() && pw.length() < 8) throw new IllegalArgumentException("Password needs 8 or more characters");
            if (!plate.isEmpty()) store.setPlate(id, plate);
            if (p.containsKey("route") && t.role().equals("OPERATOR")) {
                store.setRoute(id, routeParam("OPERATOR", p));
                desks.remove(id);                          // the jeep runs the new route's stops from its next tap
                jeepStop.remove(id);                       // its phone sends the stop again on the new route
            }
            if (!email.isEmpty()) store.setEmail(id, email);
            if (!name.isEmpty()) store.updateUserName(id, name);
            if (p.containsKey("phone")) store.setPhone(id, p.get("phone"));
            if (!pw.isEmpty()) store.setPassword(id, pw);
            // a new password or email logs the account's phones out; otherwise live sessions see the new details
            if (!pw.isEmpty() || !email.isEmpty()) tokens.values().removeIf(x -> x.getEmail().equals(t.getEmail()));
            else tokens.replaceAll((k, x) -> store.findUser(x.getId()).orElse(x));
            return json("ok", "1");
        });
        // Removes one role (a jeepney), or the whole account when it is the last role
        route(s, "/admin/api/vehicle/delete", "POST", "ADMIN", (u, p) -> {
            User t = store.findUser(req(p, "id")).orElseThrow(() -> new IllegalArgumentException("No such user"));
            if (!t.role().equals("OPERATOR")) throw new IllegalArgumentException("Not a vehicle");
            store.deleteRole(t.getId());
            tokens.values().removeIf(x -> x.getId().equals(t.getId()));
            return json("ok", "1");
        });
        route(s, "/admin/summary", "GET", "ADMIN", (u, p) -> json("summary", store.summary()));
        route(s, "/admin/api/stats", "GET", "ADMIN", (u, p) -> stats((int) num(p, "days")));

        s.start();
        System.out.println("FareX server on port " + Env.get("PORT", "8080") + (Pay.FAKE ? " (FAKE_PAY on)" : ""));
    }
}