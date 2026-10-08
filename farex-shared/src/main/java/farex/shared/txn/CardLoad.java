package farex.shared.txn;

import java.time.Instant;

public class CardLoad extends Txn {
    private final String paymentMethod;

    public CardLoad(String uuid, String accountId, String cardUid, long amount,
                    long balanceBefore, long balanceAfter, Instant time, String paymentMethod) {
        super(uuid, accountId, cardUid, amount, balanceBefore, balanceAfter, time);
        this.paymentMethod = paymentMethod;
    }

    @Override public String type() { return "CARD_LOAD"; }
    @Override protected String extra1() { return paymentMethod; }
    @Override protected String extra2() { return ""; }

    @Override
    public String describe() {
        return "Loaded " + peso(amount) + " via " + paymentMethod;
    }
}
