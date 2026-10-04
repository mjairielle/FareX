package farex.shared;

import java.security.PrivateKey;
import java.security.PublicKey;

/**
 * Ed25519 from the JDK, no library.
 * Server holds the private key and signs. Operator phone holds only the public key and verifies offline.
 * Signed message = cardUid + balance + counter. The counter stops someone restoring an old higher balance.
 */
public final class Signer {
    private Signer() {}

    public static String sign(PrivateKey key, String cardUid, long balance, long counter) {
        // TODO: java.security.Signature.getInstance("Ed25519"), return Base64
        throw new UnsupportedOperationException("TODO");
    }

    public static boolean verify(PublicKey key, String cardUid, long balance, long counter, String signature) {
        // TODO: same algorithm, return false on any exception
        throw new UnsupportedOperationException("TODO");
    }
}
