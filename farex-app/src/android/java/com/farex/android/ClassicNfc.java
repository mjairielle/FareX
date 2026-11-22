package com.farex.android;

import android.nfc.Tag;
import android.nfc.tech.MifareClassic;

/**
 * Android side of the card: payload lives in sector 1, block 2 of that sector (block 6 of the card).
 * NfcBridge hands it the Tag from reader mode.
 * shortcut: factory default key A, the card sector trailer is never written. Switch to our own keys before real cards.
 */
public final class ClassicNfc {
    private static final int SECTOR = 1;
    // sector 1 = blocks 4..7 on the whole card; the payload is the sector's block 2, so block 6 (4 is the sector's 0)
    private static final int BLOCK = 6;

    private ClassicNfc() {}

    public static String uid(Tag tag) {
        return hex(tag.getId());
    }

    // synchronized: one card connection at a time (READ and WRITE can come from two request threads)
    public static synchronized byte[] read(Tag tag) throws Exception {
        try (MifareClassic c = open(tag)) {
            return c.readBlock(BLOCK);
        }
    }

    public static synchronized void write(Tag tag, byte[] payload16) throws Exception {
        if (payload16.length != 16) throw new IllegalArgumentException("payload must be 16 bytes");
        try (MifareClassic c = open(tag)) {
            c.writeBlock(BLOCK, payload16);
        }
    }

    private static MifareClassic open(Tag tag) throws Exception {
        MifareClassic c = MifareClassic.get(tag);
        // null: not a Classic card, or the phone's NFC chip cannot read Classic (many non-NXP phones)
        if (c == null) throw new IllegalStateException("This phone or card does not support MIFARE Classic");
        c.connect();
        if (!c.authenticateSectorWithKeyA(SECTOR, MifareClassic.KEY_DEFAULT)) {
            c.close();
            throw new IllegalStateException("Card refused key");
        }
        return c;
    }

    // java.util.HexFormat only exists on Android 14+
    static String hex(byte[] bytes) {
        StringBuilder b = new StringBuilder();
        for (byte x : bytes) b.append(String.format("%02X", x));
        return b.toString();
    }

    static byte[] unhex(String s) {
        if (s.length() % 2 != 0) throw new IllegalArgumentException("bad hex");
        byte[] out = new byte[s.length() / 2];
        for (int i = 0; i < out.length; i++) {
            int hi = Character.digit(s.charAt(2 * i), 16), lo = Character.digit(s.charAt(2 * i + 1), 16);
            if (hi < 0 || lo < 0) throw new IllegalArgumentException("bad hex");
            out[i] = (byte) (hi * 16 + lo);
        }
        return out;
    }
}
