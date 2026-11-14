package farex.shared.data;

import farex.shared.txn.Txn;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/**
 * Self check for Operator App business logic, no GUI.
 * Run with: java -ea -cp farex-shared/target/classes farex.shared.data.OperatorDemo
 */
public class OperatorDemo {
    public static void main(String[] args) throws Exception {
        Path dir = Files.createTempDirectory("farex-operator-check");
        Store store = new Store(dir);
        TapDesk desk = new TapDesk(store, "o1");

        // 1. Initial State & Login Flexibility
        assert store.login("ben@mail.com", "1234").isPresent() : "login via email ben@mail.com";
        assert store.login("ben", "1234").isPresent() : "login via username ben";
        assert store.login("Ben", "1234").isPresent() : "login via case-insensitive Ben";
        assert desk.onBoard() == 0 : "initial onBoard is 0";
        assert desk.boardedPassengers().isEmpty() : "initial manifest is empty";
        assert desk.stops().size() == 6 : "6 route stops seeded";

        // 2. Physical Card Tap In
        String cardUid = "0067676767";
        TapDesk.BoardedInfo info = desk.tapIn(cardUid, 0); // Philcoa
        assert info.cardUid().equals(cardUid) : "card uid matches";
        assert info.passengerName().equals("Ana") : "passenger name resolved to Ana";
        assert info.stopIdx() == 0 : "boarded at stop 0";
        assert info.stopName().equals("Philcoa") : "stop name is Philcoa";
        assert desk.onBoard() == 1 : "1 passenger on board";
        assert desk.boardedPassengers().size() == 1 : "manifest has 1 item";

        // 3. Double Tap In Prevention
        try {
            desk.tapIn(cardUid, 1);
            assert false : "double tap in must throw";
        } catch (IllegalStateException e) {
            System.out.println("Double tap-in successfully prevented: " + e.getMessage());
        }

        // 4. Invalid Alight Stop (cannot alight at or before boarding stop)
        try {
            desk.tapOut(cardUid, 0);
            assert false : "alight at same stop must throw";
        } catch (IllegalStateException e) {
            System.out.println("Alight at same stop prevented: " + e.getMessage());
        }

        // 5. Successful Tap Out with Student Discount
        long beforeBal = store.balance(cardUid);
        Txn t1 = desk.tapOut(cardUid, 2); // Ever Gotesco
        assert desk.onBoard() == 0 : "onBoard is 0 after tap out";
        assert desk.boardedPassengers().isEmpty() : "manifest empty after tap out";
        long afterBal = store.balance(cardUid);
        assert afterBal < beforeBal : "balance was deducted";
        assert t1.getAmount() == (beforeBal - afterBal) : "txn amount equals deducted balance";
        System.out.println("Tap out successful: " + t1.describe() + " (remaining balance: P" + (afterBal / 100.0) + ")");

        // 6. App User ID / QR Code Tap In and Tap Out
        // Using "p1" directly (Ana's account ID)
        TapDesk.BoardedInfo appInfo = desk.tapIn("p1", 1); // Tandang Sora
        assert appInfo.cardUid().equals(cardUid) : "resolved account p1 to card 0067676767";
        assert desk.onBoard() == 1 : "1 passenger on board via app user id";

        // Also test resolving with "USER:p1" QR format
        assert desk.resolveCard("USER:p1").equals(cardUid) : "resolved USER:p1 format";

        // 7. Missed Tap Out / Fallback Max Fare Charging
        Txn maxTxn = desk.missedTapOut("p1");
        assert desk.onBoard() == 0 : "on board is 0 after max fare charge";
        assert maxTxn.getAmount() > 0 : "max fare charged";
        System.out.println("Fallback max fare charged: " + maxTxn.describe());

        // 8. Operator Transaction History
        List<Txn> opTxns = store.operatorHistory("o1");
        assert opTxns.size() == 2 : "operator has 2 fare payments in ledger";
        assert opTxns.get(0).getUuid().equals(maxTxn.getUuid()) : "newest txn first";
        System.out.println("Operator transaction history has " + opTxns.size() + " records");

        // 9. Ledger Deduplication / Sync append
        String testUuid = "sync-uuid-999";
        assert !store.hasTxn(testUuid) : "uuid does not exist yet";
        String[] row = new String[]{testUuid, "FARE_PAYMENT", "p1", cardUid, "1500", "50000", "48500", "2026-10-30T12:00:00Z", "o1", "1.9"};
        store.appendTxnRow(row);
        assert store.hasTxn(testUuid) : "uuid exists after append";
        // Re-append same row (simulating sync retry)
        store.appendTxnRow(row);
        int count = 0;
        for (Txn tx : store.operatorHistory("o1")) {
            if (tx.getUuid().equals(testUuid)) count++;
        }
        assert count == 1 : "deduplication prevented duplicate ledger row";
        System.out.println("Ledger deduplication verified (sync safe).");

        // 10. Update Profile Name
        store.updateUserName("p1", "Ana Santos");
        assert store.findUser("p1").get().getName().equals("Ana Santos") : "name updated in store";

        // 11. Lost Card Freeze & Replacement Transfer
        long preTransferBal = store.balance(cardUid);
        String newCardUid = store.replaceLostCard(cardUid, "88888888", "p1");
        assert store.isCardFrozen(cardUid) : "old card is frozen";
        assert !store.isCardFrozen(newCardUid) : "new card is not frozen";
        assert store.balance(newCardUid) == preTransferBal : "balance transferred to replacement card";
        assert store.cardsOf("p1").contains(newCardUid) : "replacement card in active cards list";
        assert !store.cardsOf("p1").contains(cardUid) : "frozen card excluded from active cards list";

        // Attempting to tap with frozen card must be rejected
        try {
            desk.tapIn(cardUid, 0);
            assert false : "frozen card tap-in must throw";
        } catch (IllegalStateException e) {
            System.out.println("Frozen card blocked: " + e.getMessage());
        }

        // 12. Fleet & Vehicles Check
        assert !store.allVehicles().isEmpty() : "vehicles list populated";
        assert store.allVehicles().get(0)[0].equals("ABC-123") : "ABC-123 plate verified";

        System.out.println("ALL OPERATOR AND APPLICATION CHECKS PASSED!");
    }
}
<<<<<<< Updated upstream

=======
>>>>>>> Stashed changes
