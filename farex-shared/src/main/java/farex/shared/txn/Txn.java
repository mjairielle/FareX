package farex.shared.txn;

import java.time.Instant;

public abstract class Txn {
    protected final String uuid;
    protected final String accountId;
    protected final String cardUid;
    protected final long amount;
    protected final long balanceBefore;
    protected final long balanceAfter;
    protected final Instant time;

    protected Txn(String uuid, String accountId, String cardUid, long amount,
                  long balanceBefore, long balanceAfter, Instant time) {
        this.uuid = uuid;
        this.accountId = accountId;
        this.cardUid = cardUid;
        this.amount = amount;
        this.balanceBefore = balanceBefore;
        this.balanceAfter = balanceAfter;
        this.time = time;
    }

    public abstract String describe();
}