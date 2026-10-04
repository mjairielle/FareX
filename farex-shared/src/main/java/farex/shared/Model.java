package farex.shared;

import farex.shared.Enums.*;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;

/**
 * Every data row in one file. One record = one CSV file row.
 * Money is always long centavos. Never double.
 * Each record gets toRow() and fromRow() when you implement Csv.
 */
public final class Model {
    private Model() {}

    /** One class for passenger, operator, admin. Roles are a set, so one account can be passenger and operator. */
    public record User(String id, String name, String email, String phone, String passwordHash,
                       Set<Role> roles, PassengerType passengerType, AccountStatus status,
                       String operatorPinHash, String payoutCardUid) {}

    /** Server copy of a card. The chip holds the true balance, this lags. counter blocks replaying an old signed balance. */
    public record Card(String uid, String ownerId, long balance, long counter, CardStatus status, Instant syncedAt) {}

    /** The ledger row. Append only. uuid is made on creation, server drops duplicates. */
    public record Txn(String uuid, TxnType type, String accountId, String operatorId, String cardUid,
                      long amount, long prevBalance, long newBalance, double distanceKm,
                      String method, String status, String refNo, Instant at) {}

    /** Money waiting to be written to a card. Covers paid loads, redeemed rewards (30 days), operator payouts. */
    public record Credit(String id, String accountId, String cardUid, long amount, TxnType source,
                         Instant expiresAt, boolean claimed) {}

    /** A PayMongo QR Ph load. Expiry is checked on read (expiresAt < now), no scheduler. */
    public record Load(String id, String accountId, String cardUid, long amount, PaymentMethod method,
                       LoadStatus status, String intentId, Instant expiresAt) {}

    public record Stop(String id, String name, double lat, double lng) {}
    public record Route(String id, String name, List<String> stopIds) {}
    public record Vehicle(String plateNo, String routeId, String operatorId, boolean retired) {}
    public record FareTable(long baseFare, long ratePerKm, LocalDate effectiveDate) {}
    public record Promotion(String id, double multiplier, LocalDate start, LocalDate end, String routeId) {}
    public record PointsRow(String uuid, String accountId, double delta, String reason, Instant at) {}

    public record TripOption(List<String> legRouteIds, double distanceKm, long fare, int minutes, int transfers) {}

    /** Server answer for a card write. Phone writes balance, counter, signature to the chip. */
    public record SignedBalance(String cardUid, long balance, long counter, String signature) {}
}
