package farex.shared.card;

import java.nio.ByteBuffer;
import java.security.MessageDigest;
import java.util.Arrays;
import java.util.Optional;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

/**
 * The 16 bytes kept on a card: balance (4) + counter (4) + MAC (8).
 * Same bytes fit Classic block 4 now and Ultralight pages 4 to 7 later.
 * shortcut: MAC is HMAC-SHA256 cut to 8 bytes, one shared secret, upgrade if the key can leak.
 */
public final class CardPayload {
    public static final int SIZE = 16;

    public record Data(long cents, int counter) {}

    private CardPayload() {}

    public static byte[] seal(String uid, long cents, int counter, byte[] key) {
        if (cents < 0 || cents > Integer.MAX_VALUE) throw new IllegalArgumentException("Balance does not fit on card");
        ByteBuffer b = ByteBuffer.allocate(SIZE).putInt((int) cents).putInt(counter);
        b.put(mac(uid, b.array(), key));
        return b.array();
    }

    /** Empty means wrong size, wrong card, or edited bytes. */
    public static Optional<Data> open(String uid, byte[] payload, byte[] key) {
        if (payload == null || payload.length != SIZE) return Optional.empty();
        byte[] head = Arrays.copyOf(payload, 8);
        if (!MessageDigest.isEqual(mac(uid, head, key), Arrays.copyOfRange(payload, 8, SIZE))) return Optional.empty();
        ByteBuffer b = ByteBuffer.wrap(head);
        return Optional.of(new Data(b.getInt(), b.getInt()));
    }

    // uid is in the MAC so a valid payload copied to another card fails
    private static byte[] mac(String uid, byte[] head8, byte[] key) {
        try {
            Mac m = Mac.getInstance("HmacSHA256");
            m.init(new SecretKeySpec(key, "HmacSHA256"));
            m.update(uid.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            m.update(head8, 0, 8);
            return Arrays.copyOf(m.doFinal(), 8);
        } catch (java.security.GeneralSecurityException e) {
            throw new IllegalStateException(e);
        }
    }
}
