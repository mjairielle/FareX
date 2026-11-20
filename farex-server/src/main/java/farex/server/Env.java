package farex.server;

import java.nio.file.*;
import java.util.*;

/** Real env var wins, then .env in the working folder. */
final class Env {
    private static final Map<String, String> FILE = new HashMap<>();
    static {
        try {
            for (String l : Files.readAllLines(Path.of(".env"))) {
                // Notepad's BOM, and non-breaking / zero-width spaces pasted from chat or Word, are never meant
                l = l.replaceAll("[\uFEFF\u00A0\u200B-\u200D\u2060]", "").strip();
                int i = l.indexOf('=');
                if (i > 0 && l.charAt(0) != '#') FILE.put(l.substring(0, i).strip(), l.substring(i + 1).strip());
            }
        } catch (Exception noEnvFile) {
            System.out.println("No .env file in " + Path.of("").toAbsolutePath() + " (check it is not named .env.txt)");
        }
    }
    static String get(String k) { String v = System.getenv(k); return v != null ? v : FILE.get(k); }

    /** Where k comes from, for the start-up log: a Windows/Linux environment variable wins over .env. */
    static String source(String k) {
        return System.getenv(k) != null ? "an environment variable (it overrides .env)" : FILE.containsKey(k) ? ".env" : "not set";
    }
    static String get(String k, String dflt) { String v = get(k); return v == null || v.isBlank() ? dflt : v; }
}
