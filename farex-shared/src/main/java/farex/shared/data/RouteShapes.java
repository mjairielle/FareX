package farex.shared.data;

import java.nio.file.Path;
import java.util.*;

/**
 * The road path a jeepney actually drives between consecutive stops, for drawing routes on a map.
 * Rows of route_shapes.csv: routeId,segment,seq,lat,lng. Segment i runs from stop i to stop i+1
 * of that route (stop order as in routes.csv); seq orders its points.
 *
 * Fetched once with farex.server.FetchRouteShapes and committed, so the app never calls a routing
 * service at run time. A segment with no shape is simply drawn as a straight line.
 */
public final class RouteShapes {
    public static final String FILE = "route_shapes.csv";
    public static final String HEADER = "routeId,segment,seq,lat,lng";

    /** One point of a path. */
    public record Point(double lat, double lng) {}

    // routeId -> segment index -> points in driving order
    private final Map<String, Map<Integer, List<Point>>> shapes = new LinkedHashMap<>();

    public RouteShapes(Path dir) {
        Map<String, Map<Integer, TreeMap<Integer, Point>>> tmp = new LinkedHashMap<>();
        for (String[] r : Csv.read(dir.resolve(FILE))) {
            try {
                tmp.computeIfAbsent(r[0], k -> new TreeMap<>())
                   .computeIfAbsent(Integer.parseInt(r[1]), k -> new TreeMap<>())
                   .put(Integer.parseInt(r[2]), new Point(Double.parseDouble(r[3]), Double.parseDouble(r[4])));
            } catch (RuntimeException badRow) {
                // skip a damaged row; that segment falls back to a straight line
            }
        }
        tmp.forEach((route, segs) -> {
            Map<Integer, List<Point>> m = new TreeMap<>();
            segs.forEach((seg, pts) -> m.put(seg, List.copyOf(pts.values())));
            shapes.put(route, m);
        });
    }

    /** routeId -> segment -> points. Routes with no shape file rows are absent. */
    public Map<String, Map<Integer, List<Point>>> all() { return shapes; }

    /** Points of one segment (stop i to stop i+1), or empty when it has no shape. */
    public List<Point> segment(String routeId, int i) {
        return shapes.getOrDefault(routeId, Map.of()).getOrDefault(i, List.of());
    }
}
