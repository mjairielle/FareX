package farex.shared.plan;

import farex.shared.data.RouteBook;
import farex.shared.data.RouteStop;
import farex.shared.fare.FareRule;
import farex.shared.fare.Fares;
import java.util.*;

/**
 * Best trips from A to B: up to three plans (fastest, fewest transfers, cheapest), deduped.
 * A and B are stops, or any point: the trip then starts and ends with a walk to/from a stop.
 *
 * The network is one graph, built once:
 *   ride node     (route, stop index)  "on this jeep at this stop"
 *   stop node     stop                 "standing at this stop" (got off here, or starting here)
 *   walked node   stop                 "walked to this stop": you must board next
 * Edges: ride to the next stop (forward only on one-way routes), get off (ride -> stop),
 * get on (stop or walked -> ride), and walk to a stop up to TRANSFER_WALK_M away (stop -> walked).
 * The walked layer is what stops walks chaining: at most one walk between two rides, plus one
 * walk at each end of the trip, so a trip is never "walk the whole way, stop to stop".
 *
 * Fastest and Fewest transfers are additive per edge, so they use bidirectional Dijkstra:
 * one search from the start and one backwards from the destination until they meet. Same
 * answer as plain Dijkstra, fewer nodes looked at; plain Dijkstra is kept to prove that
 * (see TripPlannerNetworkTest and PlannerBenchmark).
 *
 * Cheapest is not additive per stop (a fare is charged per boarding, with a base distance),
 * so it searches whole rides, each priced with Fares.fare. Walking costs P1 per km there,
 * only so that of two equal fares the one with less walking wins.
 */
public final class TripPlanner {
    public static final String WALK = "WALK";
    public static final double JEEP_KPH = 15.0;
    public static final double WALK_KPH = 4.5;
    public static final double WALK_DETOUR = 1.25;          // walking distance / straight line
    public static final double TRANSFER_WALK_M = 250;       // longest walk between two stops to change jeeps
    public static final double ACCESS_WALK_M = 800;         // longest walk from a start point / to a destination
    public static final int TRANSFER_MINUTES = 5;

    /** One part of a trip: a ride on `route`, or a walk when route is WALK. km = distance covered. */
    public record Leg(String route, String from, String to, double km) {
        public boolean walk() { return WALK.equals(route); }
    }
    public record Plan(String label, List<Leg> legs, double km, int transfers, long fareCents, int minutes) {}

    /** What one search minimises; per km ridden, per km walked, per boarding. */
    enum Mode {
        // Minutes, the same as Plan.minutes: 4 min per km by jeep (15 km/h), 13.3 per km on foot
        // (4.5 km/h), 5 per boarding. Time, not distance, is what a passenger compares (as Google
        // Maps does), and it stops "hop off, walk across, pay again" to save a few hundred metres.
        FASTEST("Fastest", 60 / JEEP_KPH, 60 / WALK_KPH, TRANSFER_MINUTES),
        FEWEST_TRANSFERS("Fewest transfers", 1, 1, 1000);
        final String label; final double perKm, perWalkKm, board;
        Mode(String label, double perKm, double perWalkKm, double board) {
            this.label = label; this.perKm = perKm; this.perWalkKm = perWalkKm; this.board = board;
        }
    }

    // ----- the network -----
    private final RouteBook book;
    private final List<String> routeIds = new ArrayList<>();
    private final List<List<RouteStop>> lines = new ArrayList<>();
    private final List<double[]> lineKm = new ArrayList<>();   // km from the route's first stop
    private final List<int[]> routeNodes = new ArrayList<>();  // route -> ride node per stop index
    private final boolean[] oneway;

    private final List<String> placeNames = new ArrayList<>();
    private final Map<String, Integer> placeIds = new HashMap<>();
    private final List<RouteStop> placeAt = new ArrayList<>();
    private final List<List<Integer>> placeRideNodes = new ArrayList<>();
    private final List<int[]> walkTo = new ArrayList<>();       // place -> nearby places
    private final List<double[]> walkKm = new ArrayList<>();    // and the walking km to each

    private final int rideCount, placeCount, source, target, nodeCount;
    private final int[] nodeRoute, nodeIdx, nodePlace;

    public TripPlanner(RouteBook book) {
        this.book = book;
        for (var e : book.routes().entrySet()) {
            routeIds.add(e.getKey());
            lines.add(e.getValue());
            double[] km = new double[e.getValue().size()];
            for (int i = 1; i < km.length; i++) km[i] = km[i - 1] + Fares.pathKm(e.getValue(), i - 1, i);
            lineKm.add(km);
        }
        oneway = new boolean[routeIds.size()];
        for (int r = 0; r < routeIds.size(); r++) oneway[r] = book.isOneway(routeIds.get(r));

        int rides = 0;
        for (List<RouteStop> l : lines) rides += l.size();
        rideCount = rides;
        nodeRoute = new int[rides];
        nodeIdx = new int[rides];
        nodePlace = new int[rides];
        int n = 0;
        for (int r = 0; r < lines.size(); r++) {
            int[] ids = new int[lines.get(r).size()];
            for (int i = 0; i < ids.length; i++) {
                RouteStop s = lines.get(r).get(i);
                int p = placeIds.computeIfAbsent(s.name(), k -> {
                    placeNames.add(k);
                    placeAt.add(s);
                    placeRideNodes.add(new ArrayList<>());
                    return placeNames.size() - 1;
                });
                nodeRoute[n] = r;
                nodeIdx[n] = i;
                nodePlace[n] = p;
                placeRideNodes.get(p).add(n);
                ids[i] = n++;
            }
            routeNodes.add(ids);
        }
        placeCount = placeNames.size();
        // node ids: ride nodes, then stop nodes, then walked nodes, then source and target
        source = rideCount + 2 * placeCount;
        target = source + 1;
        nodeCount = target + 1;
        buildWalks();
    }

    /** Places within TRANSFER_WALK_M of each other, found with a ~250 m grid. */
    private void buildWalks() {
        Map<Long, List<Integer>> grid = new HashMap<>();
        for (int p = 0; p < placeCount; p++) grid.computeIfAbsent(cell(placeAt.get(p)), k -> new ArrayList<>()).add(p);
        for (int p = 0; p < placeCount; p++) {
            List<Integer> near = new ArrayList<>();
            List<Double> kms = new ArrayList<>();
            long c = cell(placeAt.get(p));
            for (long dy = -1; dy <= 1; dy++) {
                for (long dx = -1; dx <= 1; dx++) {
                    for (int q : grid.getOrDefault(c + dy * 1_000_000L + dx, List.of())) {
                        if (q == p) continue;
                        double m = Fares.haversineKm(placeAt.get(p), placeAt.get(q)) * 1000;
                        if (m <= TRANSFER_WALK_M) { near.add(q); kms.add(m / 1000 * WALK_DETOUR); }
                    }
                }
            }
            walkTo.add(near.stream().mapToInt(Integer::intValue).toArray());
            walkKm.add(kms.stream().mapToDouble(Double::doubleValue).toArray());
        }
    }

    private static long cell(RouteStop s) {
        return (long) Math.floor(s.lat() / 0.0025) * 1_000_000L + (long) Math.floor(s.lng() / 0.0025);
    }

    // ===================================================================== planning

    /** Between two named stops. At most 3 plans; one that wins several rankings appears once with joined labels. */
    public List<Plan> plan(String from, String to, FareRule rule) {
        if (from.equals(to)) throw new IllegalArgumentException("Pick two different places");
        Integer a = placeIds.get(from), b = placeIds.get(to);
        if (a == null || b == null) throw new IllegalArgumentException("Unknown place");
        return plan(new Query(Map.of(a, 0.0), Map.of(b, 0.0), from, to), rule);
    }

    /**
     * Between any two points, e.g. a pin on the map. The trip starts with a walk to a stop within
     * ACCESS_WALK_M and ends with one from a stop. Empty if no trip connects them.
     */
    public List<Plan> plan(double fromLat, double fromLng, String fromLabel,
                           double toLat, double toLng, String toLabel, FareRule rule) {
        Map<Integer, Double> starts = stopsNear(fromLat, fromLng);
        Map<Integer, Double> ends = stopsNear(toLat, toLng);
        if (starts.isEmpty()) throw new IllegalArgumentException("No jeepney stop within " + (int) ACCESS_WALK_M + " m of the start");
        if (ends.isEmpty()) throw new IllegalArgumentException("No jeepney stop within " + (int) ACCESS_WALK_M + " m of the destination");
        return plan(new Query(starts, ends, fromLabel, toLabel), rule);
    }

    /** Stops within ACCESS_WALK_M of a point, with the walking km to each. */
    public Map<Integer, Double> stopsNear(double lat, double lng) {
        Map<Integer, Double> out = new HashMap<>();
        RouteStop here = new RouteStop("", lat, lng);
        for (int p = 0; p < placeCount; p++) {
            double m = Fares.haversineKm(here, placeAt.get(p)) * 1000;
            if (m <= ACCESS_WALK_M) out.put(p, m / 1000 * WALK_DETOUR);
        }
        return out;
    }

    /** The trip's ends: places (with the walking km to/from them) and what to call the ends. */
    record Query(Map<Integer, Double> starts, Map<Integer, Double> ends, String fromLabel, String toLabel) {}

    private List<Plan> plan(Query q, FareRule rule) {
        Map<String, Plan> bySignature = new LinkedHashMap<>();
        for (Mode m : Mode.values()) add(bySignature, m.label, legs(q, bidirectional(q, m)), rule);
        add(bySignature, "Cheapest", cheapest(q, rule), rule);
        return new ArrayList<>(bySignature.values());
    }

    private void add(Map<String, Plan> bySignature, String label, List<Leg> legs, FareRule rule) {
        if (legs == null || legs.isEmpty()) return;
        String sig = legs.toString();
        Plan old = bySignature.get(sig);
        bySignature.put(sig, old == null ? build(label, legs, rule)
                : new Plan(old.label() + " + " + label, old.legs(), old.km(), old.transfers(), old.fareCents(), old.minutes()));
    }

    private Plan build(String label, List<Leg> legs, FareRule rule) {
        double km = 0, rideKm = 0, walkedKm = 0;
        long fare = 0;
        int rides = 0;
        for (Leg l : legs) {
            km += l.km();
            if (l.walk()) { walkedKm += l.km(); continue; }
            rides++;
            rideKm += l.km();
            fare += Fares.fare(l.km(), rule);
        }
        int transfers = Math.max(0, rides - 1);
        int minutes = (int) Math.round(rideKm / JEEP_KPH * 60 + walkedKm / WALK_KPH * 60) + transfers * TRANSFER_MINUTES;
        return new Plan(label, legs, km, transfers, fare, minutes);
    }

    // ===================================================================== graph edges

    /** Receives one edge: to node v, costing w. */
    interface Edges { void edge(int v, double w); }

    private boolean isRide(int u) { return u < rideCount; }
    private boolean isWalked(int u) { return u >= rideCount + placeCount && u < source; }
    private int stopNode(int p) { return rideCount + p; }
    private int walkedNode(int p) { return rideCount + placeCount + p; }
    private int placeOf(int u) { return (u - rideCount) % placeCount; }
    private double rideKm(int r, int i, int j) { return Math.abs(lineKm.get(r)[j] - lineKm.get(r)[i]); }

    // Where the trip starts: at a stop itself (km 0) you may still walk once; after walking to it you must board
    private int startNode(int p, double km) { return km == 0 ? stopNode(p) : walkedNode(p); }

    // Getting on at stop p includes riding to the next stop (or the previous, two-way): a ride node
    // is always "has ridden", so you cannot get on and straight off again to reset the walk rule
    private void board(int p, Mode m, Edges e) {
        for (int v : placeRideNodes.get(p)) {
            int r = nodeRoute[v], i = nodeIdx[v];
            int[] ids = routeNodes.get(r);
            if (i + 1 < ids.length) e.edge(ids[i + 1], m.board + m.perKm * rideKm(r, i, i + 1) + rideKm(r, i, i + 1) * 1e-6);
            if (!oneway[r] && i > 0) e.edge(ids[i - 1], m.board + m.perKm * rideKm(r, i - 1, i) + rideKm(r, i - 1, i) * 1e-6);
        }
    }

    /** Edges leaving u. */
    private void out(int u, Mode m, Query q, Edges e) {
        if (u == source) { q.starts().forEach((p, km) -> e.edge(startNode(p, km), km * m.perWalkKm)); return; }
        if (u == target) return;
        if (isRide(u)) {
            int r = nodeRoute[u], i = nodeIdx[u];
            int[] ids = routeNodes.get(r);
            if (i + 1 < ids.length) e.edge(ids[i + 1], m.perKm * rideKm(r, i, i + 1) + rideKm(r, i, i + 1) * 1e-6);
            if (!oneway[r] && i > 0) e.edge(ids[i - 1], m.perKm * rideKm(r, i - 1, i) + rideKm(r, i - 1, i) * 1e-6);
            e.edge(stopNode(nodePlace[u]), 0);                       // get off
            return;
        }
        int p = placeOf(u);
        board(p, m, e);
        Double end = q.ends().get(p);
        if (isWalked(u)) {                                           // walked here: board, or this is the destination stop itself
            if (end != null && end == 0) e.edge(target, 0);
            return;
        }
        int[] near = walkTo.get(p);
        double[] km = walkKm.get(p);
        for (int k = 0; k < near.length; k++) e.edge(walkedNode(near[k]), km[k] * m.perWalkKm);
        if (end != null) e.edge(target, end * m.perWalkKm);
    }

    /** Edges arriving at u (the same edges, read backwards, for the search from the destination). */
    private void in(int u, Mode m, Query q, Edges e) {
        if (u == target) {
            q.ends().forEach((p, km) -> {
                e.edge(stopNode(p), km * m.perWalkKm);
                if (km == 0) e.edge(walkedNode(p), 0);
            });
            return;
        }
        if (u == source) return;
        if (isRide(u)) {
            int r = nodeRoute[u], i = nodeIdx[u];
            int[] ids = routeNodes.get(r);
            // riding on from the stop before (or after, two-way), or having boarded there
            if (i > 0) {
                double w = m.perKm * rideKm(r, i - 1, i) + rideKm(r, i - 1, i) * 1e-6;
                e.edge(ids[i - 1], w);
                e.edge(stopNode(nodePlace[ids[i - 1]]), m.board + w);
                e.edge(walkedNode(nodePlace[ids[i - 1]]), m.board + w);
            }
            if (!oneway[r] && i + 1 < ids.length) {
                double w = m.perKm * rideKm(r, i, i + 1) + rideKm(r, i, i + 1) * 1e-6;
                e.edge(ids[i + 1], w);
                e.edge(stopNode(nodePlace[ids[i + 1]]), m.board + w);
                e.edge(walkedNode(nodePlace[ids[i + 1]]), m.board + w);
            }
            return;
        }
        int p = placeOf(u);
        Double start = q.starts().get(p);
        if (isWalked(u)) {                                           // reached on foot: from a nearby stop, or from the start
            int[] near = walkTo.get(p);
            double[] km = walkKm.get(p);
            for (int k = 0; k < near.length; k++) e.edge(stopNode(near[k]), km[k] * m.perWalkKm);
            if (start != null && start > 0) e.edge(source, start * m.perWalkKm);
            return;
        }
        for (int v : placeRideNodes.get(p)) e.edge(v, 0);            // got off here
        if (start != null && start == 0) e.edge(source, 0);
    }

    // ===================================================================== searches

    /** A search result: the node path from source to target (null if none), its cost, and how many nodes were settled. */
    record Result(List<Integer> path, double cost, int settled) {}

    /** Plain Dijkstra from source to target. */
    Result dijkstra(Query q, Mode m) {
        double[] dist = new double[nodeCount];
        int[] pred = new int[nodeCount];
        Arrays.fill(dist, Double.MAX_VALUE);
        Arrays.fill(pred, -1);
        boolean[] done = new boolean[nodeCount];
        PriorityQueue<double[]> pq = new PriorityQueue<>(Comparator.comparingDouble(a -> a[0]));
        dist[source] = 0;
        pq.add(new double[]{0, source});
        int settled = 0;
        while (!pq.isEmpty()) {
            double[] top = pq.poll();
            int u = (int) top[1];
            if (done[u]) continue;
            done[u] = true;
            settled++;
            if (u == target) break;
            out(u, m, q, (v, w) -> {
                if (dist[u] + w < dist[v]) { dist[v] = dist[u] + w; pred[v] = u; pq.add(new double[]{dist[v], v}); }
            });
        }
        if (dist[target] == Double.MAX_VALUE) return new Result(null, Double.MAX_VALUE, settled);
        LinkedList<Integer> path = new LinkedList<>();
        for (int v = target; v != -1; v = pred[v]) path.addFirst(v);
        return new Result(path, dist[target], settled);
    }

    /**
     * Bidirectional Dijkstra: a forward search from the source and a backward one from the target
     * (over edges read backwards), always advancing the side whose queue is cheaper. Every time an
     * edge reaches a node the other side has already reached, that s-t path is a candidate (mu).
     * It is safe to stop once the two queue tops together cost at least mu: no unexplored path can
     * be cheaper. The path is the forward half up to the meeting node plus the backward half after it.
     */
    Result bidirectional(Query q, Mode m) {
        double[] df = new double[nodeCount], db = new double[nodeCount];
        int[] pf = new int[nodeCount], pb = new int[nodeCount];
        Arrays.fill(df, Double.MAX_VALUE);
        Arrays.fill(db, Double.MAX_VALUE);
        Arrays.fill(pf, -1);
        Arrays.fill(pb, -1);
        boolean[] doneF = new boolean[nodeCount], doneB = new boolean[nodeCount];
        PriorityQueue<double[]> qf = new PriorityQueue<>(Comparator.comparingDouble(a -> a[0]));
        PriorityQueue<double[]> qb = new PriorityQueue<>(Comparator.comparingDouble(a -> a[0]));
        df[source] = 0;
        db[target] = 0;
        qf.add(new double[]{0, source});
        qb.add(new double[]{0, target});
        double[] mu = {Double.MAX_VALUE};
        int[] meet = {-1};
        int settled = 0;

        while (!qf.isEmpty() && !qb.isEmpty()) {
            if (qf.peek()[0] + qb.peek()[0] >= mu[0]) break;
            boolean forward = qf.peek()[0] <= qb.peek()[0];
            PriorityQueue<double[]> pq = forward ? qf : qb;
            double[] dist = forward ? df : db, other = forward ? db : df;
            int[] pred = forward ? pf : pb;
            boolean[] done = forward ? doneF : doneB;

            int u = (int) pq.poll()[1];
            if (done[u]) continue;
            done[u] = true;
            settled++;
            Edges relax = (v, w) -> {
                if (dist[u] + w < dist[v]) { dist[v] = dist[u] + w; pred[v] = u; pq.add(new double[]{dist[v], v}); }
                if (other[v] != Double.MAX_VALUE && dist[u] + w + other[v] < mu[0]) { mu[0] = dist[u] + w + other[v]; meet[0] = v; }
            };
            if (forward) out(u, m, q, relax); else in(u, m, q, relax);
        }
        if (meet[0] < 0) return new Result(null, Double.MAX_VALUE, settled);
        LinkedList<Integer> path = new LinkedList<>();
        for (int v = meet[0]; v != -1; v = pf[v]) path.addFirst(v);
        for (int v = pb[meet[0]]; v != -1; v = pb[v]) path.addLast(v);
        return new Result(path, mu[0], settled);
    }

    /** Node path -> legs: rides on one route merged, walks between stops, and the walks at either end. */
    private List<Leg> legs(Query q, Result res) {
        if (res.path() == null) return null;
        List<Integer> path = res.path();
        List<Leg> legs = new ArrayList<>();
        int boardPlace = -1, boardIdx = -1;                  // where the current ride was boarded
        for (int k = 1; k < path.size(); k++) {
            int a = path.get(k - 1), b = path.get(k);
            if (a == source && b != target) {
                double km = q.starts().get(placeOf(b));
                if (km > 0) legs.add(new Leg(WALK, q.fromLabel(), placeNames.get(placeOf(b)), km));
            } else if (b == target && a != source) {
                double km = q.ends().get(placeOf(a));
                if (km > 0) legs.add(new Leg(WALK, placeNames.get(placeOf(a)), q.toLabel(), km));
            } else if (!isRide(a) && isRide(b)) {
                // boarding lands on the next stop; the boarding stop is b's neighbour at a's place
                int r = nodeRoute[b], i = nodeIdx[b];
                int[] ids = routeNodes.get(r);
                boardPlace = placeOf(a);
                boardIdx = i > 0 && nodePlace[ids[i - 1]] == boardPlace ? i - 1 : i + 1;
            } else if (isRide(a) && !isRide(b)) {
                int r = nodeRoute[a];
                legs.add(new Leg(routeIds.get(r), placeNames.get(boardPlace), placeNames.get(placeOf(b)),
                        rideKm(r, boardIdx, nodeIdx[a])));
            } else if (!isRide(a) && !isRide(b)) {
                int pa = placeOf(a), pb = placeOf(b);
                for (int k2 = 0; k2 < walkTo.get(pa).length; k2++) {
                    if (walkTo.get(pa)[k2] == pb) legs.add(new Leg(WALK, placeNames.get(pa), placeNames.get(pb), walkKm.get(pa)[k2]));
                }
            }
        }
        return legs;
    }

    /**
     * Cheapest trip, priced exactly. One step is a whole ride: board at this stop, get off at any
     * later stop of that route (any stop on a two-way route), costing Fares.fare of that distance.
     * Same walking rule as the graph: one walk between rides, one at each end (stop p is node p,
     * "walked to p" is node placeCount + p). Walking costs P1 per km here only so that of two
     * equal fares the one with less walking wins. Null when nothing connects.
     */
    private List<Leg> cheapest(Query q, FareRule rule) {
        final double walkCentsPerKm = 100;
        int n = 2 * placeCount + 2, src = 2 * placeCount, dst = src + 1;
        double[] dist = new double[n];
        int[] pred = new int[n];
        Leg[] via = new Leg[n];
        Arrays.fill(dist, Double.MAX_VALUE);
        Arrays.fill(pred, -1);
        boolean[] done = new boolean[n];
        PriorityQueue<double[]> pq = new PriorityQueue<>(Comparator.comparingDouble(a -> a[0]));
        dist[src] = 0;
        pq.add(new double[]{0, src});
        while (!pq.isEmpty()) {
            double[] top = pq.poll();
            int u = (int) top[1];
            if (done[u]) continue;
            done[u] = true;
            if (u == dst) break;
            if (u == src) {
                for (var s : q.starts().entrySet()) {
                    double km = s.getValue();
                    relax(dist, pred, via, pq, u, km == 0 ? s.getKey() : placeCount + s.getKey(), km * walkCentsPerKm,
                            km > 0 ? new Leg(WALK, q.fromLabel(), placeNames.get(s.getKey()), km) : null);
                }
                continue;
            }
            boolean walked = u >= placeCount;
            int p = walked ? u - placeCount : u;
            String here = placeNames.get(p);
            Double end = q.ends().get(p);
            if (end != null && (!walked || end == 0)) {
                relax(dist, pred, via, pq, u, dst, end * walkCentsPerKm, end > 0 ? new Leg(WALK, here, q.toLabel(), end) : null);
            }
            if (!walked) {
                for (int k = 0; k < walkTo.get(p).length; k++) {
                    int v = walkTo.get(p)[k];
                    double km = walkKm.get(p)[k];
                    relax(dist, pred, via, pq, u, placeCount + v, km * walkCentsPerKm, new Leg(WALK, here, placeNames.get(v), km));
                }
            }
            for (int node : placeRideNodes.get(p)) {
                int r = nodeRoute[node], i = nodeIdx[node];
                List<RouteStop> line = lines.get(r);
                for (int j = oneway[r] ? i + 1 : 0; j < line.size(); j++) {
                    if (j == i) continue;
                    double km = rideKm(r, i, j);
                    int v = placeIds.get(line.get(j).name());
                    relax(dist, pred, via, pq, u, v, Fares.fare(km, rule) + km * 1e-6,
                            new Leg(routeIds.get(r), here, line.get(j).name(), km));
                }
            }
        }
        if (dist[dst] == Double.MAX_VALUE) return null;
        LinkedList<Leg> legs = new LinkedList<>();
        for (int v = dst; v != src; v = pred[v]) if (via[v] != null) legs.addFirst(via[v]);
        return legs;
    }

    private static void relax(double[] dist, int[] pred, Leg[] via, PriorityQueue<double[]> pq,
                              int u, int v, double w, Leg leg) {
        if (dist[u] + w < dist[v]) {
            dist[v] = dist[u] + w;
            pred[v] = u;
            via[v] = leg;
            pq.add(new double[]{dist[v], v});
        }
    }

    // ===================================================================== for tests and the benchmark

    /** Cost of the best path between two named stops with plain or bidirectional Dijkstra, and nodes settled. */
    double[] compare(String from, String to, boolean fastest, boolean bidirectional) {
        Query q = new Query(Map.of(placeIds.get(from), 0.0), Map.of(placeIds.get(to), 0.0), from, to);
        Mode m = fastest ? Mode.FASTEST : Mode.FEWEST_TRANSFERS;
        Result r = bidirectional ? bidirectional(q, m) : dijkstra(q, m);
        return new double[]{r.cost(), r.settled()};
    }

    public List<String> places() { return new ArrayList<>(new TreeSet<>(placeNames)); }

    /** "Commonwealth–SM Fairview via Regalado (to SM City Fairview)" */
    public String routeName(String routeId) { return book.name(routeId); }
}
