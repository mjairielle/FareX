package com.farex.android;

import android.nfc.Tag;
import android.nfc.tech.MifareClassic;

/**
 * UNTESTED. Android side of the card: payload lives in sector 1, block 4.
 * Hand it the Tag from the activity's NFC intent (foreground dispatch).
 * shortcut: factory default key A, the card sector trailer is never written. Switch to our own keys before real cards.
 */
public final class ClassicNfc {
    private static final int SECTOR = 1;
    private static final int BLOCK = 4;

    private ClassicNfc() {}

    public static String uid(Tag tag) {
        StringBuilder b = new StringBuilder();
        for (byte x : tag.getId()) b.append(String.format("%02X", x));
        return b.toString();
    }

    public static byte[] read(Tag tag) throws Exception {
        try (MifareClassic c = MifareClassic.get(tag)) {
            c.connect();
            if (!c.authenticateSectorWithKeyA(SECTOR, MifareClassic.KEY_DEFAULT)) throw new IllegalStateException("Card refused key");
            return c.readBlock(BLOCK);
        }
    }

    public static void write(Tag tag, byte[] payload16) throws Exception {
        if (payload16.length != 16) throw new IllegalArgumentException("payload must be 16 bytes");
        try (MifareClassic c = MifareClassic.get(tag)) {
            c.connect();
            if (!c.authenticateSectorWithKeyA(SECTOR, MifareClassic.KEY_DEFAULT)) throw new IllegalStateException("Card refused key");
            c.writeBlock(BLOCK, payload16);
        }
    }
}
