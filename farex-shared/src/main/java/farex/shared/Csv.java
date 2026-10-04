package farex.shared;

import java.nio.file.Path;
import java.util.List;

/**
 * The only file IO in the system. Replaces CsvStore, CsvRepository, LedgerRepository.
 * Server and operator phone both use it.
 */
public final class Csv {
    private Csv() {}

    /** One lock for all writes. ponytail: global lock, per-file locks if throughput matters. */
    private static final Object LOCK = new Object();

    public static List<String[]> readAll(Path file) {
        // TODO: return [] if file missing, skip header, split rows. Decide quoting rule for commas in names.
        throw new UnsupportedOperationException("TODO");
    }

    /** Append one row. Ledger files only ever use this. */
    public static void append(Path file, String... row) {
        synchronized (LOCK) {
            // TODO: create file with header if missing, write row, flush
            throw new UnsupportedOperationException("TODO");
        }
    }

    /** Replace a whole file. For mutable files only (users, cards, fares). Never for the ledger. */
    public static void rewrite(Path file, List<String[]> rows) {
        synchronized (LOCK) {
            // TODO: write temp file, then atomic move over the real file, so a crash never leaves half a file
            throw new UnsupportedOperationException("TODO");
        }
    }
}
