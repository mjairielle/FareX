package farex.shared.data;

import farex.shared.txn.Txn;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class OperatorTest {

    @TempDir
    Path tempDir;

    private Store store;
    private TapDesk desk;
    private final String cardUid = "0067676767";

    @BeforeEach
    void setUp() {
        store = new Store(tempDir);
        desk = new TapDesk(store, "o1");
    }

    @Test
    void testTapInAndManifest() {
        assertEquals(0, desk.onBoard());
        TapDesk.BoardedInfo info = desk.tapIn(cardUid, 0);

        assertEquals(cardUid, info.cardUid());
        assertEquals("Ana", info.passengerName());
        assertEquals(0, info.stopIdx());
        assertEquals("Philcoa", info.stopName());
        assertEquals(1, desk.onBoard());
        assertEquals(1, desk.boardedPassengers().size());
    }

    @Test
    void testDoubleTapInPrevention() {
        desk.tapIn(cardUid, 0);
        IllegalStateException ex = assertThrows(IllegalStateException.class, () -> desk.tapIn(cardUid, 1));
        assertTrue(ex.getMessage().contains("already tapped in"));
    }

    @Test
    void testInvalidAlightStop() {
        desk.tapIn(cardUid, 2);
        IllegalStateException ex = assertThrows(IllegalStateException.class, () -> desk.tapOut(cardUid, 1));
        assertTrue(ex.getMessage().contains("must not be before board stop"));
    }

    @Test
    void testSameStopPaysBaseFare() {
        desk.tapIn(cardUid, 1);
        long before = store.balance(cardUid);
        Txn txn = desk.tapOut(cardUid, 1);
        assertEquals(before - store.balance(cardUid), txn.getAmount());
        farex.shared.user.Passenger ana = (farex.shared.user.Passenger) store.findUser(store.ownerOf(cardUid)).orElseThrow();
        assertEquals(farex.shared.fare.Fares.fare(0, ana.getFareRule()), txn.getAmount(), "0 km: the base fare, with the rider's discount");
    }

    @Test
    void testSuccessfulTapOut() {
        desk.tapIn(cardUid, 0);
        Txn txn = desk.tapOut(cardUid, 2);

        assertNotNull(txn);
        assertEquals(0, desk.onBoard());
        assertTrue(store.balance(cardUid) < 50000, "Balance reduced by fare");
    }

    @Test
    void testFallbackMaxFare() {
        desk.tapIn(cardUid, 0);
        Txn maxTxn = desk.missedTapOut(cardUid);

        assertNotNull(maxTxn);
        assertEquals(0, desk.onBoard());
        assertTrue(maxTxn.describe().contains("Fare"));
    }

    @Test
    void testFrozenCardTapBlocked() {
        store.freezeCard(cardUid);
        IllegalStateException ex = assertThrows(IllegalStateException.class, () -> desk.tapIn(cardUid, 0));
        assertTrue(ex.getMessage().contains("frozen"));
    }
}
