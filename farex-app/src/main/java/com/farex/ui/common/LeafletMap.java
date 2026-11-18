package com.farex.ui.common;

import java.net.URL;
import java.util.ArrayList;
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

    // Fetches the FareX routes from the server in the background and draws them.
    // If the server cannot be reached the map just shows the streets.
    public LeafletMap withFarexRoutes() {
        Thread t = new Thread(() -> {
            try {
                Map<String, List<RouteStop>> fetched = Api.routeStops();
                Platform.runLater(() -> showRoutes(fetched));
            } catch (RuntimeException offline) {
                // no server: streets only
            }
        }, "farex-map-routes");
        t.setDaemon(true);
        t.start();
        return this;
    }

    // Every route as a coloured line with its stops
    public void showRoutes(Map<String, List<RouteStop>> allRoutes) {
        routes = new LinkedHashMap<>(allRoutes);
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
            b.append("{\"id\":").append(str(r.getKey())).append(",\"stops\":").append(stops(r.getValue())).append('}');
        }
        b.append("],\"focus\":").append(focusRoute == null ? "null" : str(focusRoute)).append(",\"trip\":[");
        for (int i = 0; i < trip.size(); i++) {
            if (i > 0) b.append(',');
            TripLeg leg = trip.get(i);
            b.append("{\"route\":").append(str(leg.route())).append(",\"stops\":").append(stops(leg.stops())).append('}');
        }
        return b.append("]}").toString();
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
