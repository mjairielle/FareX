package farex.shared.data;

import farex.shared.fare.Fares;
import farex.shared.user.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/** Regression tests for bugs found in the launch review. Each test is one exploit that worked before the fix. */
public class HardeningTest {

    @TempDir
    Path dir;

    private Store store;

    @BeforeEach
    void setUp() {
        store = new Store(dir);
    }

    @Test
    void renameWithCommaCannotChangeRole() {
        store.updateUserName("p1", "a,b");
        User u = store.findUser("p1").orElseThrow();
        assertEquals("PASSENGER", u.role());
        assertEquals("a b", u.getName());
        assertTrue(store.login("ana@mail.com", "1234").isPresent(), "login still works");
    }

    @Test
    void renameWithNewlineStaysOneRow() throws Exception {
        store.updateUserName("p1", "Ana\nx,y,z");
        assertEquals(4, Files.readAllLines(dir.resolve("users.txt")).size());   // header + 3 users
    }

    @Test
    void registerNameWithCarriageReturnCannotAddRow() throws Exception {
        store.register("Bob\r1,fake,x", "bob@x.com", "pass1234", "PASSENGER", "REGULAR", "");
        assertEquals(5, Files.readAllLines(dir.resolve("users.txt")).size());
        assertTrue(store.login("ana@mail.com", "1234").isPresent());
    }

    @Test
    void registerEmailWithCommaIsRejected() {
        assertThrows(IllegalArgumentException.class,
                () -> store.register("A", "a,b@x.com", "pass1234", "PASSENGER", "REGULAR", ""));
        assertThrows(IllegalArgumentException.class,
                () -> store.register("A", "no-at-sign", "pass1234", "PASSENGER", "REGULAR", ""));
    }

    @Test
    void shortLedgerRowCannotBreakHistoryOrBalance() {
        long before = store.balance("0067676767");
        store.appendTxnRow(new String[]{"poison-1", "FARE_PAYMENT", "p1", "0067676767", "100", "1", "1", "2026-11-17T00:00:00Z"});
        assertEquals(before, store.balance("0067676767"));
        assertDoesNotThrow(() -> store.history("p1"));
    }

    @Test
    void badFaresRowIsSkippedNotThrown() throws Exception {
        Files.writeString(dir.resolve("fares.txt"), Fares.HEADER + "\noops,1,2,3\n2020-01-01,notnum,200,x\n2020-01-01\n");
        long base = Fares.BASE;
        assertDoesNotThrow(() -> Fares.load(dir));
        assertEquals(base, Fares.BASE);
    }

    @Test
    void zeroAndNegativeFaresAreRejected() {
        assertThrows(IllegalArgumentException.class, () -> store.pay("0067676767", "o1", -5000, 1));
        assertThrows(IllegalArgumentException.class, () -> store.pay("0067676767", "o1", 0, 1));
    }

    @Test
    void unknownRoleIsNeverAdmin() throws Exception {
        Files.writeString(dir.resolve("users.txt"),
                "id,name,email,passwordHash,role,passengerType,vehiclePlate\nz9,Z,z@x.com,h,WEIRD,,\n");
        assertThrows(IllegalStateException.class, () -> new Store(dir).findUser("z9"));
    }

    @Test
    void productionSeedHasOnlyAdminAndNoDemoLogins(@TempDir Path other) {
        Store prod = new Store(other, "S3cretPass!", false);
        assertTrue(prod.login("admin@mail.com", "S3cretPass!").isPresent());
        assertTrue(prod.login("ana@mail.com", "1234").isEmpty());
        assertFalse(prod.hasDefaultLogins());
        assertTrue(store.hasDefaultLogins(), "demo store keeps demo passwords");
    }

    @Test
    void setPasswordWorksAndLeavesNoTempFile() {
        store.setPassword("a1", "N3wPassword!");
        assertTrue(store.login("admin@mail.com", "N3wPassword!").isPresent());
        assertTrue(store.login("admin@mail.com", "admin123").isEmpty());
        assertFalse(Files.exists(dir.resolve("users.txt.tmp")));
        assertThrows(IllegalArgumentException.class, () -> store.setPassword("a1", "short"));
    }

    @Test
    void damagedRouteRowDoesNotStopRouteBook() throws Exception {
        Files.writeString(dir.resolve("routes.txt"), "routeId,seq,name,lat,lng\nR1,0,A,14.1,121.1\nR1,x,B,14.2,121.2\nR1,1\nR1,2,C,14.3,121.3\n");
        RouteBook b = new RouteBook(dir);
        assertEquals(2, b.routes().get("R1").size(), "good rows kept, bad rows skipped");
    }

    @Test
    void newRouteIsWrittenInOneWriteAndDuplicateIsRejected() throws Exception {
        RouteBook.addRoute(dir, "R9", java.util.List.of(new String[]{"A", "14.1", "121.1"}, new String[]{"B", "14.2", "121.2"}));
        assertEquals(2, new RouteBook(dir).routes().get("R9").size());
        assertThrows(IllegalArgumentException.class, () -> RouteBook.addRoute(dir, "R9",
                java.util.List.of(new String[]{"A", "14.1", "121.1"}, new String[]{"B", "14.2", "121.2"})));
    }

    @Test
    void mountedStopsFileIsNotSeededTwice(@TempDir Path other) throws Exception {
        Files.writeString(other.resolve("stops.txt"), "seq,name,lat,lng\n0,Only,14.1,121.1\n");
        new Store(other, "S3cretPass!", false);
        assertEquals(1, Files.readAllLines(other.resolve("stops.txt")).size() - 1, "no duplicate stops added");
    }

    @Test
    void oldFareRowsWithoutBaseKmStillLoadAndBadOnesAreSkipped() throws Exception {
        Files.writeString(dir.resolve("fares.txt"), Fares.HEADER + "\n2020-01-01,1400,200,x\n2020-02-01,1500,250,x,5.0\nbroken\n2020-03-01,zz,1,x\n");
        Fares.load(dir);
        assertEquals(1500, Fares.BASE);
        assertEquals(250, Fares.RATE_PER_KM);
        assertEquals(5.0, Fares.BASE_KM);
        Files.delete(dir.resolve("fares.txt"));
        Fares.publish(dir, java.time.LocalDate.of(2020, 1, 1), 1400, 200, 4.0);   // restore the defaults other tests expect
    }
}
