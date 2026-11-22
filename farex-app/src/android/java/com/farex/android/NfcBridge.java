package com.farex.android;

import android.app.Activity;
import android.app.Application;
import android.nfc.NfcAdapter;
import android.nfc.Tag;
import android.os.Bundle;
import android.util.Log;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;

/**
 * UNTESTED on a phone. The card reader, Android side.
 * The JavaFX app is a separate native image and cannot touch Android classes, so it asks this over a localhost
 * socket (com.farex.CardReader). Port and a random token go in dataDir/farex-nfc, which is private to the app
 * and is the JavaFX side's user.home, so other apps on the phone cannot drive the reader.
 * One line per connection, each on its own thread:
 *   "token NEXT"           -> "OK uid"      the next card tapped (waits up to 30 s); the app's tap listener
 *   "token READ"           -> "OK uid hex"  the card on the phone now, else the next one (Fetch Load)
 *   "token WRITE uid hex"  -> "OK"
 * Anything wrong -> "ERR why".
 */
public class NfcBridge extends Application implements Application.ActivityLifecycleCallbacks, NfcAdapter.ReaderCallback {
    private static final String TAG = "FarexNfc";
    private static final long WAIT_MS = 30_000;

    private final Object lock = new Object();
    private Tag tag;        // the last card seen, may have left the phone since
    private long taps;      // counts taps, so NEXT can wait for a new one
    private String token;

    @Override
    public void onCreate() {
        super.onCreate();
        registerActivityLifecycleCallbacks(this);
        // no lambda or this::serve: compiled against android.jar, which lacks LambdaMetafactory
        Thread t = new Thread(new Runnable() { @Override public void run() { serve(); } }, "farex-nfc");
        t.setDaemon(true);
        t.start();
    }

    // Set once at create: Android itself turns reader mode on at every resume and off at every pause
    @Override
    public void onActivityCreated(Activity a, Bundle saved) {
        NfcAdapter nfc = NfcAdapter.getDefaultAdapter(a);
        if (nfc == null) return;   // phone has no NFC: every READ times out with "no card"
        nfc.enableReaderMode(a, this, NfcAdapter.FLAG_READER_NFC_A | NfcAdapter.FLAG_READER_SKIP_NDEF_CHECK, null);
    }

    @Override
    public void onTagDiscovered(Tag t) {
        synchronized (lock) {
            tag = t;
            taps++;
            lock.notifyAll();
        }
    }

    private void serve() {
        try (ServerSocket server = new ServerSocket(0, 4, InetAddress.getByName("127.0.0.1"))) {
            token = Long.toHexString(new SecureRandom().nextLong());
            try (OutputStream out = new FileOutputStream(new File(getApplicationInfo().dataDir, "farex-nfc"))) {
                out.write((server.getLocalPort() + " " + token).getBytes(StandardCharsets.UTF_8));
            }
            while (true) {
                final Socket s = server.accept();
                // own thread: the tap listener's NEXT waits up to 30 s and must not hold up Fetch Load's READ/WRITE
                Thread t = new Thread(new Runnable() { @Override public void run() { handle(s); } }, "farex-nfc-request");
                t.setDaemon(true);
                t.start();
            }
        } catch (IOException e) {
            Log.e(TAG, "card reader bridge did not start", e);
        }
    }

    private void handle(Socket socket) {
        try (Socket s = socket) {
            BufferedReader in = new BufferedReader(new InputStreamReader(s.getInputStream(), StandardCharsets.UTF_8));
            String reply = answer(in.readLine()).replace('\n', ' ') + "\n";
            s.getOutputStream().write(reply.getBytes(StandardCharsets.UTF_8));
        } catch (IOException e) {
            Log.w(TAG, "request failed", e);
        }
    }

    private String answer(String line) {
        String[] w = (line == null) ? new String[0] : line.trim().split(" ");
        if (w.length < 2 || !w[0].equals(token)) return "ERR bad token";
        try {
            if (w[1].equals("NEXT") && w.length == 2) return "OK " + next();
            if (w[1].equals("READ") && w.length == 2) return "OK " + read();
            if (w[1].equals("WRITE") && w.length == 4) {
                Tag t;
                synchronized (lock) { t = tag; }
                // the card read a moment ago must still be on the phone
                if (t == null || !ClassicNfc.uid(t).equalsIgnoreCase(w[2])) return "ERR card moved";
                ClassicNfc.write(t, ClassicNfc.unhex(w[3]));
                return "OK";
            }
            return "ERR unknown request";
        } catch (Exception e) {
            return "ERR " + e.getMessage();
        }
    }

    // The UID of the next card tapped. UID only: it works on every phone, even one that cannot read MIFARE Classic.
    private String next() throws Exception {
        long end = System.currentTimeMillis() + WAIT_MS;
        synchronized (lock) {
            long seen = taps;
            while (taps == seen) {
                long left = end - System.currentTimeMillis();
                if (left <= 0) throw new IOException("no card");
                lock.wait(left);
            }
            return ClassicNfc.uid(tag);
        }
    }

    // Uses the card already on the phone, else waits for one
    private String read() throws Exception {
        long end = System.currentTimeMillis() + WAIT_MS;
        while (true) {
            Tag t;
            synchronized (lock) {
                while (tag == null) {
                    long left = end - System.currentTimeMillis();
                    if (left <= 0) throw new IOException("no card");
                    lock.wait(left);
                }
                t = tag;
            }
            try {
                return ClassicNfc.uid(t) + " " + ClassicNfc.hex(ClassicNfc.read(t));
            } catch (IOException gone) {   // that card left the phone: wait for the next one
                synchronized (lock) { if (tag == t) tag = null; }
            }
        }
    }

    @Override public void onActivityStarted(Activity a) {}
    @Override public void onActivityResumed(Activity a) {}
    @Override public void onActivityPaused(Activity a) {}
    @Override public void onActivityStopped(Activity a) {}
    @Override public void onActivitySaveInstanceState(Activity a, Bundle b) {}
    @Override public void onActivityDestroyed(Activity a) {}
}
