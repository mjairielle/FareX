package farex.server;

import farex.shared.data.RoadDistances;
import farex.shared.data.RouteBook;
import farex.shared.data.RouteShapes;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.*;
import javax.xml.parsers.DocumentBuilderFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

/**
 * One-off tool: imports every jeepney route in Quezon City from OpenStreetMap and writes the
 * FareX route data, replacing the sample routes.
 *
 *   java -cp build-server farex.server.ImportOsmRoutes [dataDir] [cacheDir]   (defaults: data, osm-cache)
 *
 * Reads (fetched from the Overpass API once, then reused from cacheDir):
 *   jeepney_routes.osm  route relations tagged bus=share_taxi (how OSM PH maps jeepneys), with geometry
 *   qc_roads.osm        named QC roads, only to name stops after their intersection
 * Writes into dataDir:
 *   routes.csv          routeId,seq,name,lat,lng      (stops in riding order)
 *   route_names.csv     routeId,name,oneway           (OSM name; oneway when OSM maps each direction)
 *   route_shapes.csv    the street path between consecutive stops, for the map
 *   route_km.csv        the length of that path, which fares are charged on
 *
 * Stops: OSM rarely maps jeepney stops (jeeps stop where flagged), so they are placed at the
 * mapped platforms, both ends, wherever another route joins, leaves or crosses this one (so
 * transfers line up), and then every 500 m at most. Stops of different routes within 75 m
 * (e.g. both sides of Commonwealth Avenue) become one shared stop, so the planner can transfer there.
 *
 * Map data (c) OpenStreetMap contributors, ODbL. Overpass: please do not run this in a loop.
 */
public final class ImportOsmRoutes {
    private static final String[] OVERPASS = {
            "https://overpass-api.de/api/interpreter", "https://overpass.kumi.systems/api/interpreter"};
    private static final String AREA = "area[\"name\"=\"Quezon City\"][\"boundary\"=\"administrative\"]->.qc;";
    // Relations with their geometry, then their stop/platform nodes (for the stop names)
    private static final String ROUTES_QUERY = "[out:xml][timeout:300];" + AREA
            + "(relation[\"type\"=\"route\"][\"route\"=\"bus\"][\"bus\"=\"share_taxi\"](area.qc);"
            + "relation[\"type\"=\"route\"][\"route\"=\"share_taxi\"][\"network\"~\"LTFRB\"](area.qc);)->.r;"
            + ".r out geom;node(r.r);out;";
    // Named roads in the box around all the routes (they run past QC into Manila, Caloocan, Marikina...).
    // A bounding box, because "around the routes" is too slow for the public Overpass servers.
    private static final String ROADS_QUERY = "[out:xml][timeout:600][bbox:%s];"
            + "way[\"highway\"~\"^(trunk|primary|secondary|tertiary|unclassified|residential|living_street)$\"]"
            + "[\"name\"];out geom;";

    static final double SAMPLE_M = 20;          // path resolution for finding where routes meet
    static final double NEAR_OTHER_M = 30;      // another route this close = same road / crossing
    static final double MAX_GAP_M = 500;        // longest stretch without a stop
    static final double MIN_GAP_M = 150;        // stops of one route closer than this are merged
    static final double SHARED_STOP_M = 75;     // stops of different routes this close become one (both sides of a wide avenue)
    static final double PLATFORM_SNAP_M = 60;   // a mapped platform further than this from the path is ignored
    static final double LANDMARK_M = 150;       // a FareX landmark name is kept for the nearest stop within this

    // Landmarks the app already uses; the nearest imported stop keeps the familiar name.
    static final Map<String, P> LANDMARKS = new LinkedHashMap<>();
    static {
        LANDMARKS.put("Philcoa", new P(14.65400, 121.05377));
        LANDMARKS.put("Tandang Sora", new P(14.66223, 121.06619));
        LANDMARKS.put("Ever Gotesco", new P(14.67959, 121.08409));
        LANDMARKS.put("Batasan", new P(14.68766, 121.08730));
        LANDMARKS.put("Doña Carmen", new P(14.70517, 121.07843));
        LANDMARKS.put("Fairview Terminal", new P(14.73306, 121.05840));
        LANDMARKS.put("Cubao", new P(14.62134, 121.05299));
        LANDMARKS.put("Kamuning", new P(14.62753, 121.03900));
        LANDMARKS.put("SM North", new P(14.65711, 121.03132));
    }

    record P(double lat, double lng) {}
    record Platform(P at, String name) {}
    static final class OsmRoute {
        String relId, ref, name, from, to, id;
        boolean oneway;
        List<P> path = new ArrayList<>();
        double[] cum;                               // km along path at each point
        List<Platform> platforms = new ArrayList<>();
        List<Stop> stops = new ArrayList<>();
    }
    /** A stop on one route: where along it, why it is there, and the shared place it became. */
    static final class Stop {
        double km; int priority; String name; Place place;
        Stop(double km, int priority, String name) { this.km = km; this.priority = priority; this.name = name; }
    }
    static final class Place { P at; String name; final List<String> platformNames = new ArrayList<>(); Place(P at) { this.at = at; } }

    public static void main(String[] args) throws Exception {
        Path dir = Path.of(args.length > 0 ? args[0] : "data");
        Path cache = Path.of(args.length > 1 ? args[1] : "osm-cache");
        Files.createDirectories(cache);
        Path routesFile = fetchOnce(cache.resolve("jeepney_routes.osm"), ROUTES_QUERY);
        List<OsmRoute> routes = parseRoutes(routesFile);
        Path roadsFile = fetchOnce(cache.resolve("qc_roads.osm"), String.format(ROADS_QUERY, bbox(routes)));
        Roads roads = Roads.parse(roadsFile);
        System.out.println("Routes with a usable path: " + routes.size());

        assignIds(routes);
        for (OsmRoute r : routes) placeStops(r, routes);
        List<Place> places = sharePlaces(routes);
        nameAll(places, roads);
        write(dir, routes);
        System.out.println("Wrote " + routes.size() + " routes, " + places.size() + " stops to " + dir.toAbsolutePath());
    }

    // ---------------------------------------------------------------- fetching

    /** "south,west,north,east" around every route path, with ~300 m to spare. */
    private static String bbox(List<OsmRoute> routes) {
        double s = 90, w = 180, n = -90, e = -180;
        for (OsmRoute r : routes) {
            for (P p : r.path) { s = Math.min(s, p.lat()); n = Math.max(n, p.lat()); w = Math.min(w, p.lng()); e = Math.max(e, p.lng()); }
        }
        return String.format(Locale.ROOT, "%.4f,%.4f,%.4f,%.4f", s - 0.003, w - 0.003, n + 0.003, e + 0.003);
    }

    private static Path fetchOnce(Path file, String query) throws Exception {
        if (Files.exists(file) && Files.size(file) > 1000) return file;
        HttpClient http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(30)).build();
        String body = "data=" + URLEncoder.encode(query, StandardCharsets.UTF_8);
        long wait = 10_000;
        for (int attempt = 1; attempt <= 5; attempt++) {
            for (String url : OVERPASS) {
                HttpRequest req = HttpRequest.newBuilder(URI.create(url)).timeout(Duration.ofMinutes(7))
                        .header("User-Agent", "FareX student project (one-off route import)")
                        .header("Content-Type", "application/x-www-form-urlencoded")
                        .POST(HttpRequest.BodyPublishers.ofString(body)).build();
                HttpResponse<byte[]> res = http.send(req, HttpResponse.BodyHandlers.ofByteArray());
                String head = new String(res.body(), 0, Math.min(200, res.body().length), StandardCharsets.UTF_8);
                if (res.statusCode() == 200 && head.contains("<osm")) {
                    Files.write(file, res.body());
                    return file;
                }
                System.out.println("  Overpass busy at " + url + " (" + res.statusCode() + "), retrying");
            }
            Thread.sleep(wait);
            wait *= 2;
        }
        throw new IllegalStateException("Overpass did not answer; try again later");
    }

    // ---------------------------------------------------------------- parsing

    private static List<OsmRoute> parseRoutes(Path file) throws Exception {
        Document doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(file.toFile());
        Map<String, String> nodeNames = new HashMap<>();          // node id -> name, for platform members
        NodeList nodes = doc.getElementsByTagName("node");
        for (int i = 0; i < nodes.getLength(); i++) {
            Element n = (Element) nodes.item(i);
            String name = tags(n).get("name");
            if (name != null) nodeNames.put(n.getAttribute("id"), name);
        }
        NodeList rels = doc.getElementsByTagName("relation");
        List<OsmRoute> out = new ArrayList<>();
        for (int i = 0; i < rels.getLength(); i++) {
            Element rel = (Element) rels.item(i);
            Map<String, String> tags = tags(rel);
            // UV Express vans are mapped like jeepneys but charge their own fares: not ours
            if (tags.getOrDefault("network", "").contains("UV") || tags.getOrDefault("name", "").contains("UV Express")) continue;
            OsmRoute r = new OsmRoute();
            r.relId = rel.getAttribute("id");
            r.ref = tags.getOrDefault("ref", "").trim();
            r.name = tags.getOrDefault("name", "Jeepney " + r.relId).trim();
            r.from = tags.getOrDefault("from", "").trim();
            r.to = tags.getOrDefault("to", "").trim();

            List<List<P>> ways = new ArrayList<>();
            NodeList members = rel.getElementsByTagName("member");
            for (int m = 0; m < members.getLength(); m++) {
                Element mem = (Element) members.item(m);
                String role = mem.getAttribute("role");
                if (mem.getAttribute("type").equals("way") && role.isEmpty()) {
                    List<P> g = new ArrayList<>();
                    NodeList nds = mem.getElementsByTagName("nd");
                    for (int k = 0; k < nds.getLength(); k++) {
                        Element nd = (Element) nds.item(k);
                        if (nd.hasAttribute("lat")) g.add(new P(d(nd, "lat"), d(nd, "lon")));
                    }
                    if (g.size() >= 2) ways.add(g);
                } else if (mem.getAttribute("type").equals("node") && (role.startsWith("platform") || role.startsWith("stop"))
                        && mem.hasAttribute("lat")) {
                    r.platforms.add(new Platform(new P(d(mem, "lat"), d(mem, "lon")), nodeNames.get(mem.getAttribute("ref"))));
                }
            }
            r.path = chain(ways);
            if (r.path.size() < 2) continue;
            r.cum = cumulative(r.path);
            if (r.cum[r.cum.length - 1] < 1.0) continue;               // under 1 km: a fragment, not a route
            out.add(r);
        }
        return out;
    }

    private static Map<String, String> tags(Element e) {
        Map<String, String> t = new HashMap<>();
        NodeList kids = e.getChildNodes();
        for (int i = 0; i < kids.getLength(); i++) {
            if (kids.item(i) instanceof Element k && k.getTagName().equals("tag")) t.put(k.getAttribute("k"), k.getAttribute("v"));
        }
        return t;
    }

    private static double d(Element e, String attr) { return Double.parseDouble(e.getAttribute(attr)); }

    /** Joins a relation's ways end to end, flipping any way that is mapped against the route's direction. */
    static List<P> chain(List<List<P>> ways) {
        List<P> out = new ArrayList<>();
        for (int i = 0; i < ways.size(); i++) {
            List<P> w = new ArrayList<>(ways.get(i));
            if (out.isEmpty()) {
                if (i + 1 < ways.size()) {                          // orient the first way toward the second
                    List<P> nx = ways.get(i + 1);
                    double endToNext = Math.min(m(w.get(w.size() - 1), nx.get(0)), m(w.get(w.size() - 1), nx.get(nx.size() - 1)));
                    double startToNext = Math.min(m(w.get(0), nx.get(0)), m(w.get(0), nx.get(nx.size() - 1)));
                    if (startToNext < endToNext) Collections.reverse(w);
                }
                out.addAll(w);
                continue;
            }
            P end = out.get(out.size() - 1);
            if (m(end, w.get(w.size() - 1)) < m(end, w.get(0))) Collections.reverse(w);
            if (m(end, w.get(0)) < 1) w.remove(0);
            out.addAll(w);
        }
        return out;
    }

    // ---------------------------------------------------------------- ids

    /** id = OSM ref (or the relation id); routes mapped per direction get A/B and are one-way. */
    private static void assignIds(List<OsmRoute> routes) {
        Map<String, List<OsmRoute>> byRef = new LinkedHashMap<>();
        for (OsmRoute r : routes) byRef.computeIfAbsent(r.ref.isEmpty() ? "R" + r.relId : r.ref, k -> new ArrayList<>()).add(r);
        Set<String> used = new HashSet<>();
        for (var e : byRef.entrySet()) {
            String base = e.getKey().replaceAll("[^A-Za-z0-9-]", "");
            if (base.length() > 10) base = base.substring(base.length() - 10);
            List<OsmRoute> group = e.getValue();
            for (int i = 0; i < group.size(); i++) {
                OsmRoute r = group.get(i);
                r.oneway = group.size() > 1;
                String id = group.size() == 1 ? base : base + "-" + (char) ('A' + i);
                while (!used.add(id)) id = id + "x";
                r.id = id;
            }
        }
    }

    // ---------------------------------------------------------------- stops

    private record Sample(OsmRoute route, double km, P at) {}

    private static Map<Long, List<Sample>> grid;

    private static long cell(P p) {
        return (long) Math.floor(p.lat() / 0.0003) * 1_000_000L + (long) Math.floor(p.lng() / 0.0003);
    }

    private static void index(List<OsmRoute> routes) {
        grid = new HashMap<>();
        for (OsmRoute r : routes) {
            double total = r.cum[r.cum.length - 1];
            for (double km = 0; km <= total; km += SAMPLE_M / 1000) {
                P at = pointAt(r, km);
                grid.computeIfAbsent(cell(at), k -> new ArrayList<>()).add(new Sample(r, km, at));
            }
        }
    }

    /** Is a route from another group (different ref) running within NEAR_OTHER_M of p? */
    private static boolean nearOther(OsmRoute self, P p) {
        long c = cell(p);
        for (long dy = -1; dy <= 1; dy++) {
            for (long dx = -1; dx <= 1; dx++) {
                for (Sample s : grid.getOrDefault(c + dy * 1_000_000L + dx, List.of())) {
                    if (s.route() == self || (!self.ref.isEmpty() && self.ref.equals(s.route().ref))) continue;
                    if (m(p, s.at()) <= NEAR_OTHER_M) return true;
                }
            }
        }
        return false;
    }

    private static void placeStops(OsmRoute r, List<OsmRoute> all) {
        if (grid == null) index(all);
        double total = r.cum[r.cum.length - 1];
        List<Stop> cands = new ArrayList<>();
        cands.add(new Stop(0, 3, null));
        cands.add(new Stop(total, 3, null));
        for (Platform pl : r.platforms) {
            double[] hit = project(r, pl.at());
            if (hit[1] <= PLATFORM_SNAP_M) cands.add(new Stop(hit[0], 2, pl.name()));
        }
        // where other routes join, leave, cross or run alongside
        boolean inRun = false;
        double runStart = 0, lastMark = -1;
        for (double km = 0; km <= total + 1e-9; km += SAMPLE_M / 1000) {
            boolean shared = nearOther(r, pointAt(r, km));
            if (shared && !inRun) { inRun = true; runStart = km; cands.add(new Stop(km, 1, null)); lastMark = km; }
            else if (shared && km - lastMark >= 0.4) { cands.add(new Stop(km, 1, null)); lastMark = km; }
            else if (!shared && inRun) {
                inRun = false;
                double end = km - SAMPLE_M / 1000;
                cands.add(new Stop(end - runStart < 0.08 ? (runStart + end) / 2 : end, 1, null));
            }
        }
        cands.sort(Comparator.comparingDouble(s -> s.km));
        // merge stops of this route that are too close, keeping the more important one
        List<Stop> kept = new ArrayList<>();
        for (Stop s : cands) {
            Stop prev = kept.isEmpty() ? null : kept.get(kept.size() - 1);
            if (prev != null && (s.km - prev.km) * 1000 < MIN_GAP_M) {
                if (s.priority > prev.priority) kept.set(kept.size() - 1, s);
                continue;
            }
            kept.add(s);
        }
        // fill long gaps evenly
        List<Stop> filled = new ArrayList<>();
        for (int i = 0; i < kept.size(); i++) {
            filled.add(kept.get(i));
            if (i + 1 == kept.size()) break;
            double a = kept.get(i).km, b = kept.get(i + 1).km;
            int extra = (int) Math.ceil((b - a) * 1000 / MAX_GAP_M) - 1;
            for (int k = 1; k <= extra; k++) filled.add(new Stop(a + (b - a) * k / (extra + 1), 0, null));
        }
        r.stops = filled;
    }

    /** Stops of different routes within SHARED_STOP_M become one place; important stops claim first. */
    private static List<Place> sharePlaces(List<OsmRoute> routes) {
        record Ref(OsmRoute route, Stop stop) {}
        List<Ref> all = new ArrayList<>();
        for (OsmRoute r : routes) for (Stop s : r.stops) all.add(new Ref(r, s));
        all.sort((x, y) -> Integer.compare(y.stop().priority, x.stop().priority));

        List<Place> places = new ArrayList<>();
        Map<Long, List<Place>> pgrid = new HashMap<>();
        for (Ref ref : all) {
            P at = pointAt(ref.route(), ref.stop().km);
            Place found = null;
            long c = cell(at);
            outer:
            for (long dy = -3; dy <= 3; dy++) {                     // 3 cells of ~33 m covers SHARED_STOP_M
                for (long dx = -3; dx <= 3; dx++) {
                    for (Place p : pgrid.getOrDefault(c + dy * 1_000_000L + dx, List.of())) {
                        if (m(at, p.at) <= SHARED_STOP_M) { found = p; break outer; }
                    }
                }
            }
            if (found == null) {
                found = new Place(at);
                places.add(found);
                pgrid.computeIfAbsent(c, k -> new ArrayList<>()).add(found);
            }
            if (ref.stop().name != null) found.platformNames.add(ref.stop().name);
            ref.stop().place = found;
        }
        // a route passing the same place twice in a row (both ends of a short loop) keeps one
        for (OsmRoute r : routes) {
            List<Stop> dedup = new ArrayList<>();
            for (Stop s : r.stops) if (dedup.isEmpty() || dedup.get(dedup.size() - 1).place != s.place) dedup.add(s);
            r.stops = dedup;
        }
        return places;
    }

    // ---------------------------------------------------------------- naming

    /** Names, best first: OSM's own stop name, a FareX landmark, the intersection, the street. Kept unique. */
    private static void nameAll(List<Place> places, Roads roads) {
        Map<Place, String> chosen = new HashMap<>();
        for (Place p : places) {
            String osm = mostCommon(p.platformNames);
            if (osm != null) chosen.put(p, osm);
        }
        for (var lm : LANDMARKS.entrySet()) {                     // each landmark to its nearest unnamed place
            Place best = null;
            double bestM = LANDMARK_M;
            for (Place p : places) {
                double d = m(p.at, lm.getValue());
                if (!chosen.containsKey(p) && d <= bestM) { best = p; bestM = d; }
            }
            if (best != null) chosen.put(best, lm.getKey());
        }
        Set<String> taken = new HashSet<>();
        for (Place p : places) {
            String name = chosen.get(p);
            if (name == null) name = roads.intersectionName(p.at);
            if (name == null) name = String.format(Locale.ROOT, "Stop %.4f %.4f", p.at.lat(), p.at.lng());
            name = csv(name);                                       // cleaned before the uniqueness check
            String unique = name;
            for (int n = 2; !taken.add(unique); n++) unique = name + " (" + n + ")";
            p.name = unique;
        }
    }

    private static String mostCommon(List<String> names) {
        Map<String, Integer> c = new HashMap<>();
        for (String n : names) if (n != null && !n.isBlank()) c.merge(n.trim(), 1, Integer::sum);
        return c.entrySet().stream().max(Map.Entry.comparingByValue()).map(Map.Entry::getKey).orElse(null);
    }

    /** Named QC roads in a grid, to say which street a stop is on and which one crosses it. */
    static final class Roads {
        record Seg(String name, P a, P b) {}
        final Map<Long, List<Seg>> grid = new HashMap<>();

        static Roads parse(Path file) throws Exception {
            Roads roads = new Roads();
            Document doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(file.toFile());
            NodeList ways = doc.getElementsByTagName("way");
            for (int i = 0; i < ways.getLength(); i++) {
                Element w = (Element) ways.item(i);
                String name = tags(w).get("name");
                if (name == null) continue;
                NodeList nds = w.getElementsByTagName("nd");
                P prev = null;
                for (int k = 0; k < nds.getLength(); k++) {
                    Element nd = (Element) nds.item(k);
                    if (!nd.hasAttribute("lat")) continue;
                    P p = new P(d(nd, "lat"), d(nd, "lon"));
                    if (prev != null) {
                        Seg s = new Seg(shorten(name), prev, p);
                        roads.grid.computeIfAbsent(cell(prev), c -> new ArrayList<>()).add(s);
                        if (cell(p) != cell(prev)) roads.grid.computeIfAbsent(cell(p), c -> new ArrayList<>()).add(s);
                    }
                    prev = p;
                }
            }
            return roads;
        }

        /**
         * "Commonwealth Ave / Tandang Sora Ave" when another street meets within 60 m,
         * "Commonwealth Ave near Zuzuarregui St" when the nearest cross street is within 250 m,
         * else just the street.
         */
        String intersectionName(P p) {
            Map<String, Double> nearest = new HashMap<>();
            long c = cell(p);
            for (long dy = -8; dy <= 8; dy++) {
                for (long dx = -8; dx <= 8; dx++) {
                    for (Seg s : grid.getOrDefault(c + dy * 1_000_000L + dx, List.of())) {
                        double d = segMeters(p, s.a(), s.b());
                        nearest.merge(s.name(), d, Math::min);
                    }
                }
            }
            List<Map.Entry<String, Double>> sorted = new ArrayList<>(nearest.entrySet());
            sorted.sort(Map.Entry.comparingByValue());
            if (sorted.isEmpty() || sorted.get(0).getValue() > 80) return null;
            String main = sorted.get(0).getKey();
            if (sorted.size() > 1 && sorted.get(1).getValue() <= 60) return main + " / " + sorted.get(1).getKey();
            if (sorted.size() > 1 && sorted.get(1).getValue() <= 250) return main + " near " + sorted.get(1).getKey();
            return main;
        }

        static String shorten(String n) {
            return n.replaceAll("\\bAvenue\\b", "Ave").replaceAll("\\bStreet\\b", "St").replaceAll("\\bRoad\\b", "Rd")
                    .replaceAll("\\bBoulevard\\b", "Blvd").replaceAll("\\bHighway\\b", "Hwy")
                    .replaceAll("\\bExtension\\b", "Ext").replaceAll("\\bDrive\\b", "Dr");
        }
    }

    // ---------------------------------------------------------------- writing

    private static void write(Path dir, List<OsmRoute> routes) throws Exception {
        List<String> stops = new ArrayList<>(List.of("routeId,seq,name,lat,lng"));
        List<String> names = new ArrayList<>(List.of(RouteBook.NAMES_HEADER));
        List<String> shapes = new ArrayList<>(List.of(RouteShapes.HEADER));
        List<String> kms = new ArrayList<>(List.of(RoadDistances.HEADER));
        for (OsmRoute r : routes) {
            names.add(String.join(",", r.id, csv(r.name + (r.oneway && !r.to.isEmpty() ? " (to " + r.to + ")" : "")),
                    String.valueOf(r.oneway)));
            for (int i = 0; i < r.stops.size(); i++) {
                Place p = r.stops.get(i).place;
                stops.add(String.join(",", r.id, String.valueOf(i), csv(p.name), f(p.at.lat()), f(p.at.lng())));
                if (i + 1 == r.stops.size()) continue;
                double a = r.stops.get(i).km, b = r.stops.get(i + 1).km;
                List<P> seg = slice(r, a, b);
                for (int k = 0; k < seg.size(); k++) {
                    shapes.add(String.join(",", r.id, String.valueOf(i), String.valueOf(k), f(seg.get(k).lat()), f(seg.get(k).lng())));
                }
                kms.add(String.join(",", r.id, String.valueOf(i), String.format(Locale.ROOT, "%.3f", b - a)));
            }
        }
        Files.write(dir.resolve("routes.csv"), stops, StandardCharsets.UTF_8);
        Files.write(dir.resolve(RouteBook.NAMES_FILE), names, StandardCharsets.UTF_8);
        Files.write(dir.resolve(RouteShapes.FILE), shapes, StandardCharsets.UTF_8);
        Files.write(dir.resolve(RoadDistances.FILE), kms, StandardCharsets.UTF_8);
    }

    /** No commas (CSV) and none of the separators the server's text replies use (| ~ ;). */
    private static String csv(String s) { return s.replaceAll("[,|~;\\n]", " ").replaceAll(" +", " ").trim(); }
    private static String f(double v) { return String.format(Locale.ROOT, "%.6f", v); }

    // ---------------------------------------------------------------- geometry

    static double m(P a, P b) {
        double r = 6371000.0, la1 = Math.toRadians(a.lat()), la2 = Math.toRadians(b.lat());
        double dLat = la2 - la1, dLng = Math.toRadians(b.lng() - a.lng());
        double h = Math.sin(dLat / 2) * Math.sin(dLat / 2) + Math.cos(la1) * Math.cos(la2) * Math.sin(dLng / 2) * Math.sin(dLng / 2);
        return 2 * r * Math.asin(Math.sqrt(h));
    }

    private static double[] cumulative(List<P> path) {
        double[] c = new double[path.size()];
        for (int i = 1; i < path.size(); i++) c[i] = c[i - 1] + m(path.get(i - 1), path.get(i)) / 1000.0;
        return c;
    }

    static P pointAt(OsmRoute r, double km) {
        double[] c = r.cum;
        if (km <= 0) return r.path.get(0);
        if (km >= c[c.length - 1]) return r.path.get(r.path.size() - 1);
        int lo = 0, hi = c.length - 1;
        while (hi - lo > 1) { int mid = (lo + hi) >>> 1; if (c[mid] < km) lo = mid; else hi = mid; }
        double t = (c[hi] - c[lo]) <= 0 ? 0 : (km - c[lo]) / (c[hi] - c[lo]);
        P a = r.path.get(lo), b = r.path.get(hi);
        return new P(a.lat() + t * (b.lat() - a.lat()), a.lng() + t * (b.lng() - a.lng()));
    }

    /** The path between km a and km b (inclusive of both end points). */
    private static List<P> slice(OsmRoute r, double a, double b) {
        List<P> out = new ArrayList<>();
        out.add(pointAt(r, a));
        for (int i = 0; i < r.path.size(); i++) if (r.cum[i] > a && r.cum[i] < b) out.add(r.path.get(i));
        out.add(pointAt(r, b));
        return out;
    }

    /** {km along the route of the nearest point to p, metres away}. */
    private static double[] project(OsmRoute r, P p) {
        double bestKm = 0, bestM = Double.MAX_VALUE;
        for (int i = 1; i < r.path.size(); i++) {
            P a = r.path.get(i - 1), b = r.path.get(i);
            double[] t = closest(p, a, b);
            if (t[1] < bestM) { bestM = t[1]; bestKm = r.cum[i - 1] + t[0] * (r.cum[i] - r.cum[i - 1]); }
        }
        return new double[]{bestKm, bestM};
    }

    /** {t along a->b of the closest point to p, metres away}. Flat projection, fine at street scale. */
    private static double[] closest(P p, P a, P b) {
        double k = Math.cos(Math.toRadians(p.lat()));
        double ax = a.lng() * k, ay = a.lat(), bx = b.lng() * k, by = b.lat(), px = p.lng() * k, py = p.lat();
        double dx = bx - ax, dy = by - ay, len2 = dx * dx + dy * dy;
        double t = len2 == 0 ? 0 : Math.max(0, Math.min(1, ((px - ax) * dx + (py - ay) * dy) / len2));
        return new double[]{t, m(p, new P(ay + t * dy, (ax + t * dx) / k))};
    }

    private static double segMeters(P p, P a, P b) { return closest(p, a, b)[1]; }
}
