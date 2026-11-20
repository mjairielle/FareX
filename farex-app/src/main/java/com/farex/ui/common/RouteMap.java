package com.farex.ui.common;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javafx.application.Platform;
import javafx.geometry.Point2D;
import javafx.geometry.VPos;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.scene.shape.StrokeLineCap;
import javafx.scene.shape.StrokeLineJoin;
import javafx.scene.text.Font;
import javafx.scene.text.Text;

import com.farex.Api;
import com.gluonhq.maps.MapLayer;
import com.gluonhq.maps.MapPoint;
import farex.shared.data.RouteShapes;
import farex.shared.data.RouteStop;
import farex.shared.plan.TripProgress;

// A real map: street tiles drawn by Gluon Maps (pure JavaFX; tiles from VoyagerTiles), with the
// FareX routes, a planned trip, a pin and "you are here" painted on one Canvas on top.
// Drag to move, pinch or scroll to zoom, tap to pick a point (onPick).
//
// Not a WebView: Android draws its browser view above the whole app, and it stayed white there.
// Needs internet for the tiles; offline the routes still draw, on a grey background.
public class RouteMap extends StackPane {

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

    private final com.gluonhq.maps.MapView view = new com.gluonhq.maps.MapView();
    private final Overlay overlay = new Overlay();

    // What to draw
    private Map<String, List<RouteStop>> routes = new LinkedHashMap<>();
    private Map<String, Map<Integer, List<RouteShapes.Point>>> shapes = new LinkedHashMap<>();   // road paths
    private String focusRoute;
    private List<TripLeg> trip = new ArrayList<>();
    private RouteStop pin;                                          // a picked point, or null
    private RouteShapes.Point me;                                   // tracked position, or null
    private boolean meOffRoute;
    private String ridingRoute;                                     // the jeep being ridden, or null
    private double bottomInset;                                     // px hidden under the app's cards
    private double topInset;
    private List<RouteShapes.Point> fit = List.of();                // what the view zooms to after a change
    private java.util.function.BiConsumer<Double, Double> onPick;

    // Full background: grows to fill its parent
    public RouteMap() {
        this(0);
    }

    // radius > 0 -> rounded corners
    public RouteMap(double radius) {
        view.setZoom(12);
        view.setCenter(14.65, 121.05);                              // Metro Manila until there is something to fit
        view.addLayer(overlay);
        getChildren().add(view);

        Rectangle clip = new Rectangle();
        clip.setArcWidth(radius * 2);
        clip.setArcHeight(radius * 2);
        clip.widthProperty().bind(widthProperty());
        clip.heightProperty().bind(heightProperty());
        setClip(clip);
        setMinSize(0, 0);
        setPrefSize(0, 0);
        setStyle("-fx-background-color: #E5E3DF;");                // under tiles still loading

        // A tap (not the end of a drag) picks that point
        view.addEventHandler(MouseEvent.MOUSE_CLICKED, e -> {
            if (onPick == null || e.getButton() != MouseButton.PRIMARY || !e.isStillSincePress()) return;
            MapPoint p = view.getMapPosition(e.getX(), e.getY());
            onPick.accept(p.getLatitude(), p.getLongitude());
        });
        // laid out after the first fit was asked for: fit again at the real size
        widthProperty().addListener((o, a, b) -> refit());
        heightProperty().addListener((o, a, b) -> refit());
    }

    // Fixed size, e.g. new RouteMap(320, 250, 12) for a card
    public RouteMap(double width, double height, double radius) {
        this(radius);
        setPrefSize(width, height);
        setMinSize(width, height);
        setMaxSize(width, height);
    }

    // ----- What to show -----

    // Fetches the FareX routes and their road paths from the server in the background and draws
    // them. If the server cannot be reached the map just shows the streets.
    public RouteMap withFarexRoutes() {
        return withFarexTrip(List.of());
    }

    // Same, then highlights a planned trip (Api.PlanCard.rides()) and zooms to it
    public RouteMap withFarexTrip(List<Api.PlanRide> rides) {
        return withFarexTrip(rides, Map.of());
    }

    // points: where named ends that are not stops are, e.g. "Pinned location" -> its coordinates
    public RouteMap withFarexTrip(List<Api.PlanRide> rides, Map<String, RouteStop> points) {
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
        overlay.repaint();
    }

    // Same for a card over the top of the map (the Routes tab's From / To bar)
    public void setTopInset(double px) {
        topInset = px;
        overlay.repaint();
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
        boolean first = me == null;
        me = at;
        meOffRoute = offRoute;
        if (at != null) follow(at, first);
        overlay.repaint();
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

    // ----- Drawing -----

    private static final Color LIME = Color.web("#CAFE06"), INK = Color.web("#111111");
    private static final Color[] COLORS = {Color.web("#3B82F6"), Color.web("#F97316"), Color.web("#A855F7"), Color.web("#14B8A6"),
            Color.web("#EF4444"), Color.web("#EAB308"), Color.web("#EC4899"), Color.web("#22C55E")};
    private static final double PAD = 36;                           // px kept clear around a fit

    // Something to show changed: zoom to it (or back to the passenger's dot while tracking) and repaint
    private void redraw() {
        List<RouteShapes.Point> f = new ArrayList<>();
        boolean many = routes.size() > 20;
        if (trip.isEmpty()) {
            for (Map.Entry<String, List<RouteStop>> r : routes.entrySet())
                if (r.getKey().equals(focusRoute) || (focusRoute == null && !many)) f.addAll(routePath(r.getKey()));
        }
        for (TripLeg leg : trip) f.addAll(legLine(leg));
        if (pin != null && trip.isEmpty() && f.isEmpty()) f.add(point(pin));   // a focused route keeps its own fit
        fit = f;
        if (me != null) follow(me, true);
        else refit();
        overlay.repaint();
    }

    // Zoom so everything in `fit` shows between the top and bottom cards
    private void refit() {
        double w = getWidth() - 2 * PAD, h = getHeight() - topInset - bottomInset - 2 * PAD;
        if (w <= 0 || h <= 0 || fit.isEmpty()) return;
        double minX = 1, maxX = 0, minY = 1, maxY = 0;
        for (RouteShapes.Point p : fit) {
            double x = mercX(p.lng()), y = mercY(p.lat());
            minX = Math.min(minX, x); maxX = Math.max(maxX, x);
            minY = Math.min(minY, y); maxY = Math.max(maxY, y);
        }
        double zoom = fit.size() == 1 ? Math.max(view.getZoom(), 15)
                : Math.floor(Math.log(Math.min(w / ((maxX - minX) * 256), h / ((maxY - minY) * 256))) / Math.log(2));
        centerOn(latOf((minY + maxY) / 2), lngOf((minX + maxX) / 2), Math.max(3, Math.min(16, zoom)));   // street level at most
    }

    // Puts the point in the middle of what the cards leave visible, not of the whole map
    private void centerOn(double lat, double lng, double zoom) {
        double world = 256 * Math.pow(2, zoom);                    // Gluon's tiles are 256 px
        view.setZoom(zoom);
        view.setCenter(latOf(mercY(lat) + (bottomInset - topInset) / 2 / world), lng);
    }

    // Tracking: the first fix goes to street level on the dot; later ones pan only when it nears the edge
    private void follow(RouteShapes.Point at, boolean first) {
        if (first) {
            centerOn(at.lat(), at.lng(), 15);
            return;
        }
        Point2D p = overlay.toScreen(at.lat(), at.lng());
        double w = getWidth(), h = getHeight();
        if (p.getX() < w * 0.1 || p.getX() > w * 0.9 || p.getY() < topInset + h * 0.1 || p.getY() > h - bottomInset - h * 0.1)
            centerOn(at.lat(), at.lng(), view.getZoom());
    }

    // Web Mercator, the whole world as 0..1 across and down (what map tiles use)
    private static double mercX(double lng) { return (lng + 180) / 360; }
    private static double mercY(double lat) {
        double r = Math.toRadians(lat);
        return (1 - Math.log(Math.tan(r) + 1 / Math.cos(r)) / Math.PI) / 2;
    }
    private static double lngOf(double x) { return x * 360 - 180; }
    private static double latOf(double y) { return Math.toDegrees(Math.atan(Math.sinh(Math.PI * (1 - 2 * y)))); }

    private List<RouteShapes.Point> routePath(String id) {
        List<RouteStop> line = routes.get(id);
        return roadPath(id, line, 0, line.size() - 1);
    }

    private List<RouteShapes.Point> legLine(TripLeg leg) {
        if (leg.path() != null && leg.path().size() > 1) return leg.path();
        return leg.walk() ? asPoints(leg.stops()) : legPath(leg);
    }

    // The routes, trip, pin and dot, painted on one Canvas over the tiles; repainted on every move
    private final class Overlay extends MapLayer {
        private final Canvas canvas = new Canvas();
        private double scale, ox, oy;                               // Mercator 0..1 -> pixels, for this paint

        Overlay() {
            setMouseTransparent(true);                              // drags and taps go to the map
            getChildren().add(canvas);
        }

        void repaint() {
            markDirty();
        }

        Point2D toScreen(double lat, double lng) {
            return new Point2D(mercX(lng) * scale + ox, mercY(lat) * scale + oy);
        }

        @Override
        protected void layoutLayer() {
            // Gluon's own projection for two points gives scale and offset; then every point is one multiply
            Point2D a = getMapPoint(0, 0), b = getMapPoint(0, 90);
            scale = (b.getX() - a.getX()) / 0.25;
            ox = a.getX() - 0.5 * scale;
            oy = a.getY() - 0.5 * scale;
            paint();
        }

        private void paint() {
            double w = view.getWidth(), h = view.getHeight();
            canvas.setWidth(w);
            canvas.setHeight(h);
            GraphicsContext g = canvas.getGraphicsContext2D();
            g.clearRect(0, 0, w, h);
            g.setLineCap(StrokeLineCap.ROUND);
            g.setLineJoin(StrokeLineJoin.ROUND);
            boolean hasTrip = !trip.isEmpty(), many = routes.size() > 20;

            // The network; the focused route last so it sits on top
            int i = 0, focusedIndex = -1;
            for (String id : routes.keySet()) {
                if (id.equals(focusRoute) && !hasTrip) focusedIndex = i;
                else route(g, id, i, hasTrip, many);
                i++;
            }
            if (focusedIndex >= 0) route(g, focusRoute, focusedIndex, false, many);

            if (hasTrip) {
                int rides = 0;
                for (TripLeg leg : trip) {
                    List<RouteShapes.Point> pts = legLine(leg);
                    if (leg.walk()) {
                        line(g, pts, Color.WHITE, 4, 0.95, 2, 8);  // walking: dashed white
                    } else {
                        line(g, pts, LIME, 7, 0.95);
                        RouteStop board = leg.stops().get(0);
                        dot(g, board.lat(), board.lng(), 7, INK, 2, LIME, 1);
                        // the first boarding keeps a label; transfers would pile up when zoomed out
                        if (rides == 0) label(g, "Board: " + board.name(), board.lat(), board.lng(), 7, true);
                        rides++;
                    }
                }
                RouteStop first = trip.get(0).stops().get(0);
                if (trip.get(0).walk()) {
                    dot(g, first.lat(), first.lng(), 7, LIME, 3, Color.WHITE, 1);
                    label(g, "Start: " + first.name(), first.lat(), first.lng(), 7, false);
                }
                List<RouteStop> lastStops = trip.get(trip.size() - 1).stops();
                RouteStop end = lastStops.get(lastStops.size() - 1);
                dot(g, end.lat(), end.lng(), 8, LIME, 3, INK, 1);
                label(g, "End: " + end.name(), end.lat(), end.lng(), 8, true);
            }
            if (pin != null) {
                dot(g, pin.lat(), pin.lng(), 9, INK, 3, LIME, 1);
                label(g, pin.name(), pin.lat(), pin.lng(), 9, true);
            }
            if (me != null) {
                dot(g, me.lat(), me.lng(), 8, Color.WHITE, 3, meOffRoute ? Color.web("#EF4444") : Color.web("#3B82F6"), 1);
                label(g, "You", me.lat(), me.lng(), 8, false);
            }

            // The tile owners ask to be credited
            g.setFont(Font.font(Theme.FONT, 9));
            g.setTextBaseline(VPos.BOTTOM);
            g.setGlobalAlpha(0.8);
            g.setFill(INK);
            String credit = VoyagerTiles.CREDIT;
            g.fillText(credit, w - width(credit, g.getFont()) - 6, h - bottomInset - 2);
            g.setGlobalAlpha(1);
        }

        private void route(GraphicsContext g, String id, int index, boolean hasTrip, boolean many) {
            boolean focused = !hasTrip && id.equals(focusRoute);
            boolean dim = hasTrip || (focusRoute != null && !focused);
            Color color = COLORS[index % COLORS.length];
            List<RouteStop> stops = routes.get(id);
            if (hasTrip && id.equals(ridingRoute)) {               // the jeep you are on: its whole route, faint
                line(g, routePath(id), LIME, 4, 0.55, 8, 8);
                RouteStop last = stops.get(stops.size() - 1);
                dot(g, last.lat(), last.lng(), 5, INK, 2, LIME, 0.8);
                return;
            }
            line(g, routePath(id), focused ? LIME : color, focused ? 6 : (many ? 2.5 : 4), hasTrip ? 0.15 : (dim ? 0.3 : 0.85));
            if (hasTrip && many) return;                            // a trip shown: the rest is faint context, no dots
            double r = focused ? 6 : (many ? 2.5 : 4);
            for (RouteStop s : stops) dot(g, s.lat(), s.lng(), r, INK, 1, focused ? LIME : color, dim ? 0.35 : 1);
        }

        // A polyline, skipping points under 1.5 px apart: zoomed out, the whole network is tens of
        // thousands of points
        private void line(GraphicsContext g, List<RouteShapes.Point> pts, Color color, double width, double alpha, double... dashes) {
            if (pts.size() < 2) return;
            g.setStroke(color);
            g.setLineWidth(width);
            g.setGlobalAlpha(alpha);
            g.setLineDashes(dashes.length == 0 ? null : dashes);
            g.beginPath();
            double lx = 0, ly = 0;
            for (int k = 0; k < pts.size(); k++) {
                RouteShapes.Point p = pts.get(k);
                double x = mercX(p.lng()) * scale + ox, y = mercY(p.lat()) * scale + oy;
                if (k == 0) g.moveTo(x, y);
                else if (k == pts.size() - 1 || Math.abs(x - lx) + Math.abs(y - ly) > 1.5) g.lineTo(x, y);
                else continue;
                lx = x;
                ly = y;
            }
            g.stroke();
            g.setLineDashes(null);
            g.setGlobalAlpha(1);
        }

        private void dot(GraphicsContext g, double lat, double lng, double r, Color ring, double ringWidth, Color fill, double alpha) {
            Point2D p = toScreen(lat, lng);
            g.setGlobalAlpha(alpha);
            g.setFill(fill);
            g.fillOval(p.getX() - r, p.getY() - r, 2 * r, 2 * r);
            g.setStroke(ring);
            g.setLineWidth(ringWidth);
            g.strokeOval(p.getX() - r, p.getY() - r, 2 * r, 2 * r);
            g.setGlobalAlpha(1);
        }

        // A dark tag with a lime edge, above or below a marker of radius r
        private void label(GraphicsContext g, String text, double lat, double lng, double r, boolean above) {
            Point2D p = toScreen(lat, lng);
            Font font = Theme.bold(11);
            double tw = width(text, font), bw = tw + 12, bh = 20;
            double x = p.getX() - bw / 2, y = above ? p.getY() - r - 6 - bh : p.getY() + r + 6;
            g.setFill(INK);
            g.fillRoundRect(x, y, bw, bh, 8, 8);
            g.setStroke(LIME);
            g.setLineWidth(1);
            g.strokeRoundRect(x, y, bw, bh, 8, 8);
            g.setFill(Color.WHITE);
            g.setFont(font);
            g.setTextBaseline(VPos.CENTER);
            g.fillText(text, x + 6, y + bh / 2);
        }

        private double width(String text, Font font) {
            Text t = new Text(text);
            t.setFont(font);
            return t.getLayoutBounds().getWidth();
        }
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
}
