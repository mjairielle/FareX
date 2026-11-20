package com.farex;

import farex.shared.data.RouteShapes;
import farex.shared.data.RouteStop;
import farex.shared.fare.Fares;
import farex.shared.reward.Rewards;
import farex.shared.txn.Txn;
import farex.shared.user.*;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.regex.*;

/** The only class that talks to the server. Screens call this, never HTTP. Money is long centavos. */
public final class Api {
    /** Phone must use the laptop IP here, e.g. http://192.168.1.5:8080 . Login screen can change it. */
    public static String base = System.getProperty("farex.server", "http://localhost:8080");
    private static final HttpClient HTTP = HttpClient.newBuilder().connectTimeout(java.time.Duration.ofSeconds(10)).build();
    private static String token;

    /** Unchecked so screens show the message in the red label with no extra code. */
    public static final class ApiException extends RuntimeException {
        public ApiException(String m) { super(m); }
    }

    private static String call(String method, String path, String body) {
        try {
            HttpRequest.Builder b = HttpRequest.newBuilder(URI.create(base + path)).timeout(java.time.Duration.ofSeconds(20))
                    .method(method, body == null ? HttpRequest.BodyPublishers.noBody() : HttpRequest.BodyPublishers.ofString(body));
            if (token != null) b.header("Authorization", "Bearer " + token);
            HttpResponse<String> r = HTTP.send(b.build(), HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            // 404 = this server has no such route: it is an older build than the app
            if (r.statusCode() == 404) throw new ApiException("The server is out of date. Close it and start run-server.bat again.");
            if (r.statusCode() >= 400) throw new ApiException(field(r.body(), "error", "Server error " + r.statusCode()));
            return r.body();
        } catch (ApiException e) { throw e;
        } catch (Exception e) { throw new ApiException("Cannot reach server at " + base); }
    }

    static String field(String json, String k, String dflt) {
        Matcher m = Pattern.compile("\"" + k + "\":\"((?:[^\"\\\\]|\\\\.)*)\"").matcher(json);
        return m.find() ? m.group(1).replace("\\\"", "\"").replace("\\\\", "\\") : dflt;
    }

    private static String form(String... kv) {
        StringBuilder b = new StringBuilder();
        for (int i = 0; i < kv.length; i += 2) {
            if (i > 0) b.append('&');
            b.append(kv[i]).append('=').append(URLEncoder.encode(kv[i + 1], StandardCharsets.UTF_8));
        }
        return b.toString();
    }

    private static List<String> lines(String body) {
        List<String> out = new ArrayList<>();
        for (String l : body.split("\n")) if (!l.isBlank()) out.add(l);
        return out;
    }

    // ---------- account ----------

    /** otp = the pass from verifyOtp for this email (purpose SIGNUP). */
    public static void register(String name, String email, String pw, String role, String type, String plate, String phone, String otp) {
        call("POST", "/register", form("name", name, "email", email, "password", pw, "role", role, "passengerType", type,
                "plate", plate, "phone", phone, "otp", otp));
    }

    /** Emails a 6-digit code. purpose SIGNUP (the given email) or LOST (the logged-in account's email). */
    public static void sendOtp(String purpose, String email) {
        call("POST", "/otp/send", form("purpose", purpose, "email", email));
    }

    /** Checks the code; returns the one-time pass that register / freezeCard need. */
    public static String verifyOtp(String purpose, String email, String code) {
        return field(call("POST", "/otp/verify", form("purpose", purpose, "email", email, "code", code)), "otp", "");
    }

    /** user = the role the token acts as now; roles = every role of the account; pending = roles waiting for admin. */
    public record Login(User user, List<String> roles, List<String> pending, String phone) {}

    public static Login login(String email, String pw) {
        String r = call("POST", "/login", form("email", email, "password", pw));
        token = field(r, "token", null);
        loadRoadKm();
        return new Login(user(r), csv(field(r, "roles", "")), csv(field(r, "pending", "")), field(r, "phone", ""));
    }

    /** Same login, other role of the account ("PASSENGER" or "OPERATOR"). */
    public static User switchRole(String role) { return user(call("POST", "/role/switch", form("role", role))); }

    /** Adds a role to the logged-in account. Returns "pending" (operator, admin approves) or "active". */
    public static String addRole(String role, String plate) {
        return field(call("POST", "/role/add", form("role", role, "plate", plate)), "status", "");
    }

    /** Child class chosen by role, same as the local Store did. */
    private static User user(String r) {
        String id = field(r, "id", ""), name = field(r, "name", ""), mail = field(r, "email", "");
        return switch (field(r, "role", "")) {
            case "PASSENGER" -> new Passenger(id, name, mail, field(r, "passengerType", "REGULAR"));
            case "OPERATOR" -> new Operator(id, name, mail, field(r, "plate", ""));
            default -> new Admin(id, name, mail);
        };
    }

    private static List<String> csv(String s) { return s.isEmpty() ? List.of() : List.of(s.split(",")); }

    public static void updateProfile(String name, String phone) {
        call("POST", "/profile/update", form("name", name, "phone", phone));
    }

    /** Deletes the whole account (both roles); the card balance is written off. */
    public static void deleteAccount(String password) {
        call("POST", "/profile/delete", form("password", password));
        token = null;
    }

    public static void changePassword(String current, String password) {
        call("POST", "/profile/password", form("current", current, "password", password));
    }

    public static void logout() { token = null; }

    // ---------- cards and money ----------

    public static List<String> cardsOf() { return lines(call("GET", "/cards", null)); }

    public static void addCard(String tagUid) { call("POST", "/cards/add", form("uid", tagUid)); }

    public static void freezeCard(String uid, String otp) { call("POST", "/cards/freeze", form("uid", uid, "otp", otp)); }

    /** Returns {newUid, balanceCents}. */
    public static String[] reportLostCard(String oldUid, String newTagUid) {
        String r = call("POST", "/cards/lost", form("oldUid", oldUid, "newUid", newTagUid));
        return new String[]{field(r, "newUid", ""), field(r, "cents", "0")};
    }

    /** Operator only. Each item: {uid, passengerType, ownerName}. */
    public static List<String[]> allCards() {
        List<String[]> out = new ArrayList<>();
        for (String l : lines(call("GET", "/cards/all", null))) out.add(l.split(",", 3));
        return out;
    }

    public static long balance(String uid) { return Long.parseLong(field(call("GET", "/balance?uid=" + uid, null), "cents", "0")); }

    /** Newest first. Rebuilt as CardLoad and FarePayment objects by the shared Txn. */
    public static List<Txn> history() {
        List<Txn> out = new ArrayList<>();
        for (String l : lines(call("GET", "/history", null))) out.add(Txn.fromRow(l.split(",", -1)));
        return out;
    }

    /** Returns {sessionId, checkoutUrl}. checkoutUrl "fake" means the server runs without PayMongo. */
    public static String[] startLoad(String uid, long cents) {
        String r = call("POST", "/load/start", form("uid", uid, "cents", String.valueOf(cents)));
        return new String[]{field(r, "sessionId", ""), field(r, "checkoutUrl", "")};
    }

    /** True when paid and credited. False means not paid yet. */
    public static boolean confirmLoad(String sessionId) {
        return "paid".equals(field(call("POST", "/load/confirm", form("sessionId", sessionId)), "status", ""));
    }

    // ---------- card bytes ----------

    /** Hex of the signed 16 bytes to write on the card. */
    public static String cardPayload(String uid) { return field(call("GET", "/card/payload?uid=" + uid, null), "payload", ""); }

    /** Operator check of bytes read off a card. Returns the balance on the card, or -1 when tampered. */
    public static long verifyCard(String uid, String payloadHex) {
        String r = call("POST", "/card/verify", form("uid", uid, "payload", payloadHex));
        return "true".equals(field(r, "valid", "false")) ? Long.parseLong(field(r, "cents", "0")) : -1;
    }

    // ---------- rewards ----------

    public record Points(long points, double multiplier) {}

    public static Points rewards() {
        String r = call("GET", "/rewards", null);
        return new Points(Long.parseLong(field(r, "points", "0")), Double.parseDouble(field(r, "multiplier", "1")));
    }

    public static List<Rewards.Perk> perks() {
        List<Rewards.Perk> out = new ArrayList<>();
        for (String l : lines(call("GET", "/rewards/perks", null))) {
            String[] f = l.split("\\|");
            out.add(new Rewards.Perk(f[0], f[1], Long.parseLong(f[2]), Long.parseLong(f[3])));
        }
        return out;
    }

    /** Returns the new card balance in centavos. */
    public static long redeem(String uid, String perkId) {
        return Long.parseLong(field(call("POST", "/rewards/redeem", form("uid", uid, "perk", perkId)), "cents", "0"));
    }

    // ---------- trip planner ----------

    /** The planner's route id for walking (to the first jeep, between jeeps, from the last one). */
    public static final String WALK = "WALK";

    /**
     * One part of a trip: board `from`, ride `route`, get off at `to` (or walk, when route is WALK).
     * fromAt / toAt = where those are; a ride can start or end "at the roadside near X" (a jeep
     * flagged down on its road), which is no stop. Null from an older server.
     */
    public record PlanRide(String route, String from, String to, double km, RouteStop fromAt, RouteStop toAt) {
        public PlanRide(String route, String from, String to, double km) { this(route, from, to, km, null, null); }
        public boolean walk() { return WALK.equals(route); }
    }

    /** A jeepney route whose road passes near a point: how far, where, and its two ends. */
    public record Passing(String routeId, String name, int meters, RouteStop at, String firstStop, String lastStop,
                          boolean oneway) {}

    /** Jeeps passing a point (the Routes map, tapped): routes whose road is within ~150 m, nearest first. */
    public static List<Passing> passing(double lat, double lng) {
        List<Passing> out = new ArrayList<>();
        for (String l : lines(call("GET", "/passing?" + form("lat", String.valueOf(lat), "lng", String.valueOf(lng)), null))) {
            String[] f = l.split("\\|", -1);
            String[] ll = f[3].split("~");
            out.add(new Passing(f[0], f[1], Integer.parseInt(f[2]),
                    new RouteStop(f[1], Double.parseDouble(ll[0]), Double.parseDouble(ll[1])), f[4], f[5], Boolean.parseBoolean(f[6])));
        }
        return out;
    }

    /** One suggested trip. legs look like "Ride R1: Philcoa > Tandang Sora (1.90 km)" / "Walk: A > B (0.30 km)"; rides are the same, for the map. */
    public record PlanCard(String label, String km, int transfers, long fareCents, int minutes, List<String> legs,
                           List<PlanRide> rides) {}

    public static List<String> places() { return lines(call("GET", "/places", null)); }

    /** Between two stops. */
    public static List<PlanCard> plan(String from, String to) {
        return parsePlans(call("GET", "/plan?" + form("from", from, "to", to), null));
    }

    /** Between any two points, e.g. a pin on the map; the trip starts and ends with a walk to/from a stop. */
    public static List<PlanCard> plan(double fromLat, double fromLng, String fromLabel,
                                      double toLat, double toLng, String toLabel) {
        return parsePlans(call("GET", "/plan?" + form(
                "fromLat", String.valueOf(fromLat), "fromLng", String.valueOf(fromLng), "fromLabel", fromLabel,
                "toLat", String.valueOf(toLat), "toLng", String.valueOf(toLng), "toLabel", toLabel), null));
    }

    // "14.65,121.04" -> a named point
    private static RouteStop point(String name, String latLng) {
        String[] ll = latLng.split(",");
        return new RouteStop(name, Double.parseDouble(ll[0]), Double.parseDouble(ll[1]));
    }

    private static List<PlanCard> parsePlans(String body) {
        List<PlanCard> out = new ArrayList<>();
        for (String l : lines(body)) {
            String[] f = l.split("\\|", -1);
            List<String> legs = new ArrayList<>();
            List<PlanRide> rides = new ArrayList<>();
            for (String leg : f[5].split(";")) {
                String[] x = leg.split("~");
                PlanRide r = x.length >= 6
                        ? new PlanRide(x[0], x[1], x[2], Double.parseDouble(x[3]), point(x[1], x[4]), point(x[2], x[5]))
                        : new PlanRide(x[0], x[1], x[2], Double.parseDouble(x[3]));
                legs.add((r.walk() ? "Walk: " : "Ride " + x[0] + ": ") + x[1] + " > " + x[2] + " (" + x[3] + " km)");
                rides.add(r);
            }
            out.add(new PlanCard(f[0], f[1], Integer.parseInt(f[2]), Long.parseLong(f[3]), Integer.parseInt(f[4]), legs, rides));
        }
        return out;
    }

    /** Every route's stops by route id, for the map. */
    public static Map<String, List<RouteStop>> routeStops() {
        Map<String, List<RouteStop>> out = new LinkedHashMap<>();
        for (RouteDetail r : routes()) out.put(r.id(), r.stops());
        return out;
    }

    /**
     * Gives Fares the server's road distances, so fares worked out in the app (route fare table,
     * tap terminal) match what the server charges. Older server or no file: straight lines stay.
     */
    public static void loadRoadKm() {
        Map<String, Double> km = new HashMap<>();
        try {
            for (String l : lines(call("GET", "/road-km", null))) {
                String[] f = l.split("\\|", -1);
                if (f.length == 2) km.put(f[0], Double.parseDouble(f[1]));
            }
        } catch (ApiException | NumberFormatException e) {
            return;
        }
        Fares.setRoadKm(km);
    }

    /** Road path per route and segment (stop i -> i+1), for the map. Empty if the server has none or is older. */
    public static Map<String, Map<Integer, List<RouteShapes.Point>>> routeShapes() {
        Map<String, Map<Integer, List<RouteShapes.Point>>> out = new LinkedHashMap<>();
        List<String> rows;
        try {
            rows = lines(call("GET", "/route-shapes", null));
        } catch (ApiException e) {
            return out;                                     // straight lines between stops instead
        }
        for (String l : rows) {
            String[] f = l.split("\\|", -1);
            if (f.length < 3) continue;
            List<RouteShapes.Point> pts = new ArrayList<>();
            for (String pt : f[2].split(";")) {
                String[] xy = pt.split("~");
                if (xy.length == 2) pts.add(new RouteShapes.Point(Double.parseDouble(xy[0]), Double.parseDouble(xy[1])));
            }
            out.computeIfAbsent(f[0], k -> new TreeMap<>()).put(Integer.parseInt(f[1]), pts);
        }
        return out;
    }

    // ---------- transport & operator ----------

    public record StopCoord(int index, String name, double lat, double lng) {}
    public record BoardedPassenger(String uid, String name, int stopIdx, String stopName, String time) {}
    public record TapInResult(int onBoard, String uid, String name, String stop) {}

    public static List<String> stops() {
        List<String> out = new ArrayList<>();
        for (String l : lines(call("GET", "/stops", null))) out.add(l.split(",")[0]);
        return out;
    }

    public static List<StopCoord> stopsWithCoords() {
        List<StopCoord> out = new ArrayList<>();
        List<String> raw = lines(call("GET", "/stops", null));
        for (int i = 0; i < raw.size(); i++) {
            String[] p = raw.get(i).split(",");
            out.add(new StopCoord(i, p[0], Double.parseDouble(p[1]), Double.parseDouble(p[2])));
        }
        return out;
    }

    public static int tapIn(String uid, int stop) {
        return Integer.parseInt(field(call("POST", "/tap/in", form("uid", uid, "stop", String.valueOf(stop))), "onBoard", "0"));
    }

    public static TapInResult tapInDetailed(String uid, int stop) {
        String r = call("POST", "/tap/in", form("uid", uid, "stop", String.valueOf(stop)));
        return new TapInResult(Integer.parseInt(field(r, "onBoard", "0")), field(r, "uid", uid), field(r, "name", ""), field(r, "stop", ""));
    }

    /** Returns {description, newBalanceCents, onBoard, pointsEarned}. */
    public static String[] tapOut(String uid, int stop) { return tapResult(call("POST", "/tap/out", form("uid", uid, "stop", String.valueOf(stop)))); }

    public static String[] tapMissed(String uid) { return tapResult(call("POST", "/tap/missed", form("uid", uid))); }

    private static String[] tapResult(String r) { return new String[]{field(r, "describe", ""), field(r, "balance", "0"), field(r, "onBoard", "0"), field(r, "points", "0"), field(r, "fare", "0")}; }

    public static List<BoardedPassenger> operatorBoarded() {
        List<BoardedPassenger> out = new ArrayList<>();
        for (String l : lines(call("GET", "/operator/boarded", null))) {
            String[] p = l.split("\\|", -1);
            if (p.length >= 5) {
                out.add(new BoardedPassenger(p[0], p[1], Integer.parseInt(p[2]), p[3], p[4]));
            }
        }
        return out;
    }

    public static List<Txn> operatorHistory() {
        List<Txn> out = new ArrayList<>();
        for (String l : lines(call("GET", "/operator/history", null))) {
            out.add(Txn.fromRow(l.split(",", -1)));
        }
        return out;
    }

    public static String[] syncBatch(String batch) {
        String r = call("POST", "/sync/txns", form("batch", batch));
        return new String[]{field(r, "accepted", "0"), field(r, "skipped", "0")};
    }

    /** name = "Commonwealth–SM Fairview via Regalado (to SM City Fairview)"; the id on older servers. */
    public record RouteDetail(String id, List<RouteStop> stops, String name) {}
    public record VehicleInfo(String plate, String driverName, String operatorId) {}
    /** base covers the first baseKm; rate applies per km after that. */
    public record FareRuleInfo(long base, long rate, double baseKm) {}

    public static List<RouteDetail> routes() {
        List<RouteDetail> out = new ArrayList<>();
        for (String l : lines(call("GET", "/routes", null))) {
            String[] p = l.split("\\|", -1);
            if (p.length >= 2) {
                List<RouteStop> stList = new ArrayList<>();
                for (String s : p[1].split(";")) {
                    if (s.isBlank()) continue;
                    String[] sp = s.split("~");
                    if (sp.length >= 3) {
                        stList.add(new RouteStop(sp[0], Double.parseDouble(sp[1]), Double.parseDouble(sp[2])));
                    }
                }
                out.add(new RouteDetail(p[0], stList, p.length >= 3 && !p[2].isBlank() ? p[2] : p[0]));
            }
        }
        return out;
    }

    public static List<VehicleInfo> vehicles() {
        List<VehicleInfo> out = new ArrayList<>();
        for (String l : lines(call("GET", "/vehicles", null))) {
            String[] p = l.split("\\|", -1);
            if (p.length >= 3) out.add(new VehicleInfo(p[0], p[1], p[2]));
        }
        return out;
    }

    public static FareRuleInfo currentFares() {
        String r = call("GET", "/fares/current", null);
        return new FareRuleInfo(Long.parseLong(field(r, "base", "1400")), Long.parseLong(field(r, "rate", "200")),
                Double.parseDouble(field(r, "baseKm", "4.0")));
    }

    // ---------- admin panel ----------

    public static List<String[]> adminUsers() {
        List<String[]> out = new ArrayList<>();
        for (String l : lines(call("GET", "/admin/api/users", null))) out.add(l.split("\\|", -1));
        return out;
    }

    public static void adminSetUserType(String id, String type) {
        call("POST", "/admin/api/user/type", form("id", id, "type", type));
    }

    public static void adminSetUserStatus(String id, String action) {
        call("POST", "/admin/api/user/status", form("id", id, "action", action));
    }

    public static List<String[]> adminRoutes() {
        List<String[]> out = new ArrayList<>();
        for (String l : lines(call("GET", "/admin/api/routes", null))) out.add(l.split("\\|", -1));
        return out;
    }

    public static void adminAddRoute(String id, String stops) {
        call("POST", "/admin/api/route/add", form("id", id, "stops", stops));
    }

    public static List<String[]> adminFares() {
        List<String[]> out = new ArrayList<>();
        for (String l : lines(call("GET", "/admin/api/fares", null))) out.add(l.split("\\|", -1));
        return out;
    }

    public static void adminPublishFare(String effective, long base, long rate) {
        call("POST", "/admin/api/fares/publish", form("effective", effective, "base", String.valueOf(base), "rate", String.valueOf(rate)));
    }

    public static List<String[]> adminPromos() {
        List<String[]> out = new ArrayList<>();
        for (String l : lines(call("GET", "/admin/api/promos", null))) out.add(l.split("\\|", -1));
        return out;
    }

    public static void adminAddPromo(String name, double multiplier, String start, String end) {
        call("POST", "/admin/api/promo", form("name", name, "multiplier", String.valueOf(multiplier), "start", start, "end", end));
    }

    public static String summary() { return field(call("GET", "/admin/summary", null), "summary", "").replace("\\n", "\n"); }
}