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

// A real map: OpenStreetMap streets drawn by Leaflet (resources/com/farex/ui/map/leaflet.html)
// inside a WebView, with FareX routes and a planned trip on top. Same sizes as MapView, so it
// drops in where MapView was.
//
// Needs internet for Leaflet and the map tiles. Until the page is up - or if it never comes up
// (offline, no javafx.web on the device) - the static MapView picture shows instead.
public class LeafletMap extends StackPane {

    private static final String PAGE = "/com/farex/ui/map/leaflet.html";

    // One ride of a planned trip: the route and its stops from boarding to alighting, in order
    public record TripLeg(String route, List<RouteStop> stops) {}

    private final MapView fallback;
    private WebView web;
    private boolean ready = false;

    // What to draw; kept so it can be drawn once the page is ready
    private Map<String, List<RouteStop>> routes = new LinkedHashMap<>();
    private Map<String, Map<Integer, List<RouteShapes.Point>>> shapes = new LinkedHashMap<>();   // road paths
    private String focusRoute;
    private List<TripLeg> trip = new ArrayList<>();

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
        Thread t = new Thread(() -> {
            try {
                Map<String, List<RouteStop>> fetched = Api.routeStops();
                Map<String, Map<Integer, List<RouteShapes.Point>>> roads = Api.routeShapes();
                Platform.runLater(() -> {
                    showRoutes(fetched, roads);
                    if (!rides.isEmpty()) showTrip(legsFor(fetched, rides));
                });
            } catch (RuntimeException offline) {
                // no server: streets only
            }
        }, "farex-map-routes");
        t.setDaemon(true);
        t.start();
        return this;
    }

    // Each ride's stops from boarding to alighting, for showTrip
    public static List<TripLeg> legsFor(Map<String, List<RouteStop>> allRoutes, List<Api.PlanRide> rides) {
        List<TripLeg> legs = new ArrayList<>();
        for (Api.PlanRide r : rides) {
            List<RouteStop> line = allRoutes.get(r.route());
            if (line == null) continue;
            List<RouteStop> stops = between(line, r.from(), r.to());
            if (stops.size() >= 2) legs.add(new TripLeg(r.route(), stops));
        }
        return legs;
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
        run("farexResize(); true");
        redraw();
    }

    private void resizeMap() {
        if (ready) run("farexResize(); true");
    }

    private void redraw() {
        if (!ready) return;
        run("farexDraw(" + json() + "); true");
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
            b.append("{\"route\":").append(str(leg.route())).append(",\"stops\":").append(stops(leg.stops()))
             .append(",\"path\":").append(points(legPath(leg))).append('}');
        }
        return b.append("]}").toString();
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
        Map<Integer, List<RouteShapes.Point>> segs = shapes.getOrDefault(routeId, Map.of());
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
