package farex.server;

import farex.shared.Model.*;
import java.util.List;

/**
 * cards.csv, loads.csv, credits.csv.
 * Flow: startLoad -> user pays QR -> webhook marks PAID and creates a Credit -> phone taps card -> claim() signs new balance.
 * Same claim() path pays out redeemed rewards and operator transfers.
 */
public class CardService {
    public Load startLoad(String accountId, String cardUid, long amount, String method) {
        // TODO: check amount limits (min 100, max 500_000_000 centavos), check card is ACTIVE and owned,
        //       PayMongo.createQrPh, append loads.csv with AWAITING_PAYMENT
        throw new UnsupportedOperationException("TODO");
    }

    public void onWebhook(String payload, String signatureHeader) {
        // TODO: verifyWebhook, find Load by intentId, ignore if already PAID (webhooks repeat),
        //       on paid: Load -> PAID and create Credit(CARD_LOAD). On failed: Load -> FAILED
    }

    /** Expiry on read: AWAITING_PAYMENT and expiresAt < now means EXPIRED. */
    public Load loadStatus(String loadId) { throw new UnsupportedOperationException("TODO"); }

    public List<Credit> pendingCredits(String accountId) { throw new UnsupportedOperationException("TODO"); }

    /**
     * Phone sends what the chip holds now. Server verifies that old signature first (reject if tampered),
     * then new balance = chip balance + credit, counter + 1, sign, mark Credit claimed, append ledger row.
     */
    public SignedBalance claim(String accountId, String creditId, String cardUid,
                               long chipBalance, long chipCounter, String chipSignature) {
        throw new UnsupportedOperationException("TODO");
    }

    public Credit createCredit(String accountId, String cardUid, long amount, farex.shared.Enums.TxnType source, int validDays) {
        throw new UnsupportedOperationException("TODO");
    }

    public void link(String accountId, String cardUid) { throw new UnsupportedOperationException("TODO"); }

    /** Freeze old uid, bind new uid, move last synced balance to new card as a Credit. */
    public void reportLost(String accountId, String oldUid, String newUid) { throw new UnsupportedOperationException("TODO"); }

    public long syncedBalance(String cardUid) { throw new UnsupportedOperationException("TODO"); }
}
