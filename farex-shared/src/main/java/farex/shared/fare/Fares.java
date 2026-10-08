package farex.shared.fare;

import farex.shared.data.RouteStop;
import java.util.List;

/** All fare and distance math for the MVP. Money is long centavos. */
public final class Fares {
    public static final long BASE = 1300;
    public static final long RATE_PER_KM = 150;

    private Fares() {}

    public static long fare(double km, FareRule rule) {
        return rule.apply(Math.round(BASE + RATE_PER_KM * km));
    }

    /** Distance along stop order from index a to index b. Not straight line. */
    public static double pathKm(List<RouteStop> stops, int a, int b) {
        double km = 0;
        for (int i = a; i < b; i++) km += haversineKm(stops.get(i), stops.get(i + 1));
        return km;
    }

    static double haversineKm(RouteStop p, RouteStop q) {
        double r = 6371.0;
        double dLat = Math.toRadians(q.lat() - p.lat());
        double dLng = Math.toRadians(q.lng() - p.lng());
        double h = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(p.lat())) * Math.cos(Math.toRadians(q.lat()))
                * Math.sin(dLng / 2) * Math.sin(dLng / 2);
        return 2 * r * Math.asin(Math.sqrt(h));
    }
}
