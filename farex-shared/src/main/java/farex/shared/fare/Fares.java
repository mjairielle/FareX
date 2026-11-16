package farex.shared.fare;

import farex.shared.data.Csv;
import farex.shared.data.RouteStop;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;

/** All fare and distance math for the MVP. Money is long centavos. */
public final class Fares {
    public static volatile long BASE = 1300;
    public static volatile long RATE_PER_KM = 200;
    public static final String HEADER = "effective,base,ratePerKm,published";

    private Fares() {}

    /**
     * Applies the fare table row with the latest effective date that is not in the future.
     * Rows: effective(yyyy-mm-dd),base,ratePerKm,published. No file or no row due means the built in defaults stay.
     */
    public static void load(Path dir) {
        LocalDate today = LocalDate.now(ZoneOffset.UTC);
        LocalDate best = null;
        for (String[] r : Csv.read(dir.resolve("fares.csv"))) {
            LocalDate eff = LocalDate.parse(r[0]);
            if (!eff.isAfter(today) && (best == null || !eff.isBefore(best))) {
                best = eff;
                BASE = Long.parseLong(r[1]);
                RATE_PER_KM = Long.parseLong(r[2]);
            }
        }
    }

    /** Admin publishes a new table. Append only, so the history stays. */
    public static void publish(Path dir, LocalDate effective, long base, long rate) {
        if (base < 500 || base > 10000) throw new IllegalArgumentException("Base fare must be P5.00 to P100.00");
        if (rate < 50 || rate > 2000) throw new IllegalArgumentException("Rate per km must be P0.50 to P20.00");
        Csv.append(dir.resolve("fares.csv"), HEADER, effective.toString(), String.valueOf(base), String.valueOf(rate), java.time.Instant.now().toString());
        load(dir);
    }

    public static long fare(double km, FareRule rule) {
        return rule.apply(Math.round(BASE + RATE_PER_KM * km));
    }

    /** Distance along stop order from index a to index b. Not straight line. */
    public static double pathKm(List<RouteStop> stops, int a, int b) {
        double km = 0;
        for (int i = a; i < b; i++) km += haversineKm(stops.get(i), stops.get(i + 1));
        return km;
    }

    public static double haversineKm(RouteStop p, RouteStop q) {
        return haversineKm(p.lat(), p.lng(), q.lat(), q.lng());
    }

    public static double haversineKm(double lat1, double lng1, double lat2, double lng2) {
        double r = 6371.0;
        double dLat = Math.toRadians(lat2 - lat1);
        double dLng = Math.toRadians(lng2 - lng1);
        double h = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(dLng / 2) * Math.sin(dLng / 2);
        return 2 * r * Math.asin(Math.sqrt(h));
    }
}