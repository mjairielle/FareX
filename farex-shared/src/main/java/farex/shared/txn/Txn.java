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

    public String getAccountId() { return accountId; }
    public String getCardUid() { return cardUid; }
    public long getBalanceAfter() { return balanceAfter; }
    public Instant getTime() { return time; }

    public abstract String type();
    public abstract String describe();
    protected abstract String extra1();
    protected abstract String extra2();

    protected static String peso(long centavos) {
        return String.format("P%.2f", centavos / 100.0);
    }

    /** One ledger CSV row. */
    public String[] toRow() {
        return new String[] { uuid, type(), accountId, cardUid, String.valueOf(amount),
                String.valueOf(balanceBefore), String.valueOf(balanceAfter),
                time.toString(), extra1(), extra2() };
    }

    public static Txn fromRow(String[] r) {
        String uuid = r[0], acc = r[2], card = r[3];
        long amt = Long.parseLong(r[4]), before = Long.parseLong(r[5]), after = Long.parseLong(r[6]);
        Instant at = Instant.parse(r[7]);
        return switch (r[1]) {
            case "CARD_LOAD" -> new CardLoad(uuid, acc, card, amt, before, after, at, r[8]);
            case "FARE_PAYMENT" -> new FarePayment(uuid, acc, card, amt, before, after, at, r[8], Double.parseDouble(r[9]));
            default -> throw new IllegalArgumentException("Unknown txn type " + r[1]);
        };
    }
}
