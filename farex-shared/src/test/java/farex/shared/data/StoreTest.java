package farex.shared.data;

import farex.shared.txn.Txn;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

public class StoreTest {

    @TempDir
    Path tempDir;

    private Store store;

    @BeforeEach
    void setUp() {
        store = new Store(tempDir);
    }

    @Test
    void testLoginWithEmailAndUsername() {
        assertTrue(store.login("ana@mail.com", "1234").isPresent(), "Login via email");
        assertTrue(store.login("ana", "1234").isPresent(), "Login via username");
        assertTrue(store.login("Ana", "1234").isPresent(), "Login case-insensitive");
        assertTrue(store.login("ana@mail.com", "wrongpass").isEmpty(), "Login with wrong password should fail");
    }

    @Test
    void testInitialBalanceAndLoading() {
        assertEquals(50000, store.balance("0067676767"), "Seed balance of Ana's card");
        store.load("0067676767", "p1", 10000, "GCash");
        assertEquals(60000, store.balance("0067676767"), "Balance after loading P100.00");
    }

    @Test
    void testPaymentAndInsufficientBalance() {
        store.pay("0067676767", "o1", 1000, 1.0);
        assertEquals(49000, store.balance("0067676767"), "Balance after fare deduction");

        assertThrows(IllegalStateException.class, () -> {
            store.pay("0067676767", "o1", 99999999, 1.0);
        }, "Should throw on insufficient balance");
    }

    @Test
    void testCardFreezingAndReplacement() {
        assertFalse(store.isCardFrozen("0067676767"));
        store.freezeCard("0067676767");
        assertTrue(store.isCardFrozen("0067676767"));

        String newUid = store.replaceLostCard("0067676767", "99887766", "p1");
        assertNotNull(newUid);
        assertTrue(store.isCardFrozen("0067676767"), "Old card remains frozen");
        assertFalse(store.isCardFrozen(newUid), "New card is active");
        assertEquals(50000, store.balance(newUid), "Balance transferred to replacement card");
    }
}
