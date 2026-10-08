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

    @Override public String type() { return "FARE_PAYMENT"; }
    @Override protected String extra1() { return operatorId; }
    @Override protected String extra2() { return String.valueOf(distanceKm); }

    @Override
    public String describe() {
        return String.format("Fare %s, %.1f km", peso(amount), distanceKm);
    }
}
