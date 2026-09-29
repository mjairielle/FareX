package FareX;

import java.time.LocalDateTime;

public class PendingCredit {
    private double amount;
    private TransactionType source;
    private LocalDateTime expiresAt;

    public boolean isExpired() { return false; }
}
