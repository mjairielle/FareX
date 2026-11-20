package farex.shared.data;

import farex.shared.txn.Txn;
import java.nio.file.*;

/** Self check, no GUI. Run with: java -ea -cp farex-shared/target/classes farex.shared.data.StoreDemo */
public class StoreDemo {
    public static void main(String[] args) throws Exception {
        Path dir = Files.createTempDirectory("farex-check");
        Store store = new Store(dir);

        assert store.login("ana@mail.com", "1234").isPresent() : "login ok";
        assert store.login("ana@mail.com", "bad").isEmpty() : "login bad";
        assert store.balance("0067676767") == 50000 : "seed balance";

        TapDesk desk = new TapDesk(store, "o1");
        desk.tapIn("0067676767", 0);
        Txn t = desk.tapOut("0067676767", 2);               // student, 20% off
        System.out.println(t.describe() + " -> balance P" + store.balance("0067676767") / 100.0);
        assert store.balance("0067676767") < 50000 : "fare deducted";

        desk.tapIn("0067676767", 0);
        Txn max = desk.missedTapOut("0067676767");
        System.out.println(max.describe());

        store.load("0067676767", "p1", 10000, "GCash");
        System.out.println(store.history("p1").size() + " receipts, newest: " + store.history("p1").get(0).describe());

        try { store.pay("0067676767", "o1", 99999999, 1); assert false; }
        catch (IllegalStateException e) { System.out.println("Blocked: " + e.getMessage()); }

        System.out.println(store.summary());
        System.out.println("ALL CHECKS PASSED");
    }
}
