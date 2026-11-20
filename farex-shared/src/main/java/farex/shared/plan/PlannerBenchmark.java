package farex.shared.plan;

import farex.shared.data.RouteBook;
import farex.shared.data.RoadDistances;
import farex.shared.fare.Fares;
import farex.shared.fare.RegularFare;
import java.nio.file.Path;
import java.util.*;

/**
 * Plain vs bidirectional Dijkstra on the real network, for the defense.
 *
 *   java -cp ... farex.shared.plan.PlannerBenchmark [dataDir] [pairs]     (defaults: data, 500)
 *
 * For random stop pairs it runs both searches (Fastest and Fewest transfers), checks they find
 * the same cost, and prints how many nodes each settled and how long each took.
 */
public final class PlannerBenchmark {
    public static void main(String[] args) {
        Path dir = Path.of(args.length > 0 ? args[0] : "data");
        int pairs = args.length > 1 ? Integer.parseInt(args[1]) : 500;
        RouteBook book = new RouteBook(dir);
        Fares.setRoadKm(RoadDistances.load(dir, book.routes()));

        long t0 = System.nanoTime();
        TripPlanner planner = new TripPlanner(book);
        System.out.printf("Network: %d routes, %d stops. Graph built in %.0f ms%n",
                book.routes().size(), planner.places().size(), (System.nanoTime() - t0) / 1e6);

        List<String> places = planner.places();
        Random rng = new Random(7);
        for (int warm = 0; warm < 200; warm++) {                  // let the JIT settle before timing
            String a = places.get(rng.nextInt(places.size())), b = places.get(rng.nextInt(places.size()));
            if (!a.equals(b)) { planner.compare(a, b, true, false); planner.compare(a, b, true, true); }
        }

        for (boolean fastest : new boolean[]{true, false}) {
            long plainNs = 0, biNs = 0, plainSettled = 0, biSettled = 0;
            int same = 0, reachable = 0, done = 0;
            rng = new Random(11);
            while (done < pairs) {
                String a = places.get(rng.nextInt(places.size())), b = places.get(rng.nextInt(places.size()));
                if (a.equals(b)) continue;
                long s = System.nanoTime();
                double[] plain = planner.compare(a, b, fastest, false);
                plainNs += System.nanoTime() - s;
                s = System.nanoTime();
                double[] bi = planner.compare(a, b, fastest, true);
                biNs += System.nanoTime() - s;
                plainSettled += (long) plain[1];
                biSettled += (long) bi[1];
                if (plain[0] == bi[0] || Math.abs(plain[0] - bi[0]) < 1e-9) same++;
                if (plain[0] != Double.MAX_VALUE) reachable++;
                done++;
            }
            System.out.printf("%n%s, %d random stop pairs (%d connected):%n", fastest ? "Fastest" : "Fewest transfers", pairs, reachable);
            System.out.printf("  same answer        %d of %d%n", same, pairs);
            System.out.printf("  plain Dijkstra     %7.2f ms/search, %6.0f nodes settled%n", plainNs / 1e6 / pairs, (double) plainSettled / pairs);
            System.out.printf("  bidirectional      %7.2f ms/search, %6.0f nodes settled (%.0f%% fewer)%n",
                    biNs / 1e6 / pairs, (double) biSettled / pairs, 100.0 * (plainSettled - biSettled) / plainSettled);
        }

        long s = System.nanoTime();
        int plans = 0;
        rng = new Random(3);
        for (int i = 0; i < 100; i++) {
            String a = places.get(rng.nextInt(places.size())), b = places.get(rng.nextInt(places.size()));
            if (!a.equals(b)) { planner.plan(a, b, new RegularFare()); plans++; }
        }
        System.out.printf("%nFull plan (all three rankings incl. exact-fare Cheapest): %.1f ms average over %d trips%n",
                (System.nanoTime() - s) / 1e6 / plans, plans);
    }
}
