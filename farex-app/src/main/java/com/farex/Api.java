package com.farex;

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
    private static final HttpClient HTTP = HttpClient.newHttpClient();
    private static String token;

    /** Unchecked so screens show the message in the red label with no extra code. */
    public static final class ApiException extends RuntimeException {
        public ApiException(String m) { super(m); }
    }

    private static String call(String method, String path, String body) {
        try {
            HttpRequest.Builder b = HttpRequest.newBuilder(URI.create(base + path))
                    .method(method, body == null ? HttpRequest.BodyPublishers.noBody() : HttpRequest.BodyPublishers.ofString(body));
            if (token != null) b.header("Authorization", "Bearer " + token);
            HttpResponse<String> r = HTTP.send(b.build(), HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
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

    public static void register(String name, String email, String pw, String role, String type, String plate) {
        call("POST", "/register", form("name", name, "email", email, "password", pw, "role", role, "passengerType", type, "plate", plate));
    }

    /** Child class chosen by role, same as the local Store did. */
    public static User login(String email, String pw) {
        String r = call("POST", "/login", form("email", email, "password", pw));
        token = field(r, "token", null);
        String id = field(r, "id", ""), name = field(r, "name", ""), mail = field(r, "email", "");
        return switch (field(r, "role", "")) {
            case "PASSENGER" -> new Passenger(id, name, mail, field(r, "passengerType", "REGULAR"));
            case "OPERATOR" -> new Operator(id, name, mail, field(r, "plate", ""));
            default -> new Admin(id, name, mail);
        };
    }

    public static void logout() { token = null; }

    // ---------- cards and money ----------

    public static List<String> cardsOf() { return lines(call("GET", "/cards", null)); }

    public static void addCard(String last8) { call("POST", "/cards/add", form("last8", last8)); }

    /** Operator only. Each item: {uid, ownerName}. */
    public static List<String[]> allCards() {
        List<String[]> out = new ArrayList<>();
        for (String l : lines(call("GET", "/cards/all", null))) out.add(l.split(",", 2));
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

    /** One suggested trip. legs look like "R1: Philcoa > Tandang Sora (1.9 km)". */
    public record PlanCard(String label, String km, int transfers, long fareCents, int minutes, List<String> legs) {}

    public static List<String> places() { return lines(call("GET", "/places", null)); }

    public static List<PlanCard> plan(String from, String to) {
        List<PlanCard> out = new ArrayList<>();
        for (String l : lines(call("GET", "/plan?" + form("from", from, "to", to), null))) {
            String[] f = l.split("\\|", -1);
            List<String> legs = new ArrayList<>();
            for (String leg : f[5].split(";")) {
                String[] x = leg.split("~");
                legs.add(x[0] + ": " + x[1] + " > " + x[2] + " (" + x[3] + " km)");
            }
            out.add(new PlanCard(f[0], f[1], Integer.parseInt(f[2]), Long.parseLong(f[3]), Integer.parseInt(f[4]), legs));
        }
        return out;
    }

    // ---------- transport ----------

    public static List<String> stops() {
        List<String> out = new ArrayList<>();
        for (String l : lines(call("GET", "/stops", null))) out.add(l.split(",")[0]);
        return out;
    }

    public static int tapIn(String uid, int stop) {
        return Integer.parseInt(field(call("POST", "/tap/in", form("uid", uid, "stop", String.valueOf(stop))), "onBoard", "0"));
    }

    /** Returns {description, newBalanceCents, onBoard, pointsEarned}. */
    public static String[] tapOut(String uid, int stop) { return tapResult(call("POST", "/tap/out", form("uid", uid, "stop", String.valueOf(stop)))); }

    public static String[] tapMissed(String uid) { return tapResult(call("POST", "/tap/missed", form("uid", uid))); }

    private static String[] tapResult(String r) { return new String[]{field(r, "describe", ""), field(r, "balance", "0"), field(r, "onBoard", "0"), field(r, "points", "0")}; }

    public static String summary() { return field(call("GET", "/admin/summary", null), "summary", "").replace("\\n", "\n"); }
}
