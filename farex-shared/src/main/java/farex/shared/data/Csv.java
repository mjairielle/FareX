package farex.shared.data;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;

/** Plain CSV. Append only, no edits. Commas inside values become spaces. */
public final class Csv {
    private Csv() {}

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

    public static void append(Path file, String header, String... row) {
        try {
            Files.createDirectories(file.toAbsolutePath().getParent());
            if (!Files.exists(file)) Files.writeString(file, header + "\n", StandardCharsets.UTF_8);
            String[] clean = new String[row.length];
            for (int i = 0; i < row.length; i++) clean[i] = row[i].replace(',', ' ').replace('\n', ' ');
            Files.writeString(file, String.join(",", clean) + "\n", StandardCharsets.UTF_8, StandardOpenOption.APPEND);
        } catch (IOException e) {
            throw new RuntimeException("Cannot write " + file, e);
        }
    }
}
