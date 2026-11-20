package com.farex.ui.common;

import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import javafx.application.Platform;
import javafx.concurrent.Worker;
import javafx.scene.layout.StackPane;
import javafx.scene.shape.Rectangle;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;

import com.farex.Api;
import farex.shared.data.RouteShapes;
import farex.shared.data.RouteStop;
import farex.shared.plan.TripProgress;

// A real map: OpenStreetMap streets drawn by Leaflet (resources/com/farex/ui/map/leaflet.html)
// inside a WebView, with FareX routes and a planned trip on top. Same sizes as MapView, so it
// drops in where MapView was.
//
// Needs internet for Leaflet and the map tiles. Until the page is up - or if it never comes up
// (offline, no javafx.web on the device) - the static MapView picture shows instead.
public class LeafletMap extends StackPane {

    private static final String PAGE = "/com/farex/ui/map/leaflet.html";

    // One part of a planned trip: a ride (its stops from boarding to alighting, in order) or a walk
    // (just its two ends; drawn dashed)
    // path: its road path when already known (a ride from or to a roadside point), else null
    public record TripLeg(String route, List<RouteStop> stops, boolean walk, List<RouteShapes.Point> path) {
        public TripLeg(String route, List<RouteStop> stops, boolean walk) { this(route, stops, walk, null); }
        public TripLeg(String route, List<RouteStop> stops) { this(route, stops, false, null); }
    }

    // The FareX network (stops and road paths), fetched once per run: it is ~2 MB with all QC routes
    private record Network(Map<String, List<RouteStop>> routes, Map<String, Map<Integer, List<RouteShapes.Point>>> shapes) {}
    private static volatile Network network;

    private static Network network() {
        Network n = network;
        if (n == null) network = n = new Network(Api.routeStops(), Api.routeShapes());
        return n;
    }

    private final MapView fallback;
    private WebView web;
    private boolean ready = false;

    // What to draw; kept so it can be drawn once the page is ready
    private Map<String, List<RouteStop>> routes = new LinkedHashMap<>();
    private Map<String, Map<Integer, List<RouteShapes.Point>>> shapes = new LinkedHashMap<>();   // road paths
    private String focusRoute;
    private List<TripLeg> trip = new ArrayList<>();
    private RouteStop pin;                                          // a picked point, or null
    private RouteShapes.Point me;                                   // tracked position, or null
    private boolean meOffRoute;
    private String ridingRoute;                                     // the jeep being ridden, or null
    private double bottomInset;                                     // px hidden under the app's cards

    // Map taps reach Java as the page's status text "farex-pick:lat,lng" (see farexTap in the page).
    // Not a Java object handed to JavaScript: getting `window` back from executeScript crashes the
    // JVM on JavaFX 21.0.5 + JDK 25/26, like scripts that evaluate to undefined.
    private static final String PICK = "farex-pick:";
    private java.util.function.BiConsumer<Double, Double> onPick;

    // Full background: grows to fill its parent
    public LeafletMap() {
        this(0);
    }

    // radius > 0 -> rounded corners
    public LeafletMap(double radius) {
        fallback = new MapView(radius);
        getChildren().add(fallback);

        Rectangle clip = new Rectangle();
        clip.setArcWidth(radius * 2);
        clip.setArcHeight(radius * 2);
        clip.widthProperty().bind(widthProperty());
        clip.heightProperty().bind(heightProperty());
        setClip(clip);
        setMinSize(0, 0);
        setPrefSize(0, 0);

        try {
            startWebMap();
        } catch (Throwable noWebView) {
            web = null;   // javafx.web not available here: keep the picture
        }
    }

    // Fixed size, e.g. new LeafletMap(320, 250, 12) for a card
    public LeafletMap(double width, double height, double radius) {
        this(radius);
        setPrefSize(width, height);
        setMinSize(width, height);
        setMaxSize(width, height);
    }

    // True once Leaflet and the map are actually showing (false = static picture)
    public boolean isLive() {
        return ready;
    }

    // ----- What to show -----

    // Fetches the FareX routes and their road paths from the server in the background and draws
    // them. If the server cannot be reached the map just shows the streets.
    public LeafletMap withFarexRoutes() {
        return withFarexTrip(List.of());
    }

    // Same, then highlights a planned trip (Api.PlanCard.rides()) and zooms to it
    public LeafletMap withFarexTrip(List<Api.PlanRide> rides) {
        return withFarexTrip(rides, Map.of());
    }

    // points: where named ends that are not stops are, e.g. "Pinned location" -> its coordinates
    public LeafletMap withFarexTrip(List<Api.PlanRide> rides, Map<String, RouteStop> points) {
        Thread t = new Thread(() -> {
            try {
                Network n = network();
                Platform.runLater(() -> {
                    showRoutes(n.routes(), n.shapes());
                    if (!rides.isEmpty()) showTrip(legsFor(n, rides, points));
                });
            } catch (RuntimeException offline) {
                // no server: streets only
            }
        }, "farex-map-routes");
        t.setDaemon(true);
        t.start();
        return this;
    }

    // Each ride's stops and road path from boarding to alighting, and each walk's two ends, for showTrip
    private static List<TripLeg> legsFor(Network n, List<Api.PlanRide> rides, Map<String, RouteStop> points) {
        List<TripLeg> legs = new ArrayList<>();
        for (Api.PlanRide r : rides) {
            if (r.walk()) {
                RouteStop a = end(n, points, r.from(), r.fromAt()), b = end(n, points, r.to(), r.toAt());
                if (a != null && b != null) legs.add(new TripLeg(Api.WALK, List.of(a, b), true));
                continue;
            }
            RideShape s = rideShape(n, r);
            if (s != null) legs.add(new TripLeg(r.route(), s.stops(), false, s.path()));
        }
        return legs;
    }

    // A planned trip as TripProgress legs: each ride along the jeep's road path, each walk as a
    // straight line. Fetches the network from the server on first use (call off the FX thread).
    // Empty if a leg cannot be placed (e.g. a stop no route has any more).
    public static List<TripProgress.Ride> progressLegs(List<Api.PlanRide> rides, Map<String, RouteStop> points) {
        Network n = network();
        List<TripProgress.Ride> out = new ArrayList<>();
        for (Api.PlanRide r : rides) {
            List<RouteShapes.Point> path;
            if (r.walk()) {
                RouteStop a = end(n, points, r.from(), r.fromAt()), b = end(n, points, r.to(), r.toAt());
                if (a == null || b == null) return List.of();
                path = List.of(point(a), point(b));
            } else {
                RideShape s = rideShape(n, r);
                if (s == null || s.path().size() < 2) return List.of();
                path = s.path();
            }
            out.add(new TripProgress.Ride(r.route(), r.from(), r.to(), path));
        }
        return out;
    }

    // Where a walk starts or ends: the coordinates the planner sent, else a stop or pin by name
    private static RouteStop end(Network n, Map<String, RouteStop> points, String name, RouteStop at) {
        return at != null ? at : find(n.routes(), points, name);
    }

    // The far end of the route in the direction this ride goes: where the jeep is headed
    // ("Bagong Silang"), for the tracking screen. Null when unknown. Call off the FX thread.
    public static String headedTo(Api.PlanRide r) {
        Network n = network();
        List<RouteStop> line = n.routes().get(r.route());
        RideShape s = line == null ? null : rideShape(n, r);
        if (s == null) return null;
        return (s.forward() ? line.get(line.size() - 1) : line.get(0)).name();
    }

    // ----- one ride's geometry -----

    // The stops passed (first = where you board, last = where you get off) and the road path, and
    // whether that runs in the route's own stop order
    private record RideShape(List<RouteStop> stops, List<RouteShapes.Point> path, boolean forward) {}

    // Both ends stops of the route: stop to stop along its road segments. An end that is no stop
    // ("the roadside near X": the jeep flagged down on its road) is placed by its coordinates,
    // cutting the route's road where it passes closest.
    private static RideShape rideShape(Network n, Api.PlanRide r) {
        List<RouteStop> line = n.routes().get(r.route());
        if (line == null || line.size() < 2) return null;
        Map<Integer, List<RouteShapes.Point>> segs = n.shapes().getOrDefault(r.route(), Map.of());
        int a = indexOf(line, r.from()), b = indexOf(line, r.to());
        if (a >= 0 && b >= 0 && a != b) return new RideShape(between(line, r.from(), r.to()), roadPath(segs, line, a, b), a < b);

        // The whole route as one path, with each stop's km along it
        List<RouteShapes.Point> full = new ArrayList<>();
        double[] stopKm = new double[line.size()];
        for (int i = 0; i + 1 < line.size(); i++) {
            if (!full.isEmpty()) stopKm[i] = km(full);
            List<RouteShapes.Point> seg = roadPath(segs, line, i, i + 1);
            full.addAll(seg);
        }
        stopKm[line.size() - 1] = km(full);
        double[] cum = new double[full.size()];
        for (int i = 1; i < full.size(); i++) cum[i] = cum[i - 1] + dist(full.get(i - 1), full.get(i));

        double from = a >= 0 ? stopKm[a] : (r.fromAt() == null ? -1 : project(full, cum, r.fromAt()));
        double to = b >= 0 ? stopKm[b] : (r.toAt() == null ? -1 : project(full, cum, r.toAt()));
        if (from < 0 || to < 0 || Math.abs(to - from) < 1e-6) return null;
        boolean forward = from < to;

        List<RouteStop> stops = new ArrayList<>();
        stops.add(a >= 0 ? line.get(a) : r.fromAt());
        for (int i = 0; i < line.size(); i++) {
            int k = forward ? i : line.size() - 1 - i;
            double at = stopKm[k];
            if (forward ? (at > from + 1e-6 && at < to - 1e-6) : (at < from - 1e-6 && at > to + 1e-6)) stops.add(line.get(k));
        }
        stops.add(b >= 0 ? line.get(b) : r.toAt());

        List<RouteShapes.Point> path = slice(full, cum, Math.min(from, to), Math.max(from, to));
        if (!forward) Collections.reverse(path);
        return new RideShape(stops, path, forward);
    }

    // km along `path` (cumulative `cum`) of the point nearest to p
    private static double project(List<RouteShapes.Point> path, double[] cum, RouteStop p) {
        double k = Math.cos(Math.toRadians(p.lat())), px = p.lng() * k, py = p.lat();
        double best = Double.MAX_VALUE, at = -1;
        for (int i = 1; i < path.size(); i++) {
            RouteShapes.Point a = path.get(i - 1), b = path.get(i);
            double ax = a.lng() * k, ay = a.lat(), dx = b.lng() * k - ax, dy = b.lat() - ay, len2 = dx * dx + dy * dy;
            double t = len2 == 0 ? 0 : Math.max(0, Math.min(1, ((px - ax) * dx + (py - ay) * dy) / len2));
            double ex = ax + t * dx - px, ey = ay + t * dy - py, d = ex * ex + ey * ey;
            if (d < best) { best = d; at = cum[i - 1] + t * (cum[i] - cum[i - 1]); }
        }
        return at;
    }

    // The part of `path` between km lo and hi along it
    private static List<RouteShapes.Point> slice(List<RouteShapes.Point> path, double[] cum, double lo, double hi) {
        List<RouteShapes.Point> out = new ArrayList<>();
        out.add(pointAt(path, cum, lo));
        for (int i = 0; i < path.size(); i++) if (cum[i] > lo && cum[i] < hi) out.add(path.get(i));
        out.add(pointAt(path, cum, hi));
        return out;
    }

    private static RouteShapes.Point pointAt(List<RouteShapes.Point> path, double[] cum, double km) {
        int i = 1;
        while (i < path.size() - 1 && cum[i] < km) i++;
        double seg = cum[i] - cum[i - 1], t = seg <= 0 ? 0 : Math.max(0, Math.min(1, (km - cum[i - 1]) / seg));
        RouteShapes.Point a = path.get(i - 1), b = path.get(i);
        return new RouteShapes.Point(a.lat() + t * (b.lat() - a.lat()), a.lng() + t * (b.lng() - a.lng()));
    }

    private static double km(List<RouteShapes.Point> path) {
        double km = 0;
        for (int i = 1; i < path.size(); i++) km += dist(path.get(i - 1), path.get(i));
        return km;
    }

    private static double dist(RouteShapes.Point a, RouteShapes.Point b) {
        return farex.shared.fare.Fares.haversineKm(a.lat(), a.lng(), b.lat(), b.lng());
    }

    // Pixels at the bottom of the map hidden by a card on top: zooming to a route keeps clear of them
    public void setBottomInset(double px) {
        bottomInset = px;
        if (ready) run(String.format(Locale.ROOT, "farexInset(%.0f); true", px));
    }

    // While riding (trip tracking): the whole route of the jeep you are on, faint, so you see where
    // it goes after your stop. null hides it.
    public void showRiding(String routeId) {
        if (java.util.Objects.equals(routeId, ridingRoute)) return;
        ridingRoute = routeId;
        redraw();
    }

    // "You are here" while a trip is tracked: a blue dot (red when off route); the map pans to keep it
    // in view without re-fitting the whole trip. null hides it.
    public void showMe(RouteShapes.Point at, boolean offRoute) {
        me = at;
        meOffRoute = offRoute;
        if (ready) run(meScript());
    }

    private String meScript() {
        return me == null ? "farexMe(null); true"
                : String.format(Locale.ROOT, "farexMe(%.6f,%.6f,%b); true", me.lat(), me.lng(), meOffRoute);
    }

    // A stop by name on any route, or one of the extra named points
    private static RouteStop find(Map<String, List<RouteStop>> allRoutes, Map<String, RouteStop> points, String name) {
        RouteStop p = points.get(name);
        if (p != null) return p;
        for (List<RouteStop> line : allRoutes.values()) for (RouteStop s : line) if (s.name().equals(name)) return s;
        return null;
    }

    // Lets the user tap the map to choose a point; also drops a pin there
    public void onPick(java.util.function.BiConsumer<Double, Double> handler) {
        onPick = handler;
        if (ready) run("farexEnablePick(); true");
    }

    // A pin with a label (a picked start or destination); null removes it
    public void showPin(RouteStop at) {
        pin = at;
        redraw();
    }

    // Every route as a coloured line with its stops, straight from stop to stop
    public void showRoutes(Map<String, List<RouteStop>> allRoutes) {
        showRoutes(allRoutes, Map.of());
    }

    // Every route as a coloured line along the roads (Api.routeShapes()); a segment without a
    // road path is drawn straight
    public void showRoutes(Map<String, List<RouteStop>> allRoutes,
                           Map<String, Map<Integer, List<RouteShapes.Point>>> roadPaths) {
        routes = new LinkedHashMap<>(allRoutes);
        shapes = new LinkedHashMap<>(roadPaths);
        redraw();
    }

    // Highlight one route (by id) and zoom to it; null shows them all again
    public void focusRoute(String routeId) {
        focusRoute = routeId;
        trip = new ArrayList<>();
        redraw();
    }

    // Highlight a planned trip, ride by ride, and zoom to it
    public void showTrip(List<TripLeg> legs) {
        trip = new ArrayList<>(legs);
        redraw();
    }

    // The stops you pass riding `route` from one stop name to another, in riding order.
    // Works in either direction. Empty if the route does not serve both stops.
    public static List<RouteStop> between(List<RouteStop> route, String from, String to) {
        int a = -1;
        int b = -1;
        for (int i = 0; i < route.size(); i++) {
            if (a < 0 && route.get(i).name().equals(from)) a = i;
            if (b < 0 && route.get(i).name().equals(to)) b = i;
        }
        List<RouteStop> out = new ArrayList<>();
        if (a < 0 || b < 0) return out;
        int step = a <= b ? 1 : -1;
        for (int i = a; i != b + step; i += step) out.add(route.get(i));
        return out;
    }

    // ----- WebView -----

    private void startWebMap() {
        URL page = LeafletMap.class.getResource(PAGE);
        if (page == null) return;

        web = new WebView();
        web.setContextMenuEnabled(false);
        WebEngine engine = web.getEngine();
        // OpenStreetMap's tile policy asks apps to identify themselves
        engine.setUserAgent(engine.getUserAgent() + " FareX/1.0");

        engine.getLoadWorker().stateProperty().addListener((obs, old, state) -> {
            if (state == Worker.State.SUCCEEDED) goLive();
        });
        engine.setOnStatusChanged(ev -> {                           // a map tap: "farex-pick:14.73,121.05"
            String s = ev.getData();
            if (onPick == null || s == null || !s.startsWith(PICK)) return;
            String[] ll = s.substring(PICK.length()).split(",");
            try {
                onPick.accept(Double.parseDouble(ll[0]), Double.parseDouble(ll[1]));
            } catch (RuntimeException badStatus) {
                // not one of ours
            }
        });
        widthProperty().addListener((o, a, b) -> resizeMap());
        heightProperty().addListener((o, a, b) -> resizeMap());

        engine.load(page.toExternalForm());
        getChildren().add(0, web);   // under the picture, which covers it until Leaflet is up
    }

    private void goLive() {
        Object up = run("typeof L !== 'undefined' && window.farexReady === true");
        if (!Boolean.TRUE.equals(up)) return;   // offline: Leaflet did not load, keep the picture
        ready = true;
        fallback.setVisible(false);
        if (onPick != null) run("farexEnablePick(); true");
        if (bottomInset > 0) run(String.format(Locale.ROOT, "farexInset(%.0f); true", bottomInset));
        run("farexResize(); true");
        redraw();
    }

    private void resizeMap() {
        if (ready) run("farexResize(); true");
    }

    private void redraw() {
        if (!ready) return;
        run("farexDraw(" + json() + "); true");
        if (me != null) {                // a redraw re-fits to the trip: back to following the dot
            run("farexMe(null); true");
            run(meScript());
        }
    }

    // Every script must end in a value. executeScript on a script that evaluates to
    // undefined crashes the JVM (JavaFX 21.0.5 WebView on JDK 25/26), hence the "; true".
    private Object run(String script) {
        return web.getEngine().executeScript(script);
    }

    // {"routes":[{"id":"R1","stops":[{"name":"..","lat":..,"lng":..}]}],"focus":"R1","trip":[{"route":"R1","stops":[..]}]}
    private String json() {
        StringBuilder b = new StringBuilder("{\"routes\":[");
        boolean first = true;
        for (Map.Entry<String, List<RouteStop>> r : routes.entrySet()) {
            if (!first) b.append(',');
            first = false;
            List<RouteStop> line = r.getValue();
            b.append("{\"id\":").append(str(r.getKey())).append(",\"stops\":").append(stops(line))
             .append(",\"path\":").append(points(roadPath(r.getKey(), line, 0, line.size() - 1))).append('}');
        }
        b.append("],\"focus\":").append(focusRoute == null ? "null" : str(focusRoute)).append(",\"trip\":[");
        for (int i = 0; i < trip.size(); i++) {
            if (i > 0) b.append(',');
            TripLeg leg = trip.get(i);
            b.append("{\"route\":").append(str(leg.route())).append(",\"walk\":").append(leg.walk())
             .append(",\"stops\":").append(stops(leg.stops()))
             .append(",\"path\":").append(points(leg.path() != null ? leg.path() : legPath(leg))).append('}');
        }
        b.append("],\"pin\":").append(pin == null ? "null" : stops(List.of(pin)));
        b.append(",\"riding\":").append(ridingRoute == null ? "null" : str(ridingRoute));
        return b.append("}").toString();
    }

    // The road path of one ride, from its first stop to its last, either direction
    private List<RouteShapes.Point> legPath(TripLeg leg) {
        List<RouteStop> line = routes.get(leg.route());
        List<RouteStop> s = leg.stops();
        if (line == null || s.size() < 2) return asPoints(s);
        int a = indexOf(line, s.get(0).name());
        int b = indexOf(line, s.get(s.size() - 1).name());
        if (a < 0 || b < 0) return asPoints(s);
        return roadPath(leg.route(), line, a, b);
    }

    // Stop a to stop b of a route along its road segments (segment i = stop i to i+1). Riding
    // backwards uses the same roads reversed - close enough for a map, though a one-way street
    // would really send the jeep another way. A segment with no road path is a straight line.
    private List<RouteShapes.Point> roadPath(String routeId, List<RouteStop> line, int a, int b) {
        return roadPath(shapes.getOrDefault(routeId, Map.of()), line, a, b);
    }

    private static List<RouteShapes.Point> roadPath(Map<Integer, List<RouteShapes.Point>> segs,
                                                    List<RouteStop> line, int a, int b) {
        List<RouteShapes.Point> out = new ArrayList<>();
        int step = a <= b ? 1 : -1;
        for (int i = a; i != b; i += step) {
            int seg = Math.min(i, i + step);
            List<RouteShapes.Point> road = segs.getOrDefault(seg, List.of());
            if (road.isEmpty()) {
                road = List.of(point(line.get(i)), point(line.get(i + step)));
            } else if (step < 0) {
                road = new ArrayList<>(road);
                Collections.reverse(road);
            }
            out.addAll(road);
        }
        return out;
    }

    private static int indexOf(List<RouteStop> line, String name) {
        for (int i = 0; i < line.size(); i++) if (line.get(i).name().equals(name)) return i;
        return -1;
    }

    private static RouteShapes.Point point(RouteStop s) {
        return new RouteShapes.Point(s.lat(), s.lng());
    }

    private static List<RouteShapes.Point> asPoints(List<RouteStop> s) {
        List<RouteShapes.Point> out = new ArrayList<>();
        for (RouteStop x : s) out.add(point(x));
        return out;
    }

    // [[lat,lng],...]
    private static String points(List<RouteShapes.Point> list) {
        StringBuilder b = new StringBuilder("[");
        for (int i = 0; i < list.size(); i++) {
            if (i > 0) b.append(',');
            b.append(String.format(Locale.ROOT, "[%.6f,%.6f]", list.get(i).lat(), list.get(i).lng()));
        }
        return b.append(']').toString();
    }

    private static String stops(List<RouteStop> list) {
        StringBuilder b = new StringBuilder("[");
        for (int i = 0; i < list.size(); i++) {
            RouteStop s = list.get(i);
            if (i > 0) b.append(',');
            b.append(String.format(Locale.ROOT, "{\"name\":%s,\"lat\":%.6f,\"lng\":%.6f}", str(s.name()), s.lat(), s.lng()));
        }
        return b.append(']').toString();
    }

    // JSON string; also escapes < and the line separators so a stop name cannot break the script
    private static String str(String s) {
        StringBuilder b = new StringBuilder("\"");
        for (char c : s.toCharArray()) {
            switch (c) {
                case '"' -> b.append("\\\"");
                case '\\' -> b.append("\\\\");
                case '\n' -> b.append("\\n");
                case '\r' -> b.append("\\r");
                case '<' -> b.append("\\u003c");
                case ' ' -> b.append("\\u2028");
                case ' ' -> b.append("\\u2029");
                default -> { if (c < 0x20) b.append(String.format("\\u%04x", (int) c)); else b.append(c); }
            }
        }
        return b.append('"').toString();
    }
}
