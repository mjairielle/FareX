package FareX;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Set;

/**
 * The transit network as a weighted directed graph.
 *
 * KEY DESIGN POINT - a node is NOT a stop. A node is a (stop, route) pair.
 *
 * Alabang on Route 12 and Alabang on Route 07 are two different nodes,
 * joined by a TRANSFER edge. Without this split, the algorithm would think
 * you could switch routes for free, and every result would be nonsense.
 *
 * Node key format: "STOP_ID@ROUTE_ID"
 */
public class RouteGraph {

    public enum EdgeType { RIDE, TRANSFER }

    /**
     * How many minutes it costs to change vehicles at the same stop.
     * The single source of truth: the TIME search uses it, and every
     * TripOption built here is handed the same value for display.
     */
    private double transferMinutes = TripOption.DEFAULT_TRANSFER_MINUTES;

    private final Map<String, List<Edge>> adjacency = new HashMap<>();
    private final Map<String, Stop> stopsById = new HashMap<>();
    private final Map<String, Route> routesById = new HashMap<>();

    /** stopID -> every route ID that serves it. Drives transfer edge creation. */
    private final Map<String, Set<String>> routesAtStop = new HashMap<>();

    // ---------------------------------------------------------------
    // Node key helpers
    // ---------------------------------------------------------------

    public static String nodeKey(String stopID, String routeID) {
        return stopID + "@" + routeID;
    }

    public static String nodeKey(Stop stop, Route route) {
        return nodeKey(stop.getStopID(), route.getRouteID());
    }

    public static String stopIdOf(String nodeKey) {
        return nodeKey.substring(0, nodeKey.indexOf('@'));
    }

    public static String routeIdOf(String nodeKey) {
        return nodeKey.substring(nodeKey.indexOf('@') + 1);
    }

    // ---------------------------------------------------------------
    // Building
    // ---------------------------------------------------------------

    /**
     * Build the whole graph from the loaded routes.
     * Safe to call again - it clears first.
     */
    public void build(List<Route> routes) {
        adjacency.clear();
        stopsById.clear();
        routesById.clear();
        routesAtStop.clear();

        for (Route route : routes) {
            if (!route.isActive()) continue;   // disabled routes are invisible to the planner
            indexRoute(route);
        }

        for (Route route : routes) {
            if (!route.isActive()) continue;
            addRideEdges(route);
        }

        addTransferEdges();
    }

    /** Record the route and its stops so lookups work. */
    private void indexRoute(Route route) {
        routesById.put(route.getRouteID(), route);

        for (Stop stop : route.getStopsInOrder()) {
            stopsById.put(stop.getStopID(), stop);

            routesAtStop
                .computeIfAbsent(stop.getStopID(), k -> new LinkedHashSet<>())
                .add(route.getRouteID());

            // Every (stop, route) pair is a node, even if it ends up with no edges.
            adjacency.computeIfAbsent(nodeKey(stop, route), k -> new ArrayList<>());
        }
    }

    /**
     * Connect consecutive stops along a route, in both directions.
     *
     * Both directions is a simplification - plenty of real jeepney routes
     * are one-way loops. If you model those later, build only forward edges
     * for routes flagged as loops.
     */
    private void addRideEdges(Route route) {
        List<Stop> stops = route.getStopsInOrder();

        for (int i = 0; i < stops.size() - 1; i++) {
            Stop a = stops.get(i);
            Stop b = stops.get(i + 1);

            double km = a.distanceKmTo(b);
            double minutes = route.minutesFor(km);

            // Informational only: the marginal per-km cost of this segment.
            // The FARE search does NOT sum these - a real fare is charged per
            // boarding and is not additive per segment. See cheapestByFare().
            double segmentFare = route.getPerKmRate() * km;

            addEdge(new Edge(
                nodeKey(a, route), nodeKey(b, route),
                route.getRouteID(), EdgeType.RIDE, km, minutes, segmentFare));

            addEdge(new Edge(
                nodeKey(b, route), nodeKey(a, route),
                route.getRouteID(), EdgeType.RIDE, km, minutes, segmentFare));
        }
    }

    /**
     * At every stop served by more than one route, connect each route's node
     * to every other route's node at that same stop.
     *
     * The transfer edge records the new route's base fare, because on jeeps
     * and buses a transfer means paying a whole new fare. The FARE search
     * prices whole legs exactly instead (see cheapestByFare), so this number
     * is for inspection; the TIME search uses the edge's transferMinutes.
     */
    private void addTransferEdges() {
        for (Map.Entry<String, Set<String>> entry : routesAtStop.entrySet()) {
            String stopID = entry.getKey();
            List<String> routeIDs = new ArrayList<>(entry.getValue());

            if (routeIDs.size() < 2) continue;

            for (String fromRouteID : routeIDs) {
                for (String toRouteID : routeIDs) {
                    if (fromRouteID.equals(toRouteID)) continue;

                    Route toRoute = routesById.get(toRouteID);
                    double boardingFare = (toRoute == null) ? 0.0 : toRoute.getBaseFare();

                    addEdge(new Edge(
                        nodeKey(stopID, fromRouteID),
                        nodeKey(stopID, toRouteID),
                        toRouteID, EdgeType.TRANSFER,
                        0.0, transferMinutes, boardingFare));
                }
            }
        }
    }

    private void addEdge(Edge edge) {
        adjacency.computeIfAbsent(edge.getFromNode(), k -> new ArrayList<>()).add(edge);
    }

    // ---------------------------------------------------------------
    // Queries - this is what the search algorithm will call
    // ---------------------------------------------------------------

    /** All outgoing edges from a node. Never null. */
    public List<Edge> neighbors(String nodeKey) {
        List<Edge> edges = adjacency.get(nodeKey);
        return edges == null ? Collections.emptyList() : Collections.unmodifiableList(edges);
    }

    /** Every node sitting at a given stop - i.e. every route you could board there. */
    public List<String> nodesAtStop(String stopID) {
        Set<String> routeIDs = routesAtStop.get(stopID);
        if (routeIDs == null) return Collections.emptyList();

        List<String> nodes = new ArrayList<>();
        for (String routeID : routeIDs) {
            nodes.add(nodeKey(stopID, routeID));
        }
        return nodes;
    }

    public boolean hasStop(String stopID)       { return stopsById.containsKey(stopID); }
    public Stop getStop(String stopID)          { return stopsById.get(stopID); }
    public Route getRoute(String routeID)       { return routesById.get(routeID); }
    public Collection<Stop> getAllStops()       { return Collections.unmodifiableCollection(stopsById.values()); }
    public Collection<Route> getAllRoutes()     { return Collections.unmodifiableCollection(routesById.values()); }
    public int nodeCount()                      { return adjacency.size(); }

    public int edgeCount() {
        int total = 0;
        for (List<Edge> edges : adjacency.values()) total += edges.size();
        return total;
    }

    public double getTransferMinutes()              { return transferMinutes; }
    public void setTransferMinutes(double minutes)  { this.transferMinutes = minutes; }

    /** Find stops within walking distance of a given stop. */
    public List<Stop> stopsWithinWalk(Stop from, double maxKm) {
        List<Stop> near = new ArrayList<>();
        for (Stop stop : stopsById.values()) {
            if (stop.getStopID().equals(from.getStopID())) continue;
            if (from.straightLineKmTo(stop) <= maxKm) near.add(stop);
        }
        return near;
    }

    /** Quick sanity print. Run this after build() before you trust anything. */
    public String describe() {
        return String.format(
            "RouteGraph: %d routes, %d stops, %d nodes, %d edges",
            routesById.size(), stopsById.size(), nodeCount(), edgeCount());
    }

    // ---------------------------------------------------------------
    // Pathfinding
    // ---------------------------------------------------------------

    /** What the search is minimising on a given pass. */
    private enum Weight { FARE, TIME, TRANSFERS }

    /**
     * Find candidate journeys from a to b.
     *
     * Runs Dijkstra three times - once minimising fare, once time, once
     * transfer count - and returns the distinct winners. Running it per
     * objective is what gives the commuter genuinely different choices
     * instead of three near-identical routes. TripPlanner then ranks them.
     *
     * Returns an empty list if no route exists. Never null.
     */
    public List<TripOption> findPaths(Stop a, Stop b) {
        List<TripOption> found = new ArrayList<>();
        if (a == null || b == null) return found;

        Set<String> seen = new HashSet<>();
        for (Weight mode : Weight.values()) {
            TripOption option = (mode == Weight.FARE)
                    ? cheapestByFare(a, b)
                    : dijkstra(a, b, mode);
            if (option != null && seen.add(option.signature())) {
                found.add(option);
            }
        }
        return found;
    }

    /**
     * Exact cheapest journey, using the real per-boarding fare formula.
     *
     * WHY THIS IS SEPARATE - understand this before you defend the project.
     *
     * A fare is base + perKm*(km - baseKm), rounded, charged once per
     * BOARDING. That is not additive per segment: the cost of one more
     * segment depends on how far you have already ridden on that vehicle,
     * so summing per-segment edge weights cannot reproduce it. (Charging
     * perKm per segment plus base per boarding over-charges each leg by
     * perKm * min(km, baseKm), which differs between a jeep and a bus, so
     * it can rank the wrong trip first.)
     *
     * The fix is to make one Dijkstra step a whole LEG instead of a segment.
     * Each leg's fare depends only on where you board and alight, so it is
     * computed exactly with Route.fareFor(), and a journey's fare is just
     * the sum of its legs - additive again, so Dijkstra is exact.
     *
     * State is still a (stop, route) node: "S04@J01" here means "standing at
     * S04, having just got off J01". From there you may board any OTHER
     * route serving S04 - re-boarding the route you just left would be the
     * same ride paid twice. The origin is the virtual node "S01@" (no route).
     *
     * Cost per pop: every stop on every route at that stop. Still tiny.
     */
    private TripOption cheapestByFare(Stop origin, Stop destination) {
        String originID = origin.getStopID();
        String destID = destination.getStopID();

        if (originID.equals(destID)) return null;
        if (!hasStop(originID) || !hasStop(destID)) return null;

        String start = nodeKey(originID, "");

        Map<String, Double> dist = new HashMap<>();
        Map<String, String> boardedFrom = new HashMap<>();   // node -> node the leg started at
        Set<String> settled = new HashSet<>();
        PriorityQueue<QEntry> queue =
            new PriorityQueue<>(Comparator.comparingDouble(e -> e.cost));

        dist.put(start, 0.0);
        queue.add(new QEntry(start, 0.0));

        while (!queue.isEmpty()) {
            QEntry current = queue.poll();
            if (!settled.add(current.node)) continue;

            String hereID = stopIdOf(current.node);
            if (!current.node.equals(start) && hereID.equals(destID)) {
                return reconstructLegs(current.node, start, boardedFrom);
            }

            Stop here = stopsById.get(hereID);
            String cameOn = routeIdOf(current.node);

            for (String routeID : routesAtStop.getOrDefault(hereID, Collections.emptySet())) {
                if (routeID.equals(cameOn)) continue;
                Route route = routesById.get(routeID);
                if (route == null) continue;

                for (Stop alight : route.getStopsInOrder()) {
                    if (alight.getStopID().equals(hereID)) continue;

                    String next = nodeKey(alight, route);
                    if (settled.contains(next)) continue;

                    double legFare = route.fareFor(route.getDistance(here, alight));
                    double candidate = current.cost + legFare;

                    if (candidate < dist.getOrDefault(next, Double.POSITIVE_INFINITY)) {
                        dist.put(next, candidate);
                        boardedFrom.put(next, current.node);
                        queue.add(new QEntry(next, candidate));
                    }
                }
            }
        }

        return null;   // destination unreachable
    }

    /** Walk the leg chain back to the virtual origin node and build the trip. */
    private TripOption reconstructLegs(String goalNode, String start, Map<String, String> boardedFrom) {
        LinkedList<Leg> legs = new LinkedList<>();

        String cursor = goalNode;
        while (!cursor.equals(start)) {
            String from = boardedFrom.get(cursor);
            if (from == null) return null;

            Route route = routesById.get(routeIdOf(cursor));
            legs.addFirst(new Leg(route,
                    stopsById.get(stopIdOf(from)),
                    stopsById.get(stopIdOf(cursor))));
            cursor = from;
        }

        return legs.isEmpty() ? null : new TripOption(legs, transferMinutes);
    }

    /**
     * Standard Dijkstra over (stop, route) nodes, for the objectives that
     * ARE additive per segment: time and transfer count.
     * Returns the single best journey for one objective, or null if
     * the destination cannot be reached.
     */
    private TripOption dijkstra(Stop origin, Stop destination, Weight mode) {
        String originID = origin.getStopID();
        String destID = destination.getStopID();

        if (originID.equals(destID)) return null;
        if (!hasStop(originID) || !hasStop(destID)) return null;

        Map<String, Double> dist = new HashMap<>();
        Map<String, Edge> prevEdge = new HashMap<>();
        Set<String> settled = new HashSet<>();

        // NOTE: entries carry their own cost rather than the queue reading
        // dist on the fly. A PriorityQueue does NOT re-sort when a value in
        // an external map changes, so comparing against a live map silently
        // corrupts the heap. Instead we push a new entry on every improvement
        // and skip stale ones with the settled set. This is the standard
        // "lazy deletion" Dijkstra.
        PriorityQueue<QEntry> queue =
            new PriorityQueue<>(Comparator.comparingDouble(e -> e.cost));

        // Seed: you can board any route that serves the origin stop.
        // Boarding costs no time, and the first boarding is not a transfer.
        for (String node : nodesAtStop(originID)) {
            if (!routesById.containsKey(routeIdOf(node))) continue;
            dist.put(node, 0.0);
            queue.add(new QEntry(node, 0.0));
        }

        while (!queue.isEmpty()) {
            QEntry current = queue.poll();

            if (!settled.add(current.node)) continue;   // already finalised

            // Popping a node is Dijkstra's guarantee that its cost is final,
            // so the first time we pop anything at the destination stop,
            // that is the optimal answer for this objective.
            if (stopIdOf(current.node).equals(destID)) {
                return reconstruct(current.node, prevEdge);
            }

            for (Edge edge : neighbors(current.node)) {
                String next = edge.getToNode();
                if (settled.contains(next)) continue;

                double candidate = current.cost + weightOf(edge, mode);
                double known = dist.getOrDefault(next, Double.POSITIVE_INFINITY);

                if (candidate < known) {
                    dist.put(next, candidate);
                    prevEdge.put(next, edge);
                    queue.add(new QEntry(next, candidate));
                }
            }
        }

        return null;   // destination unreachable
    }

    /** Cost of a single edge under the chosen objective. FARE never comes here. */
    private double weightOf(Edge edge, Weight mode) {
        if (mode == Weight.TRANSFERS) {
            // A large constant per transfer makes the search avoid them
            // unless there is no alternative. The tiny distance term is
            // only a tie-breaker, so among routes with equal transfers
            // it still picks the shorter one.
            return edge.isTransfer() ? 1000.0 : edge.getKm() * 0.001;
        }
        return edge.getMinutes();   // TIME; transfer edges carry transferMinutes
    }

    /**
     * Walk the predecessor chain back from the goal, then group the edges
     * into Legs - consecutive RIDE edges on one route become a single leg,
     * and TRANSFER edges are where one leg ends and the next begins.
     */
    private TripOption reconstruct(String goalNode, Map<String, Edge> prevEdge) {
        LinkedList<Edge> path = new LinkedList<>();

        String cursor = goalNode;
        while (prevEdge.containsKey(cursor)) {
            Edge edge = prevEdge.get(cursor);
            path.addFirst(edge);
            cursor = edge.getFromNode();
        }

        if (path.isEmpty()) return null;

        List<Leg> legs = new ArrayList<>();
        String legStart = path.getFirst().getFromNode();

        for (Edge edge : path) {
            if (edge.isTransfer()) {
                appendLeg(legs, legStart, edge.getFromNode());
                legStart = edge.getToNode();
            }
        }
        appendLeg(legs, legStart, path.getLast().getToNode());

        return legs.isEmpty() ? null : new TripOption(legs, transferMinutes);
    }

    /** Build one Leg between two nodes on the same route. Skips empty legs. */
    private void appendLeg(List<Leg> legs, String fromNode, String toNode) {
        if (fromNode.equals(toNode)) return;   // transfer landed and left immediately

        Route route = routesById.get(routeIdOf(fromNode));
        Stop board = stopsById.get(stopIdOf(fromNode));
        Stop alight = stopsById.get(stopIdOf(toNode));

        if (route == null || board == null || alight == null) return;
        legs.add(new Leg(route, board, alight));
    }

    /** One entry in the priority queue. Immutable on purpose - see dijkstra(). */
    private static class QEntry {
        private final String node;
        private final double cost;

        QEntry(String node, double cost) {
            this.node = node;
            this.cost = cost;
        }
    }

    // ---------------------------------------------------------------

    /**
     * One weighted connection between two nodes.
     * Immutable - once the graph is built nothing should mutate an edge.
     */
    public static class Edge {

        private final String fromNode;
        private final String toNode;
        private final String routeID;
        private final EdgeType type;
        private final double km;
        private final double minutes;
        private final double fare;

        public Edge(String fromNode, String toNode, String routeID,
                    EdgeType type, double km, double minutes, double fare) {
            this.fromNode = fromNode;
            this.toNode = toNode;
            this.routeID = routeID;
            this.type = type;
            this.km = km;
            this.minutes = minutes;
            this.fare = fare;
        }

        public String getFromNode() { return fromNode; }
        public String getToNode()   { return toNode; }
        public String getRouteID()  { return routeID; }
        public EdgeType getType()   { return type; }
        public double getKm()       { return km; }
        public double getMinutes()  { return minutes; }
        public double getFare()     { return fare; }

        public boolean isTransfer() { return type == EdgeType.TRANSFER; }

        @Override
        public String toString() {
            return String.format("%s -> %s [%s %.2fkm %.1fmin PHP%.2f]",
                    fromNode, toNode, type, km, minutes, fare);
        }
    }
}
