package farex.shared.data;

import java.nio.file.Path;
import java.util.*;

/**
 * Road distance between consecutive stops, from route_km.txt (routeId,segment,km; segment i is
 * stop i -> i+1). Written by farex.server.FetchRouteShapes from OSRM, committed with the data.
 *
 * Fares are charged on these through Fares.pathKm, which looks them up by the two stops'
 * coordinates. Keying by coordinates, not route, lets TapDesk (which only has a stop list) and
 * every route sharing a stretch of road use the same number.
 */
public final class RoadDistances {
    public static final String FILE = "route_km.txt";
    public static final String HEADER = "routeId,segment,km";

    private RoadDistances() {}

    /** Lookup key for the road from a to b (direction matters: one-way streets). */
    public static String key(RouteStop a, RouteStop b) {
        return key(a.lat(), a.lng(), b.lat(), b.lng());
    }

    public static String key(double aLat, double aLng, double bLat, double bLng) {
        return String.format(Locale.ROOT, "%.5f,%.5f>%.5f,%.5f", aLat, aLng, bLat, bLng);
    }

    /** key(a, b) -> road km for every segment of `routes` listed in dir/route_km.txt. */
    public static Map<String, Double> load(Path dir, Map<String, List<RouteStop>> routes) {
        return load(dir, FILE, routes);
    }

    /** Same from another file of the same format, e.g. stops_km.txt for the operator's stops.txt route. */
    public static Map<String, Double> load(Path dir, String file, Map<String, List<RouteStop>> routes) {
        Map<String, Double> out = new HashMap<>();
        for (String[] r : Csv.read(dir.resolve(file))) {
            try {
                List<RouteStop> stops = routes.get(r[0]);
                int i = Integer.parseInt(r[1]);
                double km = Double.parseDouble(r[2]);
                if (stops == null || i < 0 || i + 1 >= stops.size() || !(km > 0)) continue;
                out.put(key(stops.get(i), stops.get(i + 1)), km);
            } catch (RuntimeException badRow) {
                // skip; that stretch is charged on straight-line distance
            }
        }
        return out;
    }
}
