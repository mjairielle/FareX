package farex.shared.fare;

import farex.shared.data.Csv;
import farex.shared.data.RouteStop;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;

/**
 * All fare and distance math for the MVP. Money is long centavos.
 *
 * Defaults are the LTFRB traditional jeepney scale effective 2026-09-28:
 * P14.00 covers the first 4 km, then P2.00 per km after that.
 */
public final class Fares {
    public static volatile long BASE = 1400;          // flat fare for the first BASE_KM
    public static volatile double BASE_KM = 4.0;      // distance BASE already covers
    public static volatile long RATE_PER_KM = 200;    // charged only for km beyond BASE_KM
    public static final double DEFAULT_BASE_KM = 4.0;
    /** baseKm is last so rows written before it existed (4 columns) still read. */
    public static final String HEADER = "effective,base,ratePerKm,published,baseKm";

    private Fares() {}

    /**
     * Applies the fare table row with the latest effective date that is not in the future.
     * Rows: effective(yyyy-mm-dd),base,ratePerKm,published[,baseKm]. A row without baseKm uses
     * DEFAULT_BASE_KM. No file or no row due means the built in defaults stay.
     */
    public static void load(Path dir) {
        LocalDate today = LocalDate.now(ZoneOffset.UTC);
        LocalDate best = null;
        for (String[] r : Csv.read(dir.resolve("fares.txt"), 3)) {
            try {
                LocalDate eff = LocalDate.parse(r[0]);
                if (!eff.isAfter(today) && (best == null || !eff.isBefore(best))) {
                    long base = Long.parseLong(r[1]);
                    long rate = Long.parseLong(r[2]);
                    double baseKm = r.length > 4 && !r[4].isBlank() ? Double.parseDouble(r[4]) : DEFAULT_BASE_KM;
                    best = eff;
                    BASE = base;
                    RATE_PER_KM = rate;
                    BASE_KM = baseKm;
                }
            } catch (RuntimeException badRow) {                // one bad row must not take every request down
                System.err.println("Skipped bad fares.txt row: " + String.join(",", r));
            }
        }
    }

    /** Admin publishes a new table, keeping the current base distance. Append only, so the history stays. */
    public static void publish(Path dir, LocalDate effective, long base, long rate) {
        publish(dir, effective, base, rate, BASE_KM);
    }

    /** Admin publishes a new table. baseKm is how far the base fare goes (4 km jeepney, 5 km bus). */
    public static void publish(Path dir, LocalDate effective, long base, long rate, double baseKm) {
        if (base < 500 || base > 10000) throw new IllegalArgumentException("Base fare must be P5.00 to P100.00");
        if (rate < 50 || rate > 2000) throw new IllegalArgumentException("Rate per km must be P0.50 to P20.00");
        if (!(baseKm >= 0 && baseKm <= 20)) throw new IllegalArgumentException("Base distance must be 0 to 20 km");
        Csv.append(dir.resolve("fares.txt"), HEADER, effective.toString(), String.valueOf(base), String.valueOf(rate),
                java.time.Instant.now().toString(), String.valueOf(baseKm));
        load(dir);
    }

    /**
     * Admin deletes one fare table row (effective date + published time pick it). The fare in use is
     * then chosen again from the rows left, or the built-in P14.00 / 4 km / P2.00 when none is due.
     */
    public static void delete(Path dir, String effective, String published) {
        Path file = dir.resolve("fares.txt");
        List<String[]> rows = new java.util.ArrayList<>(Csv.read(file));
        if (!rows.removeIf(r -> r[0].equals(effective) && r.length > 3 && r[3].equals(published)))
            throw new IllegalArgumentException("No such fare table");
        Csv.rewrite(file, HEADER, rows);
        BASE = 1400; BASE_KM = DEFAULT_BASE_KM; RATE_PER_KM = 200;
        load(dir);
    }

    /**
     * Fare for one boarding, the way the LTFRB fare guides compute it:
     * base covers the first BASE_KM, RATE_PER_KM applies only past it, and the
     * total is rounded to the nearest 25 centavos (half up).
     *
     * A discount applies to the scale, not the rounded total: base and rate are
     * each discounted to the centavo first. That reproduces the printed student
     * column, e.g. P11.25 minimum (11.20 rounded), P20.75 at 10 km.
     */
    public static long fare(double km, FareRule rule) {
        long base = rule.apply(BASE);
        long rate = rule.apply(RATE_PER_KM);
        double extraKm = Math.max(0.0, km - BASE_KM);
        return roundToQuarter(base + rate * extraKm);
    }

    /** Nearest 25 centavos, half up. The epsilon keeps x.5 boundaries from flooring on float error. */
    public static long roundToQuarter(double cents) {
        return (long) Math.floor(cents / 25.0 + 0.5 + 1e-9) * 25;
    }

    /**
     * Road km between consecutive stops, keyed by RoadDistances.key(from, to). Set once at startup
     * from route_km.txt (server) or /road-km (app). Empty means straight-line distances everywhere.
     */
    private static volatile java.util.Map<String, Double> ROAD_KM = java.util.Map.of();

    public static void setRoadKm(java.util.Map<String, Double> roadKm) { ROAD_KM = java.util.Map.copyOf(roadKm); }
    public static java.util.Map<String, Double> roadKm() { return ROAD_KM; }

    /**
     * Distance riding from stop a to stop b (a <= b) in stop order: the road distance for each
     * stretch between stops when it is known (route_km.txt), else the straight line between them.
     * A stretch known only in the other direction uses that; one-way streets make the two differ.
     */
    public static double pathKm(List<RouteStop> stops, int a, int b) {
        double km = 0;
        for (int i = a; i < b; i++) km += stretchKm(stops.get(i), stops.get(i + 1));
        return km;
    }

    private static double stretchKm(RouteStop from, RouteStop to) {
        java.util.Map<String, Double> road = ROAD_KM;
        Double km = road.get(farex.shared.data.RoadDistances.key(from, to));
        if (km == null) km = road.get(farex.shared.data.RoadDistances.key(to, from));
        return km != null ? km : haversineKm(from, to);
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