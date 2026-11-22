package farex.shared.data;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;

/** Plain CSV. Every cell goes through clean(), so a value can never add a column or a row. */
public final class Csv {
    private Csv() {}

    /** Commas, CR and LF become spaces. The only way text enters a CSV file. */
    public static String clean(String s) {
        return s.replace(',', ' ').replace('\r', ' ').replace('\n', ' ');
    }

    /** Size and last change time: equal stamps mean the file did not change since it was read. */
    public static Object stamp(Path file) {
        try {
            return Files.exists(file) ? List.of(Files.size(file), Files.getLastModifiedTime(file)) : "missing";
        } catch (IOException e) {
            return new Object();                               // never equal: read the file again
        }
    }

    public static List<String[]> read(Path file) {
        List<String[]> rows = new ArrayList<>();
        try {
            if (!Files.exists(file)) return rows;
            List<String> lines = Files.readAllLines(file, StandardCharsets.UTF_8);
            for (int i = 1; i < lines.size(); i++) {           // line 0 is header
                if (!lines.get(i).isBlank()) rows.add(lines.get(i).split(",", -1));
            }
            return rows;
        } catch (IOException e) {
            throw new RuntimeException("Cannot read " + file, e);
        }
    }

    /** Like read, but drops rows with fewer than cols columns, so one bad row cannot crash every caller. */
    public static List<String[]> read(Path file, int cols) {
        List<String[]> rows = read(file);
        rows.removeIf(r -> {
            if (r.length >= cols) return false;
            System.err.println("Skipped short row in " + file.getFileName() + ": " + String.join(",", r));
            return true;
        });
        return rows;
    }

    public static void append(Path file, String header, String... row) {
        try {
            Files.createDirectories(file.toAbsolutePath().getParent());
            if (!Files.exists(file)) Files.writeString(file, header + "\n", StandardCharsets.UTF_8);
            String[] cells = new String[row.length];
            for (int i = 0; i < row.length; i++) cells[i] = clean(row[i]);
            Files.writeString(file, String.join(",", cells) + "\n", StandardCharsets.UTF_8, StandardOpenOption.APPEND);
        } catch (IOException e) {
            throw new RuntimeException("Cannot write " + file, e);
        }
    }

    /** Adds several rows in one write. */
    public static void appendAll(Path file, String header, List<String[]> rows) {
        try {
            Files.createDirectories(file.toAbsolutePath().getParent());
            StringBuilder sb = new StringBuilder();
            if (!Files.exists(file)) sb.append(header).append('\n');
            for (String[] r : rows) {
                String[] cells = new String[r.length];
                for (int i = 0; i < r.length; i++) cells[i] = clean(r[i]);
                sb.append(String.join(",", cells)).append('\n');
            }
            Files.writeString(file, sb.toString(), StandardCharsets.UTF_8, StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        } catch (IOException e) {
            throw new RuntimeException("Cannot write " + file, e);
        }
    }

    /** Replaces the whole file through a temp file and an atomic move, so a crash never leaves it half written. */
    public static void rewrite(Path file, String header, List<String[]> rows) {
        try {
            StringBuilder sb = new StringBuilder(header).append('\n');
            for (String[] r : rows) {
                String[] cells = new String[r.length];
                for (int i = 0; i < r.length; i++) cells[i] = clean(r[i]);
                sb.append(String.join(",", cells)).append('\n');
            }
            Path tmp = file.resolveSibling(file.getFileName() + ".tmp");
            Files.writeString(tmp, sb.toString(), StandardCharsets.UTF_8);
            Files.move(tmp, file, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        } catch (IOException e) {
            throw new RuntimeException("Cannot write " + file, e);
        }
    }
}
