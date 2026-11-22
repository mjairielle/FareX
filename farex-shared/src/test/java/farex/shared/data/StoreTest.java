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

    // users.csv is cached in memory: every write, and an edit made outside Store, must still show at once
    @Test
    void userCacheSeesEveryChange() {
        assertEquals("STUDENT", ((farex.shared.user.Passenger) store.findUser("p1").get()).getPassengerType());
        store.setPassengerType("p1", "SENIOR");
        assertEquals("SENIOR", ((farex.shared.user.Passenger) store.findUser("p1").get()).getPassengerType());
        store.updateUserName("p1", "Ana Cruz");
        assertEquals("Ana Cruz", store.findUser("p1").get().getName());
        store.setPhone("p1", "09171234567");
        assertEquals("09171234567", store.phoneOf("p1"));
        farex.shared.user.User op = store.addRole("p1", "OPERATOR", "XYZ-1");
        assertEquals(2, store.siblings("p1").size(), "new role row seen");
        assertEquals("XYZ-1", ((farex.shared.user.Operator) store.findUser(op.getId()).get()).getVehiclePlate());
        Csv.append(tempDir.resolve("users.csv"), "", "x9", "Hand Edit", "hand@mail.com", "h", "PASSENGER", "REGULAR", "");
        assertTrue(store.findUser("x9").isPresent(), "row added by hand to the file is seen");
        store.deleteAccount("p1");
        assertTrue(store.findUser("p1").isEmpty() && store.findUser(op.getId()).isEmpty(), "deleted rows are gone");
    }

    @Test
    void onBoardAndAllCardsOf() {
        String card = "0067676767";
        assertFalse(store.isOnBoard(card));
        store.logBoarding(card, "o1", 0, "BOARD");
        assertTrue(store.isOnBoard(card));
        assertEquals(store.activeBoardings().containsKey(card), store.isOnBoard(card));
        store.logBoarding(card, "o1", 2, "ALIGHT");
        assertFalse(store.isOnBoard(card));
        store.freezeCard(card);
        assertTrue(store.cardsOf("p1").isEmpty(), "frozen card is not active");
        assertEquals(java.util.List.of(card), store.allCardsOf("p1"), "but is still one of the owner's cards");
    }

    @Test
    void unlinkedCardCanBeAddedByAnotherAccountWithItsBalance() {
        String card = store.addCard("5389CB02", "p1");
        store.load(card, "p1", 10000, "Test");
        long before = store.balance(card);
        IllegalArgumentException taken = assertThrows(IllegalArgumentException.class, () -> store.addCard(card, "o1"));
        assertEquals("Card is already linked to an account", taken.getMessage());
        store.unlinkCard(card);
        assertNull(store.ownerOf(card));
        assertFalse(store.cardsOf("p1").contains(card));
        store.addCard(card, "o1");
        assertEquals("o1", store.ownerOf(card), "another account adds it after the unlink");
        assertEquals(before, store.balance(card), "the money stays on the card");
        assertThrows(IllegalArgumentException.class, () -> store.unlinkCard("DEADBEEF"), "unknown card");
    }

    @Test
    void plateCannotBeRegisteredTwice() {
        // seeded operator Ben drives ABC-123
        IllegalArgumentException dup = assertThrows(IllegalArgumentException.class,
                () -> store.register("Cy", "cy@mail.com", "password1", "OPERATOR", "", "abc 123"));
        assertEquals("Plate abc 123 is already registered to another operator", dup.getMessage());
        assertThrows(IllegalArgumentException.class, () -> store.addRole("p1", "OPERATOR", "ABC123"), "same plate as a second role");
        store.register("Cy", "cy@mail.com", "password1", "OPERATOR", "", "XYZ-789");
        store.deleteAccount("o1");
        store.register("Di", "di@mail.com", "password1", "OPERATOR", "", "ABC-123");   // deleting Ben freed his plate
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
    void testCardIdIsTagUid() {
        assertEquals("5389CB02", store.addCard("53:89:cb:02", "p1"));
        assertThrows(IllegalArgumentException.class, () -> store.addCard("5389CB0", "p1"));
        assertThrows(IllegalArgumentException.class, () -> store.addCard("53 89 CB 02", "p1"), "already linked");
        assertEquals("53 89 CB 02", Store.formatUid("5389CB02"));
        assertEquals("00 6767 6767", Store.formatUid("0067676767"));
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

    @Test
    void testOneEmailBothRoles() {
        store.register("Juan", "juan@mail.com", "secret123", "PASSENGER", "STUDENT", "");
        IllegalArgumentException wrongPw = assertThrows(IllegalArgumentException.class,
                () -> store.register("X", "juan@mail.com", "otherpass1", "OPERATOR", "", "ABC-1"));
        assertTrue(wrongPw.getMessage().contains("same password"));
        assertThrows(IllegalArgumentException.class,
                () -> store.register("Juan", "juan@mail.com", "secret123", "PASSENGER", "REGULAR", ""), "same role twice");
        store.register("Other Name", "juan@mail.com", "secret123", "OPERATOR", "", "ABC-1");
        var roles = store.loginAll("juan@mail.com", "secret123");
        assertEquals(2, roles.size());
        assertEquals("Juan", roles.get(1).getName(), "operator row keeps the account name");
        store.setPassword(roles.get(0).getId(), "newpass123");
        assertEquals(2, store.loginAll("juan@mail.com", "newpass123").size(), "password change covers both roles");
        assertThrows(IllegalArgumentException.class, () -> store.addRole(roles.get(0).getId(), "OPERATOR", "X"), "already has it");
        assertThrows(IllegalArgumentException.class,
                () -> store.register("A", "admin@mail.com", "admin123", "PASSENGER", "REGULAR", ""), "admin email stays admin only");
        var ben = store.login("ben@mail.com", "1234").get();
        store.addRole(ben.getId(), "PASSENGER", "");
        assertEquals(2, store.siblings(ben.getId()).size());
    }

    @Test
    void testPhoneAndDeleteAccount() {
        var p = store.register("Lea", "lea@mail.com", "secret123", "PASSENGER", "REGULAR", "");
        store.setPhone(p.getId(), "09171234567");
        var o = store.register("Lea", "lea@mail.com", "secret123", "OPERATOR", "", "LEA-1");
        assertEquals("09171234567", store.phoneOf(o.getId()), "phone is per account");
        assertThrows(IllegalArgumentException.class, () -> store.setPhone(p.getId(), "12345"));
        String uid = store.addCard("1234ABCD", p.getId());
        store.load(uid, p.getId(), 10000, "GCash");
        store.deleteAccount(o.getId());
        assertTrue(store.loginAll("lea@mail.com", "secret123").isEmpty(), "both roles gone");
        assertEquals(0, store.balance(uid), "balance written off");
        assertTrue(store.isCardFrozen(uid), "card can no longer ride");
        assertTrue(store.login("ana@mail.com", "1234").isPresent(), "other users untouched");
        assertThrows(IllegalArgumentException.class, () -> store.deleteAccount("a1"), "admin cannot be deleted");
    }
}
