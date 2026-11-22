package farex.server;

import java.net.URI;
import java.net.http.*;
import java.time.Duration;
import java.util.Base64;
import java.util.UUID;
import java.util.regex.*;

/** PayMongo checkout. FAKE_PAY=1 skips the network for local demos. */
final class Pay {
    private static final HttpClient HTTP = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();
    private static final Duration WAIT = Duration.ofSeconds(20);      // a hanging PayMongo must not hang the request forever
    static final boolean FAKE = "1".equals(Env.get("FAKE_PAY"));

    private static String auth() {
        String k = Env.get("PAYMONGO_SECRET");
        if (k == null || k.isBlank()) throw new IllegalStateException("PAYMONGO_SECRET missing");
        return "Basic " + Base64.getEncoder().encodeToString((k + ":").getBytes());
    }

    private static String find(String json, String regex) {
        Matcher m = Pattern.compile(regex).matcher(json);
        return m.find() ? m.group(1) : null;
    }

    // The payment methods this PayMongo account takes. Test keys take any; with live keys PayMongo refuses the whole
    // checkout if one listed here is not activated for the account, so PAYMONGO_METHODS lists exactly what it activated.
    static final java.util.List<String> METHODS = java.util.Arrays.stream(Env.get("PAYMONGO_METHODS", "gcash,paymaya,card").split(","))
            .map(String::trim).filter(m -> m.matches("[a-z_]+")).toList();

    // The PayMongo methods to offer for what the rider picked in the app: straight to GCash for "GCash" (and BPI online
    // banking for "BPI", if activated), every activated method for anything else
    static java.util.List<String> types(String appMethod) {
        String want = switch (appMethod) {
            case "GCash" -> "gcash";
            case "BPI" -> "dob";
            default -> "";
        };
        return METHODS.contains(want) ? java.util.List.of(want) : METHODS;
    }

    /** Returns {sessionId, checkoutUrl}. appMethod: what the rider picked in the app ("GCash", "BPI", "Others"...). */
    static String[] start(double pesos, String appMethod) throws Exception {
        if (FAKE) return new String[]{"fake-" + UUID.randomUUID(), "fake"};
        String back = Env.get("PUBLIC_URL", "http://localhost:8080");
        String body = "{\"data\":{\"attributes\":{\"line_items\":[{\"amount\":" + Math.round(pesos * 100)
                + ",\"currency\":\"PHP\",\"name\":\"FareX card load\",\"quantity\":1}],"
                + "\"payment_method_types\":[\"" + String.join("\",\"", types(appMethod)) + "\"],"
                + "\"success_url\":\"" + back + "/paid\",\"cancel_url\":\"" + back + "/cancelled\"}}}";
        HttpResponse<String> r = HTTP.send(HttpRequest.newBuilder(URI.create("https://api.paymongo.com/v1/checkout_sessions")).timeout(WAIT)
                .header("Authorization", auth()).header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(body)).build(), HttpResponse.BodyHandlers.ofString());
        String id = find(r.body(), "\"id\"\\s*:\\s*\"(cs_[^\"]+)\"");
        String url = find(r.body(), "\"checkout_url\"\\s*:\\s*\"([^\"]+)\"");
        if (id == null || url == null) {                 // e.g. a method in PAYMONGO_METHODS that is not activated: say which
            String why = find(r.body(), "\"detail\"\\s*:\\s*\"([^\"]+)\"");
            System.err.println("PayMongo refused a checkout: HTTP " + r.statusCode() + " " + r.body());
            throw new IllegalStateException("PayMongo refused: " + (why != null ? why : "HTTP " + r.statusCode()));
        }
        return new String[]{id, url};
    }

    static boolean paid(String sessionId) throws Exception {
        if (FAKE) return sessionId.startsWith("fake-");
        HttpResponse<String> r = HTTP.send(HttpRequest.newBuilder(URI.create("https://api.paymongo.com/v1/checkout_sessions/" + sessionId)).timeout(WAIT)
                .header("Authorization", auth()).GET().build(), HttpResponse.BodyHandlers.ofString());
        return r.body().matches("(?s).*\"status\"\\s*:\\s*\"(succeeded|paid)\".*");
    }
}
