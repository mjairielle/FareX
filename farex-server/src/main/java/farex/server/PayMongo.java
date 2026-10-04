package farex.server;

import java.net.http.HttpClient;
import java.time.Instant;

/**
 * PayMongo client. JDK HttpClient, no SDK. Use test keys while building.
 * One class replaces PayMongoGateway, PaymentIntent, IntentStatus, WebHookEvent, WebhookEventType, GatewayException.
 */
public class PayMongo {
    public record Qr(String intentId, String qrImage, Instant expiresAt) {}
    public record Event(String type, String intentId) {}   // type: "paid", "failed", "expired"

    private final HttpClient http = HttpClient.newHttpClient();
    private final String secretKey = System.getenv("PAYMONGO_SECRET");
    private final String webhookSecret = System.getenv("PAYMONGO_WEBHOOK_SECRET");

    /** Create payment intent and attach QR Ph in one call. Throws RuntimeException on gateway error. */
    public Qr createQrPh(long amountCentavos, String description) { throw new UnsupportedOperationException("TODO"); }

    public boolean verifyWebhook(String payload, String signatureHeader) { throw new UnsupportedOperationException("TODO"); }

    public Event parseWebhook(String payload) { throw new UnsupportedOperationException("TODO"); }
}
