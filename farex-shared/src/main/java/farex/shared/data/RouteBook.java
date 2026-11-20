package farex.shared.data;

import java.nio.file.*;
import java.util.*;

/**
 * All jeepney routes, each an ordered list of stops. Rows: routeId,seq,name,lat,lng. First run seeds sample routes.
 * Optional route_names.csv (routeId,name,oneway) gives a route its display name and marks it one-way:
 * OpenStreetMap maps most jeepney routes once per direction, and each direction is ridden forward only.
 */
public final class RouteBook {
    private static final String H = "routeId,seq,name,lat,lng";
    public static final String NAMES_FILE = "route_names.csv";
    public static final String NAMES_HEADER = "routeId,name,oneway";
    private final Map<String, List<RouteStop>> routes = new LinkedHashMap<>();
    private final Map<String, String> names = new HashMap<>();
    private final Set<String> oneway = new HashSet<>();

    public RouteBook(Path dir) {
        Path file = dir.resolve("routes.csv");
        if (!Files.exists(file)) seed(file);
        Map<String, TreeMap<Integer, RouteStop>> tmp = new LinkedHashMap<>();
        for (String[] r : Csv.read(file, 5)) {
            try {
                tmp.computeIfAbsent(r[0], k -> new TreeMap<>())
                   .put(Integer.parseInt(r[1]), new RouteStop(r[2], Double.parseDouble(r[3]), Double.parseDouble(r[4])));
            } catch (NumberFormatException bad) {           // one damaged row must not stop the server from starting
                System.err.println("Skipped bad routes.csv row: " + String.join(",", r));
            }
        }
        tmp.forEach((id, m) -> routes.put(id, List.copyOf(m.values())));
        for (String[] r : Csv.read(dir.resolve(NAMES_FILE), 2)) {
            if (!routes.containsKey(r[0])) continue;
            names.put(r[0], r[1]);
            if (r.length > 2 && Boolean.parseBoolean(r[2].trim())) oneway.add(r[0]);
        }
    }

    public Map<String, List<RouteStop>> routes() { return routes; }

    /** "Commonwealth–SM Fairview via Regalado (to SM City Fairview)"; the id when no name is known. */
    public String name(String routeId) { return names.getOrDefault(routeId, routeId); }

    /** True when the route is ridden in stop order only (the other direction is its own route). */
    public boolean isOneway(String routeId) { return oneway.contains(routeId); }

    /** Admin adds a new route. Existing routes are never edited, so old trips stay valid. Stops: name, lat, lng. */
    public static void addRoute(Path dir, String id, List<String[]> stops) {
        id = id.trim();
        if (!id.matches("[A-Za-z0-9-]{1,12}")) throw new IllegalArgumentException("Route id: letters, digits, dash, up to 12");
        if (stops.size() < 2) throw new IllegalArgumentException("A route needs at least 2 stops");
        Path file = dir.resolve("routes.csv");
        for (String[] r : Csv.read(file)) if (r[0].equals(id)) throw new IllegalArgumentException("Route " + id + " already exists");
        for (String[] s : stops) {
            if (s.length != 3 || s[0].isBlank()) throw new IllegalArgumentException("Each stop is: name, lat, lng");
            try {
                double lat = Double.parseDouble(s[1].trim()), lng = Double.parseDouble(s[2].trim());
                if (Math.abs(lat) > 90 || Math.abs(lng) > 180) throw new IllegalArgumentException("Coordinates out of range");
            } catch (NumberFormatException e) { throw new IllegalArgumentException("Bad coordinates for " + s[0]); }
        }
        List<String[]> rows = new ArrayList<>();
        for (int i = 0; i < stops.size(); i++) {
            String[] s = stops.get(i);
            rows.add(new String[]{id, String.valueOf(i), s[0].trim(), s[1].trim(), s[2].trim()});
        }
        Csv.appendAll(file, H, rows);                        // one write, so a crash never leaves half a route
    }

    /** Every place a passenger can pick, sorted. */
    public List<String> places() {
        TreeSet<String> out = new TreeSet<>();
        routes.values().forEach(l -> l.forEach(s -> out.add(s.name())));
        return new ArrayList<>(out);
    }

    // Sample coordinates around Quezon City. Edit routes.csv to change. Shared stops must repeat the same name.
    private static void seed(Path file) {
        String[][] r = {
            {"R1", "Philcoa", "14.65400", "121.05377"}, {"R1", "Tandang Sora", "14.66223", "121.06619"},
            {"R1", "Ever Gotesco", "14.67959", "121.08409"}, {"R1", "Batasan", "14.68766", "121.08730"},
            {"R1", "Doña Carmen", "14.70517", "121.07843"}, {"R1", "Fairview Terminal", "14.73306", "121.05840"},
            {"R2", "Cubao", "14.62134", "121.05299"}, {"R2", "Kamuning", "14.62753", "121.03900"}, {"R2", "Philcoa", "14.65400", "121.05377"},
            {"R3", "SM North", "14.65711", "121.03132"}, {"R3", "Tandang Sora", "14.66223", "121.06619"}, {"R3", "Fairview Terminal", "14.73306", "121.05840"}};
        Map<String, Integer> seq = new HashMap<>();
        for (String[] s : r) {
            int i = seq.merge(s[0], 1, Integer::sum) - 1;
            Csv.append(file, H, s[0], String.valueOf(i), s[1], s[2], s[3]);
        }
    }
}
