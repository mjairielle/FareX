package farex.shared.data;

import static org.junit.jupiter.api.Assertions.*;

import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

// Data files moved from .csv to .txt: the old files must carry over, and an existing .txt must never be overwritten
class CsvRenameTest {

    @TempDir
    Path dir;

    @Test
    void oldCsvBecomesTxtWithItsContent() throws Exception {
        Files.writeString(dir.resolve("users.csv"), "id,name\nu1,Ana\n");
        assertEquals(1, Csv.renameOldCsv(dir));
        assertFalse(Files.exists(dir.resolve("users.csv")));
        assertEquals("id,name\nu1,Ana\n", Files.readString(dir.resolve("users.txt")));
        assertEquals("Ana", Csv.read(dir.resolve("users.txt")).get(0)[1]);
    }

    @Test
    void existingTxtWins() throws Exception {
        Files.writeString(dir.resolve("routes.txt"), "new");
        Files.writeString(dir.resolve("routes.csv"), "old");
        assertEquals(0, Csv.renameOldCsv(dir));
        assertEquals("new", Files.readString(dir.resolve("routes.txt")));
        assertTrue(Files.exists(dir.resolve("routes.csv")), "left alone, never deleted");
    }

    @Test
    void noFolderYetIsFine() {
        assertEquals(0, Csv.renameOldCsv(dir.resolve("missing")));
    }

    @Test
    void storeStartsFromRenamedFiles() throws Exception {
        new Store(dir);                                     // creates its .txt files (demo accounts)
        for (String f : new String[]{"users", "cards", "ledger"}) {
            assertTrue(Files.exists(dir.resolve(f + ".txt")), f + ".txt created");
            assertFalse(Files.exists(dir.resolve(f + ".csv")), "no " + f + ".csv");
        }
    }
}
