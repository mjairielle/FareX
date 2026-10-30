package FareX;

import java.time.LocalDateTime;

public class XPressCard {
    private String cardUID;
    private double balance;
    private String signature;
    private CardStatus status;
    private String ownerAccountID;
    private LocalDateTime lastSyncedAt;

    public XPressCard() {
        this.status = CardStatus.ACTIVE;
    }

    public XPressCard(String cardUID, String ownerAccountID, double balance) {
        this.cardUID = cardUID;
        this.ownerAccountID = ownerAccountID;
        this.balance = balance;
        this.status = CardStatus.ACTIVE;
    }

    public String getCardUID()           { return cardUID; }
    public String getOwnerAccountID()    { return ownerAccountID; }
    public double getBalance()           { return balance; }
    public CardStatus getStatus()        { return status; }
    public LocalDateTime getLastSyncedAt() { return lastSyncedAt; }

    /** A frozen card (lost, reported) cannot pay for a trip. */
    public boolean isUsable() { return status == CardStatus.ACTIVE; }

    public boolean verifySignature() { return false; }
    public void writeBalance(double newBalance, String signature) { }
    public void freeze() { this.status = CardStatus.FROZEN; }
}
