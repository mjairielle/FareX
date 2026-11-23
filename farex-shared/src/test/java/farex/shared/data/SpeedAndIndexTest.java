package farex.shared.data;

import farex.shared.reward.Rewards;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.Instant;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/** Items 9 and 15. The in memory index must be fast and must never go out of date. */
public class SpeedAndIndexTest {

    static final String CARD = "0067676767";

    @TempDir
    Path dir;

    @Test
    void bigLedgerStaysFastAfterFirstRead() throws Exception {
        new Store(dir);
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 200_000; i++)
            sb.append(UUID.randomUUID()).append(",CARD_LOAD,p9,0011223344,500,0,500000,2026-11-17T00:00:00Z,x,\n");
        Files.writeString(dir.resolve("ledger.txt"), Files.readString(dir.resolve("ledger.txt")) + sb, StandardOpenOption.TRUNCATE_EXISTING);
        Store s = new Store(dir);
        s.balance("0011223344");                                   // first call reads the file once
        long t0 = System.nanoTime();
        for (int i = 0; i < 1000; i++) { s.balance("0011223344"); s.txnCount("0011223344"); s.hasTxn("nope"); }
        long ms = (System.nanoTime() - t0) / 1_000_000;
        assertTrue(ms < 500, "3000 lookups took " + ms + " ms");
        assertEquals(500_000, s.balance("0011223344"));
        assertEquals(500, s.history("p9").size(), "history is capped at the newest 500");
    }

    @Test
    void secondStoreOnSameFolderSeesNewRowsAtOnce() {
        Store a = new Store(dir);
        Store b = new Store(dir);
        long before = b.balance(CARD);
        a.load(CARD, "p1", 5000, "GCash");
        assertEquals(before + 5000, b.balance(CARD));
        a.freezeCard(CARD);
        assertTrue(b.isCardFrozen(CARD));
    }

    @Test
    void indexRebuildsWhenFileIsReplacedByAShorterOne() throws Exception {
        Store s = new Store(dir);
        s.load(CARD, "p1", 5000, "GCash");
        long big = s.balance(CARD);
        Files.writeString(dir.resolve("ledger.txt"),
                "uuid,type,accountId,cardUid,amount,balanceBefore,balanceAfter,time,extra1,extra2\n"
                + "u1,CARD_LOAD,p1," + CARD + ",100,0,100,2026-11-17T00:00:00Z,x,\n");
        assertEquals(100, s.balance(CARD));
        assertNotEquals(big, s.balance(CARD));
    }

    @Test
    void halfWrittenLastLineWaitsAndBadNumberRowIsSkipped() throws Exception {
        Store s = new Store(dir);
        long before = s.balance(CARD);
        Files.writeString(dir.resolve("ledger.txt"), "u2,CARD_LOAD,p1," + CARD + ",1,0,9",
                StandardOpenOption.APPEND);                         // no newline yet, writer is mid write
        assertEquals(before, s.balance(CARD));
        Files.writeString(dir.resolve("ledger.txt"), "99,2026-11-17T00:00:00Z,x,\nu3,CARD_LOAD,p1," + CARD + ",1,0,abc,2026-11-17T00:00:00Z,x,\n",
                StandardOpenOption.APPEND);
        assertEquals(999, s.balance(CARD), "finished row counted, bad number row skipped");
    }

    @Test
    void sameLoadIdCannotCreditTwice() {
        Store s = new Store(dir);
        s.load(CARD, "p1", 5000, "PayMongo", "load-1");
        long after = s.balance(CARD);
        assertThrows(IllegalStateException.class, () -> s.load(CARD, "p1", 5000, "PayMongo", "load-1"));
        assertEquals(after, s.balance(CARD));
    }

    @Test
    void rewardsIndexAndRefund() {
        Rewards a = new Rewards(dir);
        Rewards b = new Rewards(dir);
        a.earn("p1", "trip-1", 12.0, Instant.now());
        assertEquals(12, b.balance("p1"), "other instance sees the new row");
        assertEquals(0, a.earn("p1", "trip-1", 12.0, Instant.now()), "same trip earns once");
        Rewards.Perk perk = a.perk("P10");
        a.spend("p1", perk, Instant.now());
        assertEquals(12 - perk.points(), a.balance("p1"));
        a.refund("p1", perk, Instant.now());
        assertEquals(12, a.balance("p1"));
    }
}
