package farex.shared.txn;

import java.time.Instant;

public class CardLoad extends Txn {
    private final String paymentMethod;

    public CardLoad(String uuid, String accountId, String cardUid, long amount,
                    long balanceBefore, long balanceAfter, Instant time, String paymentMethod) {
        super(uuid, accountId, cardUid, amount, balanceBefore, balanceAfter, time);
        this.paymentMethod = paymentMethod;
    }

    @Override
    public String describe() {
        return String.format("Loaded P%.2f via %s", amount / 100.0, paymentMethod);
    }
}