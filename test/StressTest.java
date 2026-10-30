package FareX;

import java.util.*;

/** Throwaway. Randomised networks, checked against independent BFS reachability. */
public class StressTest {
    public static void main(String[] args) {
        Random rng = new Random(42);
        int networks = 300, queries = 0, mismatches = 0, invariantFails = 0;
        int fareChecks = 0, fareMisses = 0;
        long worstNs = 0;

        for (int n = 0; n < networks; n++) {
            int stopCount = 6 + rng.nextInt(25);
            List<Stop> stops = new ArrayList<>();
            for (int i = 0; i < stopCount; i++) {
                stops.add(new Stop("S" + i, "Stop" + i,
                        14.4 + rng.nextDouble() * 0.25, 120.95 + rng.nextDouble() * 0.15));
            }

            int routeCount = 2 + rng.nextInt(7);
            List<Route> routes = new ArrayList<>();
            for (int r = 0; r < routeCount; r++) {
                List<Stop> shuffled = new ArrayList<>(stops);
                Collections.shuffle(shuffled, rng);
                int len = 2 + rng.nextInt(Math.min(8, stopCount - 1));
                routes.add(new Route("R" + r, "Route" + r,
                        new ArrayList<>(shuffled.subList(0, len)),
                        15.0, 4.0, 1.5 + rng.nextDouble() * 2, 10 + rng.nextInt(35)));
            }

            RouteGraph g = new RouteGraph();
            g.build(routes);

            for (int q = 0; q < 12; q++) {
                Stop a = stops.get(rng.nextInt(stopCount));
                Stop b = stops.get(rng.nextInt(stopCount));
                if (a.getStopID().equals(b.getStopID())) continue;
                if (!g.hasStop(a.getStopID()) || !g.hasStop(b.getStopID())) continue;
                queries++;

                boolean bfsReachable = bfs(g, a.getStopID(), b.getStopID());

                long t0 = System.nanoTime();
                List<TripOption> found = g.findPaths(a, b);
                worstNs = Math.max(worstNs, System.nanoTime() - t0);

                if (bfsReachable != !found.isEmpty()) mismatches++;

                // Exact-fare check: no journey of up to BRUTE_MAX_LEGS legs may
                // be cheaper than the cheapest one the planner returned.
                if (!found.isEmpty()) {
                    double plannerBest = Double.POSITIVE_INFINITY;
                    for (TripOption o : found) plannerBest = Math.min(plannerBest, o.getTotalFare());
                    Set<String> visited = new HashSet<>();
                    visited.add(a.getStopID());
                    double bruteBest = bruteCheapest(routes, a, b.getStopID(), null, 0, visited);
                    fareChecks++;
                    if (bruteBest < plannerBest - 1e-9) fareMisses++;
                }

                for (TripOption o : found) {
                    List<Leg> legs = o.getLegs();
                    if (legs.isEmpty()) { invariantFails++; continue; }
                    if (!legs.get(0).getBoardStop().getStopID().equals(a.getStopID())) invariantFails++;
                    if (!legs.get(legs.size()-1).getAlightStop().getStopID().equals(b.getStopID())) invariantFails++;
                    if (o.getTransfers() != legs.size()-1) invariantFails++;
                    for (int i = 0; i < legs.size()-1; i++) {
                        if (!legs.get(i).getAlightStop().getStopID()
                                .equals(legs.get(i+1).getBoardStop().getStopID())) invariantFails++;
                        if (legs.get(i).getRoute().getRouteID()
                                .equals(legs.get(i+1).getRoute().getRouteID())) invariantFails++;
                    }
                }
            }
        }

        System.out.printf("networks=%d  queries=%d%n", networks, queries);
        System.out.printf("BFS-vs-Dijkstra reachability mismatches: %d%n", mismatches);
        System.out.printf("leg invariant violations: %d%n", invariantFails);
        System.out.printf("cheaper trip found by exhaustive search: %d of %d%n", fareMisses, fareChecks);
        System.out.printf("slowest findPaths call: %.2f ms%n", worstNs / 1_000_000.0);
        boolean ok = mismatches == 0 && invariantFails == 0 && fareMisses == 0;
        System.out.println(ok ? "STRESS PASS" : "STRESS FAIL");
        if (!ok) System.exit(1);
    }

    static final int BRUTE_MAX_LEGS = 4;

    /**
     * Cheapest fare from `here` to `goal` by trying every journey of up to
     * BRUTE_MAX_LEGS legs that never revisits a stop and never re-boards the
     * route it just left. Shares nothing with RouteGraph except Route.fareFor.
     */
    static double bruteCheapest(List<Route> routes, Stop here, String goal,
                                Route cameOn, int legs, Set<String> visited) {
        if (legs == BRUTE_MAX_LEGS) return Double.POSITIVE_INFINITY;
        double best = Double.POSITIVE_INFINITY;
        for (Route r : routes) {
            if (r == cameOn || !r.serves(here)) continue;
            for (Stop next : r.getStopsInOrder()) {
                if (visited.contains(next.getStopID())) continue;
                double fare = r.fareFor(r.getDistance(here, next));
                if (next.getStopID().equals(goal)) {
                    best = Math.min(best, fare);
                } else if (fare < best) {
                    visited.add(next.getStopID());
                    best = Math.min(best, fare + bruteCheapest(routes, next, goal, r, legs + 1, visited));
                    visited.remove(next.getStopID());
                }
            }
        }
        return best;
    }

    /** Independent reachability over stops, ignoring all weights. */
    static boolean bfs(RouteGraph g, String fromStop, String toStop) {
        Deque<String> queue = new ArrayDeque<>(g.nodesAtStop(fromStop));
        Set<String> seen = new HashSet<>(queue);
        while (!queue.isEmpty()) {
            String node = queue.poll();
            if (RouteGraph.stopIdOf(node).equals(toStop)) return true;
            for (RouteGraph.Edge e : g.neighbors(node)) {
                if (seen.add(e.getToNode())) queue.add(e.getToNode());
            }
        }
        return false;
    }
}
