package farex.server;

import farex.shared.Model.Credit;

/**
 * Operator earnings. No stored account, the ledger is the truth.
 * available = sum of FARE_PAYMENT rows for this operator minus sum of OPERATOR_TRANSFER rows.
 */
public class EarningsService {
    public long available(String operatorId) { throw new UnsupportedOperationException("TODO"); }

    /**
     * Check amount <= available, then CardService.createCredit onto the operator's payout card
     * and append an OPERATOR_TRANSFER ledger row. Operator then taps the payout card to claim.
     */
    public Credit requestTransfer(String operatorId, long amount) { throw new UnsupportedOperationException("TODO"); }
}
