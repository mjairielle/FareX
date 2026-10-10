package farex.shared.reward;

import farex.shared.data.Csv;
import java.nio.file.*;
import java.time.Instant;
import java.util.*;

/**
 * Points ledger. Append only, balance is the sum. Earn is keyed by trip id so a retry never double counts.
 * Files: rewards.csv (id,time,accountId,type,points,ref) and promotions.csv (id,name,multiplier,start,end).
 * shortcut: spend and the matching card credit are two writes, a crash between them loses nothing for the
 * passenger but can leave points undeducted. Fine on one server thread, wrap in a transaction on a real DB.
 */
public final class Rewards {
    public static final double POINTS_PER_KM = 1.0;
    public record Perk(String id, String name, long points, long cents) {}
    public record Promo(String id, String name, double multiplier, Instant start, Instant end) {}

    public static final List<Perk> PERKS = List.of(
            new Perk("P10", "P10 fare credit", 100, 1000),
            new Perk("P20", "P20 fare credit", 200, 2000),
            new Perk("P50", "P50 fare credit", 500, 5000));

    private static final String RH = "id,time,accountId,type,points,ref";
    private static final String PH = "id,name,multiplier,start,end";
    private final Path rewards, promos;

    public Rewards(Path dir) {
        rewards = dir.resolve("rewards.csv");
        promos = dir.resolve("promotions.csv");
    }

    public long balance(String accountId) {
        long sum = 0;
        for (String[] r : Csv.read(rewards)) if (r[2].equals(accountId)) sum += Long.parseLong(r[4]);
        return sum;
    }

    /** Biggest multiplier among promotions running right now, 1.0 when none. */
    public double multiplier(Instant now) {
        double best = 1.0;
        for (Promo p : promotions()) if (!now.isBefore(p.start()) && !now.isAfter(p.end())) best = Math.max(best, p.multiplier());
        return best;
    }

    /** Points for one finished trip. Same tripId twice earns once. Returns points added, 0 when repeated or too short. */
    public long earn(String accountId, String tripId, double km, Instant now) {
        for (String[] r : Csv.read(rewards)) if (r[5].equals(tripId) && r[3].equals("EARN")) return 0;
        long pts = (long) Math.floor(km * POINTS_PER_KM * multiplier(now));
        if (pts > 0) Csv.append(rewards, RH, UUID.randomUUID().toString(), now.toString(), accountId, "EARN", String.valueOf(pts), tripId);
        return pts;
    }

    public Perk perk(String id) {
        return PERKS.stream().filter(p -> p.id().equals(id)).findFirst().orElseThrow(() -> new IllegalArgumentException("Unknown perk"));
    }

    /** Throws when points are short. Call before crediting the card. */
    public void check(String accountId, Perk p) {
        if (balance(accountId) < p.points()) throw new IllegalStateException("Not enough points, need " + p.points());
    }

    /** Deduct after the card credit succeeded. */
    public void spend(String accountId, Perk p, Instant now) {
        Csv.append(rewards, RH, UUID.randomUUID().toString(), now.toString(), accountId, "REDEEM", String.valueOf(-p.points()), p.id());
    }

    public void addPromotion(String name, double multiplier, Instant start, Instant end) {
        if (multiplier < 1 || multiplier > 10) throw new IllegalArgumentException("Multiplier must be 1 to 10");
        if (end.isBefore(start)) throw new IllegalArgumentException("End is before start");
        Csv.append(promos, PH, UUID.randomUUID().toString(), name, String.valueOf(multiplier), start.toString(), end.toString());
    }

    public List<Promo> promotions() {
        List<Promo> out = new ArrayList<>();
        for (String[] r : Csv.read(promos)) out.add(new Promo(r[0], r[1], Double.parseDouble(r[2]), Instant.parse(r[3]), Instant.parse(r[4])));
        return out;
    }
}
