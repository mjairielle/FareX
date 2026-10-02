package Backend;

import java.net.http.HttpClient;

public class PayMongoGateway {
    private String secretKey;
    private String webhookSecret;
    private String baseUrl;
    private HttpClient httpClient;


    public PaymentIntent createPaymentIntent(double amount, String description) throws GatewayException { return null; }
    public PaymentIntent attachQrPh(PaymentIntent intent) throws GatewayException { return null; }
    public IntentStatus getIntentStatus(String paymentIntentID) throws GatewayException { return null; }
    public boolean verifyWebhookSignature(String payload, String signatureHeader) { return false; }
    public WebhookEvent parseWebhook(String payload) { return null; }
}
