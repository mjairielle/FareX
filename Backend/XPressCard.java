package FareX;

import java.time.LocalDateTime;

public class XPressCard {
    private String cardUID;
    private double balance;
    private String signature;
    private CardStatus status;
    private String ownerAccountID;
    private LocalDateTime lastSyncedAt;

    public boolean verifySignature() { return false; }
    public void writeBalance(double newBalance, String signature) { }
    public void freeze() { }
}
