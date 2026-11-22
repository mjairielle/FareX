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

    /** Returns {sessionId, checkoutUrl}. */
    static String[] start(double pesos) throws Exception {
        if (FAKE) return new String[]{"fake-" + UUID.randomUUID(), "fake"};
        String back = Env.get("PUBLIC_URL", "http://localhost:8080");
        String body = "{\"data\":{\"attributes\":{\"line_items\":[{\"amount\":" + Math.round(pesos * 100)
                + ",\"currency\":\"PHP\",\"name\":\"FareX card load\",\"quantity\":1}],"
                + "\"payment_method_types\":[\"gcash\",\"paymaya\",\"card\"],"
                + "\"success_url\":\"" + back + "/paid\",\"cancel_url\":\"" + back + "/cancelled\"}}}";
        HttpResponse<String> r = HTTP.send(HttpRequest.newBuilder(URI.create("https://api.paymongo.com/v1/checkout_sessions")).timeout(WAIT)
                .header("Authorization", auth()).header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(body)).build(), HttpResponse.BodyHandlers.ofString());
        String id = find(r.body(), "\"id\"\\s*:\\s*\"(cs_[^\"]+)\"");
        String url = find(r.body(), "\"checkout_url\"\\s*:\\s*\"([^\"]+)\"");
        if (id == null || url == null) throw new IllegalStateException("PayMongo refused: HTTP " + r.statusCode());
        return new String[]{id, url};
    }

    static boolean paid(String sessionId) throws Exception {
        if (FAKE) return sessionId.startsWith("fake-");
        HttpResponse<String> r = HTTP.send(HttpRequest.newBuilder(URI.create("https://api.paymongo.com/v1/checkout_sessions/" + sessionId)).timeout(WAIT)
                .header("Authorization", auth()).GET().build(), HttpResponse.BodyHandlers.ofString());
        return r.body().matches("(?s).*\"status\"\\s*:\\s*\"(succeeded|paid)\".*");
    }
}
