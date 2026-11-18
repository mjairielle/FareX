package farex.server;

import com.sun.net.httpserver.*;
import farex.shared.card.CardPayload;
import farex.shared.data.*;
import farex.shared.fare.*;
import farex.shared.plan.TripPlanner;
import farex.shared.reward.Rewards;
import farex.shared.txn.Txn;
import farex.shared.user.*;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.*;

/**
 * HTTP door to the shared Store. No business logic here, Store and TapDesk own it.
 * shortcut: default single-thread executor serializes requests, so Store needs no locks. Upgrade past a demo.
 * shortcut: Store hashes passwords with plain SHA-256, fine for school, add salt before real users.
 */
public final class Main {
    static Store store;
    static Path dir;
    static TripPlanner planner;
    static Rewards rewards;
    static RouteBook book;
    static RouteShapes shapes;                               // road paths for the map; see FetchRouteShapes
    static final Map<String, User> tokens = new HashMap<>();
    static final Map<String, TapDesk> desks = new HashMap<>();
    static final String SUSPENDED_H = "userId,action,time";
    // shortcut: dev default key, set CARD_KEY in .env before any real card is written
    static final byte[] CARD_KEY = Env.get("CARD_KEY", "farex-dev-card-key").getBytes(java.nio.charset.StandardCharsets.UTF_8);
    static final String PENDING_H = "sessionId,accountId,cardUid,cents";

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

    /** role null means public, "ANY" means any logged in user. */
    static void route(HttpServer s, String path, String method, String role, Act act) {
        s.createContext(path, ex -> {
            int code = 200;
            String out;
            try {
                if (!ex.getRequestMethod().equals(method)) { code = 405; out = json("error", "use " + method); }
                else {
                    String raw = new String(ex.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
                    String q = ex.getRequestURI().getRawQuery();
                    Map<String, String> p = form((q == null ? "" : q + "&") + raw);
                    User u = null;
                    if (role != null) {
                        String h = ex.getRequestHeaders().getFirst("Authorization");
                        u = h != null && h.startsWith("Bearer ") ? tokens.get(h.substring(7)) : null;
                    }
                    if (role != null && u == null) { code = 401; out = json("error", "Please log in again"); }
                    else if (role != null && !role.equals("ANY") && !role.equals(u.role())) { code = 403; out = json("error", "Not allowed for your account"); }
                    else if (u != null && suspended(u.getId())) { code = 403; out = json("error", "Account suspended"); }
                    else { Fares.load(dir); out = act.run(u, p); }
                }
            } catch (IllegalArgumentException | IllegalStateException e) { code = 400; out = json("error", String.valueOf(e.getMessage()));
            } catch (Exception e) { code = 500; out = json("error", String.valueOf(e.getMessage())); }
            byte[] b = out.getBytes(StandardCharsets.UTF_8);
            ex.getResponseHeaders().set("Content-Type", out.startsWith("{") ? "application/json" : out.startsWith("<!") ? "text/html; charset=utf-8" : "text/plain; charset=utf-8");
            ex.sendResponseHeaders(code, b.length);
            try (OutputStream os = ex.getResponseBody()) { os.write(b); }
        });
    }

    /** Last SUSPEND or REINSTATE row for the user wins. */
    static boolean suspended(String id) {
        boolean s = false;
        for (String[] r : Csv.read(dir.resolve("suspended.csv"))) if (r[0].equals(id)) s = r[1].equals("SUSPEND");
        return s;
    }

    static String clean(String s) { return s.replace('|', ' ').replace('\n', ' '); }

    static String req(Map<String, String> p, String k) {
        String v = p.get(k);
        if (v == null || v.isBlank()) throw new IllegalArgumentException(k + " is required");
        return v;
    }

    static long cents(Map<String, String> p) {
        try { return Long.parseLong(req(p, "cents")); } catch (NumberFormatException e) { throw new IllegalArgumentException("bad amount"); }
    }

    /** A passenger may touch only their own cards. Operators may read any card, as the tap screen needs. */
    static void mayUse(User u, String uid) {
        if (u.role().equals("PASSENGER") && !u.getId().equals(store.ownerOf(uid))) throw new IllegalArgumentException("Card is not yours");
    }

    static TapDesk desk(User op) { return desks.computeIfAbsent(op.getId(), id -> new TapDesk(store, id)); }

    /** Finishing a trip also earns reward points. Trip id is the txn uuid, so a retry cannot earn twice. */
    static String tapResult(Txn t, TapDesk d) {
        String[] row = t.toRow();
        long pts = rewards.earn(t.getAccountId(), row[0], Double.parseDouble(row[9]), Instant.now());
        return json("describe", t.describe(), "balance", String.valueOf(t.getBalanceAfter()), "onBoard", String.valueOf(d.onBoard()), "points", String.valueOf(pts), "fare", String.valueOf(t.getAmount()));
    }

    public static void main(String[] a) throws Exception {
        dir = Path.of(Env.get("DATA_DIR", "data"));
        store = new Store(dir);
        book = new RouteBook(dir);
        shapes = new RouteShapes(dir);
        Fares.setRoadKm(RoadDistances.load(dir, book.routes()));   // fares on road km where known
        planner = new TripPlanner(book);
        rewards = new Rewards(dir);
        // shortcut: Store seeds admin@mail.com / admin123 on first run. Change it in data/users.csv by hand before a public demo.

        HttpServer s = HttpServer.create(new InetSocketAddress(Integer.parseInt(Env.get("PORT", "8080"))), 0);

        route(s, "/register", "POST", null, (u, p) -> {
            String role = req(p, "role").toUpperCase();
            if (!role.equals("PASSENGER") && !role.equals("OPERATOR")) throw new IllegalArgumentException("Bad role");
            String type = p.getOrDefault("passengerType", "REGULAR").toUpperCase();
            if (!List.of("REGULAR", "STUDENT", "SENIOR", "PWD").contains(type)) type = "REGULAR";
            User n = store.register(req(p, "name"), req(p, "email"), req(p, "password"), role, type, p.getOrDefault("plate", ""));
            return json("id", n.getId());
        });
        route(s, "/login", "POST", null, (u, p) -> {
            User n = store.login(req(p, "email"), req(p, "password")).orElseThrow(() -> new IllegalArgumentException("Wrong email or password"));
            String t = UUID.randomUUID().toString();
            if (suspended(n.getId())) throw new IllegalArgumentException("Account suspended");
            tokens.put(t, n);
            return json("token", t, "id", n.getId(), "name", n.getName(), "email", n.getEmail(), "role", n.role(),
                    "passengerType", n instanceof Passenger x ? x.getPassengerType() : "",
                    "plate", n instanceof Operator o ? o.getVehiclePlate() : "");
        });

        route(s, "/profile/update", "POST", "ANY", (u, p) -> {
            String name = req(p, "name");
            store.updateUserName(u.getId(), name);
            tokens.replaceAll((t, x) -> x.getId().equals(u.getId()) ? store.findUser(u.getId()).orElse(x) : x);
            return json("name", name);
        });
        route(s, "/cards", "GET", "PASSENGER", (u, p) -> String.join("\n", store.cardsOf(u.getId())));
        route(s, "/cards/add", "POST", "PASSENGER", (u, p) -> json("uid", store.addCard(req(p, "uid"), u.getId())));
        route(s, "/cards/lost", "POST", "PASSENGER", (u, p) -> {
            String oldUid = req(p, "oldUid");
            String newTagUid = req(p, "newUid");
            String newUid = store.replaceLostCard(oldUid, newTagUid, u.getId());
            return json("newUid", newUid, "cents", String.valueOf(store.balance(newUid)));
        });
        route(s, "/routes", "GET", "ANY", (u, p) -> {
            StringBuilder b = new StringBuilder();
            book.routes().forEach((id, stops) -> {
                b.append(clean(id)).append('|');
                List<String> stopStrs = new ArrayList<>();
                for (RouteStop st : stops) stopStrs.add(st.name() + "~" + st.lat() + "~" + st.lng());
                b.append(String.join(";", stopStrs)).append('\n');
            });
            return b.toString();
        });
        // Road path per segment (stop i -> i+1): "routeId|segment|lat~lng;lat~lng;..." per line
        route(s, "/route-shapes", "GET", "ANY", (u, p) -> {
            StringBuilder b = new StringBuilder();
            shapes.all().forEach((id, segs) -> segs.forEach((seg, pts) -> {
                b.append(clean(id)).append('|').append(seg).append('|');
                for (int i = 0; i < pts.size(); i++) {
                    if (i > 0) b.append(';');
                    b.append(String.format(Locale.ROOT, "%.6f~%.6f", pts.get(i).lat(), pts.get(i).lng()));
                }
                b.append('\n');
            }));
            return b.toString();
        });
        // Road km between consecutive stops, so the app's fare tables match what is charged:
        // "fromLat,fromLng>toLat,toLng|km" per line (RoadDistances.key)
        route(s, "/road-km", "GET", "ANY", (u, p) -> {
            StringBuilder b = new StringBuilder();
            Fares.roadKm().forEach((k, km) -> b.append(k).append('|').append(String.format(Locale.ROOT, "%.3f", km)).append('\n'));
            return b.toString();
        });
        route(s, "/vehicles", "GET", "ANY", (u, p) -> {
            StringBuilder b = new StringBuilder();
            for (String[] v : store.allVehicles()) b.append(String.join("|", v)).append('\n');
            return b.toString();
        });
        route(s, "/fares/current", "GET", "ANY", (u, p) -> json("base", String.valueOf(Fares.BASE), "rate", String.valueOf(Fares.RATE_PER_KM)));
        route(s, "/cards/all", "GET", "OPERATOR", (u, p) -> {
            StringBuilder b = new StringBuilder();
            for (String uid : store.allCards())
                b.append(uid).append(',').append(store.findUser(store.ownerOf(uid)).map(User::getName).orElse("?")).append('\n');
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
            store.load(uid, u.getId(), k.cents(), "Rewards");
            rewards.spend(u.getId(), k, Instant.now());
            return json("points", String.valueOf(rewards.balance(u.getId())), "cents", String.valueOf(store.balance(uid)));
        });

        route(s, "/places", "GET", "ANY", (u, p) -> String.join("\n", book.places()));
        // line per plan: label|km|transfers|fareCents|minutes|route~from~to~km;route~from~to~km
        route(s, "/plan", "GET", "ANY", (u, p) -> {
            FareRule rule = u instanceof Passenger x ? x.getFareRule() : new RegularFare();
            StringBuilder b = new StringBuilder();
            for (TripPlanner.Plan pl : planner.plan(req(p, "from"), req(p, "to"), rule)) {
                List<String> legs = new ArrayList<>();
                for (TripPlanner.Leg l : pl.legs()) legs.add(String.format(Locale.ROOT, "%s~%s~%s~%.1f", l.route(), l.from(), l.to(), l.km()));
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
            String[] r = Pay.start(c / 100.0);
            Csv.append(dir.resolve("pending.csv"), PENDING_H, r[0], u.getId(), uid, String.valueOf(c));
            return json("sessionId", r[0], "checkoutUrl", r[1]);
        });
        // credited.csv remembers paid sessions, so confirming twice credits once
        route(s, "/load/confirm", "POST", "PASSENGER", (u, p) -> {
            String id = req(p, "sessionId");
            String[] pd = Csv.read(dir.resolve("pending.csv")).stream().filter(r -> r[0].equals(id)).findFirst()
                    .orElseThrow(() -> new IllegalArgumentException("Unknown payment"));
            if (!pd[1].equals(u.getId())) throw new IllegalArgumentException("Unknown payment");
            if (Csv.read(dir.resolve("credited.csv")).stream().anyMatch(r -> r[0].equals(id)))
                return json("status", "paid", "cents", String.valueOf(store.balance(pd[2])));
            if (!Pay.paid(id)) return json("status", "unpaid");
            store.load(pd[2], u.getId(), Long.parseLong(pd[3]), "PayMongo");
            Csv.append(dir.resolve("credited.csv"), "sessionId", id);
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
            if (d.isEmpty()) return json("valid", "false");
            return json("valid", "true", "cents", String.valueOf(d.get().cents()), "counter", String.valueOf(d.get().counter()));
        });

        route(s, "/tap/in", "POST", "OPERATOR", (u, p) -> {
            String cardUid = desk(u).resolveCard(req(p, "uid"));
            if (suspended(store.ownerOf(cardUid))) throw new IllegalStateException("Card owner account is suspended");
            TapDesk.BoardedInfo info = desk(u).tapIn(cardUid, Integer.parseInt(req(p, "stop")));
            return json("onBoard", String.valueOf(desk(u).onBoard()), "uid", info.cardUid(), "name", info.passengerName(), "stop", info.stopName());
        });
        route(s, "/tap/out", "POST", "OPERATOR", (u, p) -> tapResult(desk(u).tapOut(req(p, "uid"), Integer.parseInt(req(p, "stop"))), desk(u)));
        route(s, "/tap/missed", "POST", "OPERATOR", (u, p) -> tapResult(desk(u).missedTapOut(req(p, "uid")), desk(u)));
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
        route(s, "/sync/txns", "POST", "OPERATOR", (u, p) -> {
            String batch = p.getOrDefault("batch", "");
            int accepted = 0;
            int skipped = 0;
            for (String line : batch.split("\n")) {
                if (line.isBlank()) continue;
                String[] row = line.split(",", -1);
                if (row.length >= 8) {
                    if (store.hasTxn(row[0])) {
                        skipped++;
                    } else {
                        store.appendTxnRow(row);
                        accepted++;
                        if ("FARE_PAYMENT".equals(row[1]) && row.length > 9) {
                            try {
                                rewards.earn(row[2], row[0], Double.parseDouble(row[9]), Instant.now());
                            } catch (Exception ignored) {}
                        }
                    }
                }
            }
            return json("accepted", String.valueOf(accepted), "skipped", String.valueOf(skipped));
        });

        route(s, "/admin", "GET", null, (u, p) -> AdminPage.HTML);

        route(s, "/admin/api/users", "GET", "ADMIN", (u, p) -> {
            StringBuilder b = new StringBuilder();
            for (String[] r : Csv.read(dir.resolve("users.csv")))
                b.append(String.join("|", clean(r[0]), clean(r[1]), clean(r[2]), r[4], clean(r[5]), clean(r[6]), suspended(r[0]) ? "suspended" : "active")).append('\n');
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
            if (!act.equals("SUSPEND") && !act.equals("REINSTATE")) throw new IllegalArgumentException("Bad action");
            Csv.append(dir.resolve("suspended.csv"), SUSPENDED_H, t.getId(), act, Instant.now().toString());
            return json("ok", "1");
        });
        route(s, "/admin/api/routes", "GET", "ADMIN", (u, p) -> {
            StringBuilder b = new StringBuilder();
            book.routes().forEach((id, stops) -> b.append(clean(id)).append('|').append(String.join(" > ", stops.stream().map(x -> clean(x.name())).toList())).append('\n'));
            return b.toString();
        });
        route(s, "/admin/api/route/add", "POST", "ADMIN", (u, p) -> {
            List<String[]> stops = new ArrayList<>();
            for (String line : req(p, "stops").split(";")) stops.add(line.split(",", -1));
            RouteBook.addRoute(dir, req(p, "id"), stops);
            book = new RouteBook(dir);                       // planner sees the new route at once
            shapes = new RouteShapes(dir);                   // a new route has no road path until FetchRouteShapes runs
            Fares.setRoadKm(RoadDistances.load(dir, book.routes()));
            planner = new TripPlanner(book);
            return json("ok", "1");
        });
        route(s, "/admin/api/fares", "GET", "ADMIN", (u, p) -> {
            StringBuilder b = new StringBuilder("Current|" + Fares.BASE + "|" + Fares.RATE_PER_KM + "|-\n");
            for (String[] r : Csv.read(dir.resolve("fares.csv"))) b.append(String.join("|", r)).append('\n');
            return b.toString();
        });
        route(s, "/admin/api/fares/publish", "POST", "ADMIN", (u, p) -> {
            LocalDate eff;
            try { eff = LocalDate.parse(req(p, "effective")); } catch (java.time.format.DateTimeParseException e) { throw new IllegalArgumentException("Bad date"); }
            Fares.publish(dir, eff, Long.parseLong(req(p, "base")), Long.parseLong(req(p, "rate")));
            return json("ok", "1");
        });
        route(s, "/admin/api/promos", "GET", "ADMIN", (u, p) -> {
            StringBuilder b = new StringBuilder();
            for (Rewards.Promo x : rewards.promotions()) b.append(clean(x.name())).append('|').append(x.multiplier()).append('|').append(x.start()).append('|').append(x.end()).append('\n');
            return b.toString();
        });
        route(s, "/admin/api/promo", "POST", "ADMIN", (u, p) -> {
            LocalDate from, to;
            try { from = LocalDate.parse(req(p, "start")); to = LocalDate.parse(req(p, "end")); } catch (java.time.format.DateTimeParseException e) { throw new IllegalArgumentException("Bad date"); }
            rewards.addPromotion(req(p, "name"), Double.parseDouble(req(p, "multiplier")), from.atStartOfDay().toInstant(ZoneOffset.UTC), to.plusDays(1).atStartOfDay().minusSeconds(1).toInstant(ZoneOffset.UTC));
            return json("ok", "1");
        });
        route(s, "/admin/summary", "GET", "ADMIN", (u, p) -> json("summary", store.summary()));

        s.start();
        System.out.println("FareX server on port " + Env.get("PORT", "8080") + (Pay.FAKE ? " (FAKE_PAY on)" : ""));
    }
}