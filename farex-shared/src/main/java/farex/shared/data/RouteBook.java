package farex.shared.data;

import java.nio.file.*;
import java.util.*;

/** All jeepney routes, each an ordered list of stops. Rows: routeId,seq,name,lat,lng. First run seeds sample routes. */
public final class RouteBook {
    private static final String H = "routeId,seq,name,lat,lng";
    private final Map<String, List<RouteStop>> routes = new LinkedHashMap<>();

    public RouteBook(Path dir) {
        Path file = dir.resolve("routes.csv");
        if (!Files.exists(file)) seed(file);
        Map<String, TreeMap<Integer, RouteStop>> tmp = new LinkedHashMap<>();
        for (String[] r : Csv.read(file)) {
            tmp.computeIfAbsent(r[0], k -> new TreeMap<>())
               .put(Integer.parseInt(r[1]), new RouteStop(r[2], Double.parseDouble(r[3]), Double.parseDouble(r[4])));
        }
        tmp.forEach((id, m) -> routes.put(id, List.copyOf(m.values())));
    }

    public Map<String, List<RouteStop>> routes() { return routes; }

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
        for (int i = 0; i < stops.size(); i++) {
            String[] s = stops.get(i);
            Csv.append(file, H, id, String.valueOf(i), s[0].trim(), s[1].trim(), s[2].trim());
        }
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
            {"R1", "Philcoa", "14.6537", "121.0594"}, {"R1", "Tandang Sora", "14.6700", "121.0540"},
            {"R1", "Ever Gotesco", "14.6810", "121.0480"}, {"R1", "Batasan", "14.6850", "121.0870"},
            {"R1", "Doña Carmen", "14.6930", "121.0800"}, {"R1", "Fairview Terminal", "14.7300", "121.0560"},
            {"R2", "Cubao", "14.6191", "121.0509"}, {"R2", "Kamuning", "14.6290", "121.0420"}, {"R2", "Philcoa", "14.6537", "121.0594"},
            {"R3", "SM North", "14.6565", "121.0300"}, {"R3", "Tandang Sora", "14.6700", "121.0540"}, {"R3", "Fairview Terminal", "14.7300", "121.0560"}};
        Map<String, Integer> seq = new HashMap<>();
        for (String[] s : r) {
            int i = seq.merge(s[0], 1, Integer::sum) - 1;
            Csv.append(file, H, s[0], String.valueOf(i), s[1], s[2], s[3]);
        }
    }
}
