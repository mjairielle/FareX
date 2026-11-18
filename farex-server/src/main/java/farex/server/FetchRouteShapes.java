package farex.server;

import farex.shared.data.RoadDistances;
import farex.shared.data.RouteBook;
import farex.shared.data.RouteShapes;
import farex.shared.data.RouteStop;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * One-off tool: asks the public OSRM router for the road between each pair of consecutive stops
 * in routes.csv, and writes next to it
 *   route_shapes.csv  the road path, for drawing routes on the map
 *   route_km.csv      the road distance, which fares are charged on (Fares.pathKm)
 * Run it after adding or changing routes, then commit both files. The server and app never call
 * OSRM themselves.
 *
 *   java -cp build-server farex.server.FetchRouteShapes [dataDir]     (default: data)
 *
 * router.project-osrm.org is a free demo server: one request per second, identify yourself,
 * and do not use it for anything heavy. A segment that fails is left out: drawn straight and
 * charged on straight-line distance.
 */
public final class FetchRouteShapes {
    private static final String OSRM = "https://router.project-osrm.org/route/v1/driving/";
    private static final Pattern PAIR = Pattern.compile("\\[\\s*(-?[0-9.]+)\\s*,\\s*(-?[0-9.]+)\\s*]");
    private static final Pattern DISTANCE = Pattern.compile("\"distance\":([0-9.]+)");
    private static final double SUSPICIOUS_DETOUR = 2.0;   // road km / straight km worth a second look

    /** The road from one stop to the next: its points ([lat, lng]) and length. */
    record Road(List<double[]> path, double km) {}

    public static void main(String[] args) throws Exception {
        Path dir = Path.of(args.length > 0 ? args[0] : "data");
        Map<String, List<RouteStop>> routes = new RouteBook(dir).routes();
        HttpClient http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(15)).build();

        List<String> shapeLines = new ArrayList<>(List.of(RouteShapes.HEADER));
        List<String> kmLines = new ArrayList<>(List.of(RoadDistances.HEADER));
        int ok = 0, failed = 0, suspicious = 0;
        for (var route : routes.entrySet()) {
            List<RouteStop> stops = route.getValue();
            for (int i = 0; i < stops.size() - 1; i++) {
                RouteStop a = stops.get(i), b = stops.get(i + 1);
                Road road = fetch(http, a, b);
                Thread.sleep(1100);                                // OSRM demo: max 1 request per second
                if (road == null) {
                    failed++;
                    System.out.println("  no road " + route.getKey() + " " + a.name() + " -> " + b.name() + " (straight line)");
                    continue;
                }
                ok++;
                for (int k = 0; k < road.path().size(); k++) {
                    double[] p = road.path().get(k);
                    shapeLines.add(String.format(Locale.ROOT, "%s,%d,%d,%.6f,%.6f", route.getKey(), i, k, p[0], p[1]));
                }
                kmLines.add(String.format(Locale.ROOT, "%s,%d,%.3f", route.getKey(), i, road.km()));
                double straight = farex.shared.fare.Fares.haversineKm(a, b);
                System.out.printf(Locale.ROOT, "  %s %s -> %s: %.2f km by road (%.2f km straight), %d points%n",
                        route.getKey(), a.name(), b.name(), road.km(), straight, road.path().size());
                // City roads are rarely more than ~2x the straight line. More usually means a stop
                // sits on the wrong side of a divided road and OSRM added a U-turn: fares go up.
                if (road.km() > SUSPICIOUS_DETOUR * straight) {
                    suspicious++;
                    System.out.printf(Locale.ROOT, "    CHECK: %.1fx the straight line - are %s and %s placed on the road the jeep uses?%n",
                            road.km() / straight, a.name(), b.name());
                }
            }
        }
        Files.write(dir.resolve(RouteShapes.FILE), shapeLines, StandardCharsets.UTF_8);
        Files.write(dir.resolve(RoadDistances.FILE), kmLines, StandardCharsets.UTF_8);
        System.out.println("Wrote " + RouteShapes.FILE + " and " + RoadDistances.FILE + " in " + dir
                + ": " + ok + " segments, " + failed + " without a road, " + suspicious + " to check");
    }

    /** The road from a to b, or null when OSRM has none. */
    static Road fetch(HttpClient http, RouteStop a, RouteStop b) {
        String url = OSRM + String.format(Locale.ROOT, "%.6f,%.6f;%.6f,%.6f", a.lng(), a.lat(), b.lng(), b.lat())
                + "?overview=full&geometries=geojson";
        try {
            HttpRequest req = HttpRequest.newBuilder(URI.create(url))
                    .header("User-Agent", "FareX student project (route shape seeding)")
                    .timeout(Duration.ofSeconds(30)).build();
            String body = http.send(req, HttpResponse.BodyHandlers.ofString()).body();
            if (!body.contains("\"code\":\"Ok\"")) return null;

            Matcher d = DISTANCE.matcher(body);                    // metres; one leg, so the first is the leg's
            if (!d.find()) return null;
            double km = Double.parseDouble(d.group(1)) / 1000.0;

            int start = body.indexOf("\"coordinates\":[");
            if (start < 0) return null;
            int end = body.indexOf("]]", start);
            List<double[]> path = new ArrayList<>();
            Matcher m = PAIR.matcher(body.substring(start, end + 2));
            while (m.find()) path.add(new double[]{Double.parseDouble(m.group(2)), Double.parseDouble(m.group(1))});  // GeoJSON is lng,lat
            return path.isEmpty() || km <= 0 ? null : new Road(path, km);
        } catch (Exception e) {
            return null;
        }
    }
}
