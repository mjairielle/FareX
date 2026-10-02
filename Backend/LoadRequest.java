package Backend;

import java.time.Instant;
import java.util.Objects;

public class LoadRequest {
    private final String loadID;
    private final String ownerAccountID;
    private final String cardUID;
    private final double amount;
    private final PaymentMethod method;
    private String paymentIntentID;
    private LoadStatus status;
    private final Instant createdAt;
    private Instant expiresAt;

    public LoadRequest(String loadID, String ownerAccountID, String cardUID, double amount, PaymentMethod method) {
        if (Double.isNaN(amount) || amount <= 0) {
        throw new IllegalArgumentException("amount must be positive, got " + amount);
        }

        if (Objects.isNull(loadID) || loadID.isBlank()) {
            throw new IllegalArgumentException("loadID must be non-null and non-blank");
        }

        if (Objects.isNull(ownerAccountID) || ownerAccountID.isBlank()) {
            throw new IllegalArgumentException("ownerAccountID must be non-null and non-blank");
        }

        if (Objects.isNull(cardUID) || cardUID.isBlank()) {
            throw new IllegalArgumentException("cardUID must be non-null and non-blank");
        }

        if (Objects.isNull(method)) {
            throw new IllegalArgumentException("method must be non-null");
        }
        
        if (amount == Double.POSITIVE_INFINITY) {
            throw new IllegalArgumentException("amount must be finite, got " + amount);
        }

        this.loadID = loadID;
        this.ownerAccountID = ownerAccountID;
        this.cardUID = cardUID;
        this.amount = amount;
        this.method = method;
        this.status = LoadStatus.CREATED;
        this.createdAt = Instant.now();
    }

    private boolean isOneOf(LoadStatus... statuses) {
        
        for (LoadStatus s : statuses) {
            if (this.status == s) {
                return true;
            }
        }
        return false;
    }

    public boolean attachIntent(String paymentIntentID, Instant expiresAt) { 
        if(paymentIntentID == null || paymentIntentID.isBlank()) {
            throw new IllegalArgumentException("paymentIntentID and expiresAt must be non-null and non-blank");
        }
        if (!isOneOf(LoadStatus.CREATED)) {
            return false;
        }
        this.paymentIntentID = paymentIntentID;
        this.expiresAt = expiresAt;
        this.status = LoadStatus.AWAITING_PAYMENT;
        
        
        return true; 
    }
    public boolean markPaid() { return false; }
    public boolean markExpired() { return false; }
    public boolean cancel() { return false; }
    public boolean markFailed() { return false; }

    public boolean isTerminal() { return false; }
    public long secondsLeft(Instant now) { return 0; }
}