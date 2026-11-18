package farex.server;

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
 * One-off tool: asks the public OSRM router for the road path between each pair of consecutive
 * stops in routes.csv and writes route_shapes.csv next to it. Run it after adding or changing
 * routes, then commit the file. The server and app never call OSRM themselves.
 *
 *   java -cp build-server farex.server.FetchRouteShapes [dataDir]     (default: data)
 *
 * router.project-osrm.org is a free demo server: one request per second, identify yourself,
 * and do not use it for anything heavy. A segment that fails is left out and drawn straight.
 */
public final class FetchRouteShapes {
    private static final String OSRM = "https://router.project-osrm.org/route/v1/driving/";
    private static final Pattern PAIR = Pattern.compile("\\[\\s*(-?[0-9.]+)\\s*,\\s*(-?[0-9.]+)\\s*]");

    public static void main(String[] args) throws Exception {
        Path dir = Path.of(args.length > 0 ? args[0] : "data");
        Map<String, List<RouteStop>> routes = new RouteBook(dir).routes();
        HttpClient http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(15)).build();

        List<String> lines = new ArrayList<>(List.of(RouteShapes.HEADER));
        int ok = 0, failed = 0;
        for (var route : routes.entrySet()) {
            List<RouteStop> stops = route.getValue();
            for (int i = 0; i < stops.size() - 1; i++) {
                RouteStop a = stops.get(i), b = stops.get(i + 1);
                List<double[]> path = fetch(http, a, b);
                Thread.sleep(1100);                                // OSRM demo: max 1 request per second
                if (path.isEmpty()) {
                    failed++;
                    System.out.println("  no path " + route.getKey() + " " + a.name() + " -> " + b.name() + " (straight line)");
                    continue;
                }
                ok++;
                for (int k = 0; k < path.size(); k++) {
                    lines.add(String.format(Locale.ROOT, "%s,%d,%d,%.6f,%.6f", route.getKey(), i, k, path.get(k)[0], path.get(k)[1]));
                }
                System.out.printf(Locale.ROOT, "  %s %s -> %s: %d points%n", route.getKey(), a.name(), b.name(), path.size());
            }
        }
        Files.write(dir.resolve(RouteShapes.FILE), lines, StandardCharsets.UTF_8);
        System.out.println("Wrote " + dir.resolve(RouteShapes.FILE) + ": " + ok + " segments, " + failed + " without a road path");
    }

    /** Road path from a to b as [lat, lng] points, or empty when OSRM has none. */
    static List<double[]> fetch(HttpClient http, RouteStop a, RouteStop b) {
        String url = OSRM + String.format(Locale.ROOT, "%.6f,%.6f;%.6f,%.6f", a.lng(), a.lat(), b.lng(), b.lat())
                + "?overview=full&geometries=geojson";
        List<double[]> out = new ArrayList<>();
        try {
            HttpRequest req = HttpRequest.newBuilder(URI.create(url))
                    .header("User-Agent", "FareX student project (route shape seeding)")
                    .timeout(Duration.ofSeconds(30)).build();
            String body = http.send(req, HttpResponse.BodyHandlers.ofString()).body();
            if (!body.contains("\"code\":\"Ok\"")) return out;
            int start = body.indexOf("\"coordinates\":[");
            if (start < 0) return out;
            int end = body.indexOf("]]", start);
            Matcher m = PAIR.matcher(body.substring(start, end + 2));
            while (m.find()) out.add(new double[]{Double.parseDouble(m.group(2)), Double.parseDouble(m.group(1))});  // GeoJSON is lng,lat
        } catch (Exception e) {
            out.clear();
        }
        return out;
    }
}
