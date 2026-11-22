package farex.shared.data;

import farex.shared.fare.Fares;
import farex.shared.txn.Txn;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/** Free ride fixes (7) and password fixes (8). Each test is a problem found in the launch review. */
public class LaunchFixesTest {

    static final String CARD = "0067676767";

    @TempDir
    Path dir;

    private Store store;
    private TapDesk desk;

    @BeforeEach
    void setUp() {
        store = new Store(dir);
        desk = new TapDesk(store, "o1");
    }

    private void leaveBalance(long cents) {
        store.pay(CARD, "o1", store.balance(CARD) - cents, 1);
    }

    // ---------- 7: free rides ----------

    @Test
    void cannotTapInBelowBaseFare() {
        leaveBalance(Fares.BASE - 1);
        IllegalStateException ex = assertThrows(IllegalStateException.class, () -> desk.tapIn(CARD, 0));
        assertTrue(ex.getMessage().contains("Balance too low"));
        assertEquals(0, desk.onBoard());
    }

    @Test
    void shortRiderStillPaysThenIsBlockedUntilLoaded() {
        leaveBalance(Fares.BASE);
        desk.tapIn(CARD, 0);
        Txn t = desk.tapOut(CARD, 5);                          // long trip, fare is more than the balance
        assertEquals(0, desk.onBoard());
        assertTrue(store.balance(CARD) < 0, "balance went negative, fare was collected");
        assertThrows(IllegalStateException.class, () -> desk.tapIn(CARD, 0));
        store.load(CARD, "p1", 5000, "GCash");
        assertTrue(store.balance(CARD) >= Fares.BASE, "one load clears the debt");
        assertDoesNotThrow(() -> desk.tapIn(CARD, 0));
        assertNotNull(t);
    }

    @Test
    void sameCardCannotBoardTwoJeepneys() {
        desk.tapIn(CARD, 0);
        TapDesk other = new TapDesk(store, "o2");
        IllegalStateException ex = assertThrows(IllegalStateException.class, () -> other.tapIn(CARD, 1));
        assertTrue(ex.getMessage().contains("already tapped in"));
    }

    @Test
    void ridersStayOnBoardAfterServerRestart() {
        desk.tapIn(CARD, 0);
        TapDesk restarted = new TapDesk(store, "o1");
        assertEquals(1, restarted.onBoard());
        assertNotNull(restarted.tapOut(CARD, 2));
        assertEquals(0, new TapDesk(store, "o1").onBoard(), "paid rider is not on board again");
    }

    @Test
    void endOfShiftClearFreesTheCard() {
        desk.tapIn(CARD, 0);
        desk.clearBoarded();
        assertEquals(0, new TapDesk(store, "o1").onBoard());
        assertDoesNotThrow(() -> new TapDesk(store, "o2").tapIn(CARD, 0));
    }

    @Test
    void oldBoardingDoesNotLockTheCardForever() throws Exception {
        Files.writeString(dir.resolve("boarding.csv"),
                "uid,operatorId,stopIdx,time,event\n" + CARD + ",o2,0,2020-01-01T00:00:00Z,BOARD\n");
        assertDoesNotThrow(() -> desk.tapIn(CARD, 0));
    }

    // ---------- 8: passwords ----------

    @Test
    void sameImpossibleToSpotPasswordsGetDifferentSaltedHashes() throws Exception {
        store.register("A", "a@x.com", "samePass123", "PASSENGER", "REGULAR", "");
        store.register("B", "b@x.com", "samePass123", "PASSENGER", "REGULAR", "");
        List<String> hashes = Files.readAllLines(dir.resolve("users.csv")).stream()
                .filter(l -> l.contains("@x.com")).map(l -> l.split(",")[3]).toList();
        assertEquals(2, hashes.size());
        assertNotEquals(hashes.get(0), hashes.get(1));
        assertTrue(hashes.get(0).startsWith("pbkdf2$"));
        assertTrue(store.login("a@x.com", "samePass123").isPresent());
        assertTrue(store.login("a@x.com", "samePass124").isEmpty());
    }

    @Test
    void oldSha256HashStillWorksAndUpgradesOnLogin() throws Exception {
        byte[] d = MessageDigest.getInstance("SHA-256").digest("oldPassword1".getBytes("UTF-8"));
        StringBuilder sb = new StringBuilder();
        for (byte b : d) sb.append(String.format("%02x", b));
        Files.writeString(dir.resolve("users.csv"), Files.readString(dir.resolve("users.csv"))
                + "z1,Old,old@x.com," + sb + ",PASSENGER,REGULAR,\n");
        assertTrue(store.login("old@x.com", "oldPassword1").isPresent());
        assertTrue(Files.readString(dir.resolve("users.csv")).contains("old@x.com,pbkdf2$"));
        assertTrue(store.login("old@x.com", "oldPassword1").isPresent(), "works again after the upgrade");
    }

    @Test
    void passwordUnder8CharactersIsRejected() {
        assertThrows(IllegalArgumentException.class,
                () -> store.register("A", "a@x.com", "1234567", "PASSENGER", "REGULAR", ""));
    }
}
