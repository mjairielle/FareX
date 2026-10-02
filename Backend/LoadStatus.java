package Backend;

public enum LoadStatus {
    CREATED,            // record exists, PayMongo not asked yet
    AWAITING_PAYMENT,   // QR generated and live
    PAID,
    EXPIRED,
    CANCELLED,
    FAILED
}