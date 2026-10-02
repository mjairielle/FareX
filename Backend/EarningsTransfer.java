package Backend;

import java.time.LocalDateTime;

public class EarningsTransfer {
    private String uuid, operatorID, payoutCardUID;
    private double amount;
    private String status;
    private LocalDateTime requestedAt;

    public boolean isWithinAvailable(EarningsAccount acct) { return false; }
}
