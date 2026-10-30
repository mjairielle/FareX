package FareX;

import java.util.List;

/** Throwaway. */
public class LoaderTest {
    public static void main(String[] args) throws Exception {
        RouteLoader loader = new RouteLoader();
        List<Route> routes = loader.load("data/stops.csv", "data/routes.csv", "data/route_stops.csv");

        System.out.println("Loaded " + routes.size() + " routes, " + loader.getStops().size() + " stops");
        for (String w : loader.getWarnings()) System.out.println("  WARN: " + w);

        for (Route r : routes) {
            System.out.printf("  %s: %d stops, %.1f km end to end%n",
                    r.getName(), r.getStopsInOrder().size(), r.getTotalDistance());
        }

        RouteGraph graph = new RouteGraph();
        graph.build(routes);
        System.out.println(graph.describe());

        Stop from = loader.getStops().get("S01");
        Stop to   = loader.getStops().get("S07");
        TripPlanner planner = new TripPlanner(graph);

        System.out.println("\nZapote -> Ayala, all preferences:");
        for (TripOption o : planner.planAllPreferences(from, to)) {
            System.out.println(o.toDetailedString());
            System.out.println();
        }

        // sequence-order robustness: shuffled rows must still load in order
        System.out.println("J01 stop order: ");
        for (Route r : routes) if (r.getRouteID().equals("J01"))
            for (Stop s : r.getStopsInOrder()) System.out.println("   " + s);
    }
}
