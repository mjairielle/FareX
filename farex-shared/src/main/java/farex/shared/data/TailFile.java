package farex.shared.data;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SeekableByteChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * Reads only what was added to an append only CSV since the last call. Lets a cache stay current
 * without rereading the whole file on every tap. A half written last line waits for the next call.
 * If the file got shorter (someone replaced it) onReset runs and the file is read again from the top.
 */
public final class TailFile {
    private final Path file;
    private long pos;

    public TailFile(Path file) { this.file = file; }

    public synchronized List<String[]> poll(int cols, Runnable onReset) {
        try {
            long size = Files.exists(file) ? Files.size(file) : 0;
            if (size < pos) { onReset.run(); pos = 0; }
            if (size == pos) return List.of();
            ByteBuffer bb = ByteBuffer.allocate((int) (size - pos));
            try (SeekableByteChannel c = Files.newByteChannel(file)) {
                c.position(pos);
                while (bb.hasRemaining() && c.read(bb) > 0) { /* keep reading */ }
            }
            byte[] buf = bb.array();
            int end = bb.position() - 1;
            while (end >= 0 && buf[end] != '\n') end--;
            if (end < 0) return List.of();
            boolean fromTop = pos == 0;
            pos += end + 1;
            String[] lines = new String(buf, 0, end + 1, StandardCharsets.UTF_8).split("\n", -1);
            List<String[]> rows = new ArrayList<>(lines.length);
            for (int i = fromTop ? 1 : 0; i < lines.length; i++) {          // first line of the file is the header
                String l = lines[i].endsWith("\r") ? lines[i].substring(0, lines[i].length() - 1) : lines[i];
                if (l.isBlank()) continue;
                String[] r = l.split(",", -1);
                if (r.length >= cols) rows.add(r); else System.err.println("Skipped short row in " + file.getFileName() + ": " + l);
            }
            return rows;
        } catch (IOException e) {
            throw new RuntimeException("Cannot read " + file, e);
        }
    }
}
