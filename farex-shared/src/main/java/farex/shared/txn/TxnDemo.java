package farex.shared.txn;

import java.time.Instant;

public class TxnDemo {
    public static void main(String[] args) {
        Txn[] history = {
            new CardLoad("u1", "acc1", "card1", 50000, 10000, 60000, Instant.now(), "GCash"),
            new FarePayment("u2", "acc1", "card1", 1640, 60000, 58360, Instant.now(), "op1", 5.0)
        };
        for (Txn t : history) {
            System.out.println(t.describe());
        }
    }
}