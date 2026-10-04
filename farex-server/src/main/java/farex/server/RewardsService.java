package farex.server;

import farex.shared.Model.*;

/** rewards.csv. Balance is the sum of PointsRow deltas, nothing stored twice. */
public class RewardsService {
    static final double POINTS_PER_KM = 1.0;   // TODO: team to confirm the rate

    /** Called from SyncService for each FARE_PAYMENT row. points = km x rate x active promo multiplier. */
    public void earn(Txn fare) { throw new UnsupportedOperationException("TODO"); }

    public double total(String accountId) { throw new UnsupportedOperationException("TODO"); }

    /** Deduct points, create a Credit via CardService (valid 30 days), append POINTS_REDEEMED ledger row. */
    public Credit redeem(String accountId, double points) { throw new UnsupportedOperationException("TODO"); }

    /** Best multiplier among promotions active today for this route, else 1.0. */
    Promotion activePromo(String routeId) { throw new UnsupportedOperationException("TODO"); }
}
