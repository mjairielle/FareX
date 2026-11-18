package farex.shared.plan;

import farex.shared.data.RouteBook;
import farex.shared.data.RouteStop;
import farex.shared.fare.DiscountedFare;
import farex.shared.fare.FareRule;
import farex.shared.fare.Fares;
import farex.shared.fare.RegularFare;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

public class TripPlannerTest {

    @TempDir
    Path tempDir;

    private TripPlanner planner;

    @BeforeEach
    void setUp() {
        RouteBook book = new RouteBook(tempDir);
        planner = new TripPlanner(book);
    }

    @Test
    void testPlanBetweenCubaoAndFairview() {
        List<TripPlanner.Plan> plans = planner.plan("Cubao", "Fairview Terminal", new RegularFare());
        assertTrue(plans.size() >= 2 && plans.size() <= 3, "2 to 3 distinct plans returned");

        TripPlanner.Plan shortest = plans.stream().filter(p -> p.label().contains("Shortest")).findFirst().orElseThrow();
        TripPlanner.Plan fewest = plans.stream().filter(p -> p.label().contains("Fewest")).findFirst().orElseThrow();
        TripPlanner.Plan cheapest = plans.stream().filter(p -> p.label().contains("Cheapest")).findFirst().orElseThrow();

        assertTrue(plans.stream().allMatch(p -> p.km() >= shortest.km() - 1e-9));
        assertTrue(plans.stream().allMatch(p -> p.transfers() >= fewest.transfers()));
        assertTrue(plans.stream().allMatch(p -> p.fareCents() >= cheapest.fareCents()));
    }

    @Test
    void testDirectRideZeroTransfers() {
        List<TripPlanner.Plan> plans = planner.plan("Philcoa", "Fairview Terminal", new RegularFare());
        TripPlanner.Plan fewest = plans.stream().filter(p -> p.label().contains("Fewest")).findFirst().orElseThrow();
        assertEquals(0, fewest.transfers(), "Direct ride has 0 transfers");
    }

    @Test
    void testDiscountedFareLowerThanRegular() {
        long reg = planner.plan("Cubao", "Philcoa", new RegularFare()).get(0).fareCents();
        long dis = planner.plan("Cubao", "Philcoa", new DiscountedFare()).get(0).fareCents();
        assertTrue(dis < reg, "Discounted fare is lower than regular fare");
    }

    @Test
    void testCheapestUsesTheRealFareNotPerKmCost() throws Exception {
        // X and Y are 0.8 km apart. Route LOOP goes X -> far point -> Y (8.1 km, one ride):
        // P14.00 + 4.1 km x P2.00 = P22.25. Routes A and B hop X -> M -> Y (0.4 km each,
        // two rides): P14.00 + P14.00 = P28.00. LOOP is cheaper. A per-km search cost
        // (base per boarding + rate on every km) scored LOOP 30.2 vs hops 29.6 and picked the hops.
        Path dir = Files.createTempDirectory(tempDir, "loop");
        Files.writeString(dir.resolve("routes.csv"), String.join("\n",
                "routeId,seq,name,lat,lng",
                "LOOP,0,X,14.6000,121.0000", "LOOP,1,Far,14.6400,121.0000", "LOOP,2,Y,14.6072,121.0000",
                "A,0,X,14.6000,121.0000", "A,1,M,14.6036,121.0000",
                "B,0,M,14.6036,121.0000", "B,1,Y,14.6072,121.0000", ""));
        List<TripPlanner.Plan> plans = new TripPlanner(new RouteBook(dir)).plan("X", "Y", new RegularFare());
        TripPlanner.Plan cheapest = plans.stream().filter(p -> p.label().contains("Cheapest")).findFirst().orElseThrow();
        assertEquals(1, cheapest.legs().size(), "one ride on LOOP");
        assertEquals("LOOP", cheapest.legs().get(0).route());
        assertEquals(2225, cheapest.fareCents());
        assertTrue(plans.stream().allMatch(p -> p.fareCents() >= cheapest.fareCents()));
    }

    @Test
    void testCheapestMatchesExhaustiveSearchOnSeedRoutes() {
        RouteBook book = new RouteBook(tempDir);
        for (FareRule rule : new FareRule[]{new RegularFare(), new DiscountedFare()}) {
            for (String a : book.places()) for (String b : book.places()) {
                if (a.equals(b)) continue;
                long brute = bruteCheapest(book.routes(), a, b, null, rule, 0, new HashSet<>(Set.of(a)));
                List<TripPlanner.Plan> plans = planner.plan(a, b, rule);
                if (brute == Long.MAX_VALUE) { assertTrue(plans.isEmpty(), a + " -> " + b); continue; }
                long best = plans.stream().mapToLong(TripPlanner.Plan::fareCents).min().orElseThrow();
                assertEquals(brute, best, a + " -> " + b + " " + rule.discountPercent() + "% off");
            }
        }
    }

    /** Every journey of up to 4 rides, never revisiting a place or re-boarding the route just left. */
    private static long bruteCheapest(Map<String, List<RouteStop>> routes, String here, String goal,
                                      String cameOn, FareRule rule, int rides, Set<String> seen) {
        if (rides == 4) return Long.MAX_VALUE;
        long best = Long.MAX_VALUE;
        for (var e : routes.entrySet()) {
            if (e.getKey().equals(cameOn)) continue;
            List<RouteStop> line = e.getValue();
            for (int i = 0; i < line.size(); i++) {
                if (!line.get(i).name().equals(here)) continue;
                for (int j = 0; j < line.size(); j++) {
                    String next = line.get(j).name();
                    if (j == i || seen.contains(next)) continue;
                    long fare = Fares.fare(Fares.pathKm(line, Math.min(i, j), Math.max(i, j)), rule);
                    if (next.equals(goal)) { best = Math.min(best, fare); continue; }
                    seen.add(next);
                    long rest = bruteCheapest(routes, next, goal, e.getKey(), rule, rides + 1, seen);
                    seen.remove(next);
                    if (rest != Long.MAX_VALUE) best = Math.min(best, fare + rest);
                }
            }
        }
        return best;
    }

    @Test
    void testInvalidOriginOrDestination() {
        assertThrows(IllegalArgumentException.class, () -> planner.plan("Cubao", "Cubao", new RegularFare()));
        assertThrows(IllegalArgumentException.class, () -> planner.plan("Cubao", "NonExistentStop", new RegularFare()));
    }
}
