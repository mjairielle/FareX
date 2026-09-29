package FareX;
public class XPressCard {
    private String cardUID;
    private double balance;
    private String signature;
    private CardStatus status;
    private String ownerAccountID;
    private LocalDateTime lastSyncedAt;

    public boolean verifySignature() {}
    public void writeBalance(double newBalance, String signature) {}
    public void freeze() {}
}

public class PendingCredit {
    private double amount;
    private TransactionType source;
    private LocalDateTime expiresAt;

    public boolean isExpired() {}
}

public class LoadRequest {
    private String cardUID, paymentAccountNo;
    private PaymentMethod method;
    private double amount, fee;
    private String status;
}