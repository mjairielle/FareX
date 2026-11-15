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
        desk.tapIn(cardUid, 1);
        IllegalStateException ex = assertThrows(IllegalStateException.class, () -> desk.tapOut(cardUid, 1));
        assertTrue(ex.getMessage().contains("must be after board stop"));
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
