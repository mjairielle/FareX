package farex.server;

import farex.shared.Model.*;
import java.util.List;
import java.util.Set;

/** ledger.csv. Operator uploads land here. Safe to retry: duplicate uuid rows are skipped. */
public class SyncService {
    /** uuids already in ledger, loaded once at startup. */
    private Set<String> seen;

    /** Returns how many new rows were saved. */
    public int receiveTxns(String operatorId, String csvBody) {
        // TODO: parse rows, skip uuid in seen, check amount is not negative,
        //       Csv.append ledger, RewardsService.earn for FARE_PAYMENT, update cards.csv synced balance
        throw new UnsupportedOperationException("TODO");
    }

    /** GPS stop arrivals from operator phone go to stop_candidates.csv for admin review. */
    public int receiveStopCandidates(String operatorId, String csvBody) { throw new UnsupportedOperationException("TODO"); }

    /** All rows for an account, or one card only. Powers both Transactions views. */
    public List<Txn> history(String accountId, String cardUidOrNull) { throw new UnsupportedOperationException("TODO"); }
}
