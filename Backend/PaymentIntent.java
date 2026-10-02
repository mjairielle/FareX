package Backend;

import java.time.LocalDateTime;

public class PaymentIntent {
    private String paymentIntentID, clientKey;
    private double amount;
    private IntentStatus status;
    private String qrImage;
    private LocalDateTime expiresAt;
}
