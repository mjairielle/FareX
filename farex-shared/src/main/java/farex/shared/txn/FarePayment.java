package farex.shared.txn;

import java.time.Instant;

public class FarePayment extends Txn {
    private final String operatorId;
    private final double distanceKm;

    public FarePayment(String uuid, String accountId, String cardUid, long amount,
                       long balanceBefore, long balanceAfter, Instant time,
                       String operatorId, double distanceKm) {
        super(uuid, accountId, cardUid, amount, balanceBefore, balanceAfter, time);
        this.operatorId = operatorId;
        this.distanceKm = distanceKm;
    }

    @Override
    public String describe() {
        return String.format("Fare P%.2f, %.1f km", amount / 100.0, distanceKm);
    }
}