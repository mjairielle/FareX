package com.farex;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.InetAddress;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * The phone's NFC card reader. It lives on the Android side (src/android/java/com/farex/android/NfcBridge),
 * which this native image cannot call directly, so we ask it over localhost. A laptop has none.
 */
public final class CardReader {
    private CardReader() {}

    // NfcBridge writes "port token" here at app start; user.home is the app's private data folder on Android
    private static Path bridge() { return Path.of(System.getProperty("user.home"), "farex-nfc"); }

    public static boolean available() { return Files.exists(bridge()); }

    /** Waits up to 30 s for a card on the phone. Returns {uid, payload hex}. */
    public static String[] read() throws IOException {
        String[] r = ask("READ", 35_000).split(" ");
        if (r.length != 2) throw new IOException("bad answer from the card reader");
        return r;
    }

    /** Writes 16 bytes (32 hex) to the card just read. Fails if another card is on the phone now. */
    public static void write(String uid, String payloadHex) throws IOException {
        ask("WRITE " + uid + " " + payloadHex, 10_000);
    }

    private static String ask(String request, int timeoutMs) throws IOException {
        String[] portToken = Files.readString(bridge()).trim().split(" ");
        try (Socket s = new Socket(InetAddress.getLoopbackAddress(), Integer.parseInt(portToken[0]))) {
            s.setSoTimeout(timeoutMs);
            s.getOutputStream().write((portToken[1] + " " + request + "\n").getBytes(StandardCharsets.UTF_8));
            String r = new BufferedReader(new InputStreamReader(s.getInputStream(), StandardCharsets.UTF_8)).readLine();
            if (r == null) throw new IOException("no answer from the card reader");
            if (!r.startsWith("OK")) throw new IOException(r.replaceFirst("^ERR ?", ""));
            return r.substring(2).trim();
        }
    }
}
