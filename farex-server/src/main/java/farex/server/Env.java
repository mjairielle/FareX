package farex.server;

import java.nio.file.*;
import java.util.*;

/** Real env var wins, then .env in the working folder. */
final class Env {
    private static final Map<String, String> FILE = new HashMap<>();
    static {
        try {
            for (String l : Files.readAllLines(Path.of(".env"))) {
                l = l.strip();
                int i = l.indexOf('=');
                if (i > 0 && l.charAt(0) != '#') FILE.put(l.substring(0, i).strip(), l.substring(i + 1).strip());
            }
        } catch (Exception noEnvFile) { /* .env is optional */ }
    }
    static String get(String k) { String v = System.getenv(k); return v != null ? v : FILE.get(k); }
    static String get(String k, String dflt) { String v = get(k); return v == null || v.isBlank() ? dflt : v; }
}
