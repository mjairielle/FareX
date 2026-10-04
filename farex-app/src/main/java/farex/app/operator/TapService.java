package farex.app.operator;

import farex.shared.Model.*;
import java.time.Instant;

/**
 * Works offline. Verifies the card signature with the public key, computes fare with shared FareCalculator,
 * writes rows to the local CSV. Replaces TapDevice and TapSession.
 */
public class TapService {
    public record Session(String passengerRef, Stop tapInStop, Instant inTime, double gpsLat, double gpsLng) {}

    /** passengerRef is a card uid (NFC) or an app user id (QR). Reject if signature is bad or card is not usable. */
    public Session tapIn(String passengerRef, Stop here) { throw new UnsupportedOperationException("TODO"); }

    /** Distance, fare, deduct from chip (signed write) or server balance, append FARE_PAYMENT row to local CSV. */
    public Txn tapOut(Session s, Stop here) { throw new UnsupportedOperationException("TODO"); }

    /** No tap out before route end: charge FareCalculator.maxFare so operator is never shorted. */
    public Txn chargeMaxFare(Session s, Route route) { throw new UnsupportedOperationException("TODO"); }
}
