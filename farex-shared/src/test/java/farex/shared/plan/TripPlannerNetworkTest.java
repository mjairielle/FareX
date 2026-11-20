package farex.shared.plan;

import farex.shared.data.RouteBook;
import farex.shared.fare.RegularFare;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

/** One-way routes, walking, start/end anywhere, and bidirectional Dijkstra == plain Dijkstra. */
public class TripPlannerNetworkTest {

    @TempDir
    Path tempDir;

    /** Writes routes.csv (+ route_names.csv) from "id|oneway|name,lat,lng;name,lat,lng;..." lines. */
    private RouteBook book(Path dir, String... routes) throws Exception {
        List<String> stops = new ArrayList<>(List.of("routeId,seq,name,lat,lng"));
        List<String> names = new ArrayList<>(List.of(RouteBook.NAMES_HEADER));
        for (String r : routes) {
            String[] f = r.split("\\|");
            names.add(f[0] + "," + f[0] + " jeep," + f[1]);
            String[] s = f[2].split(";");
            for (int i = 0; i < s.length; i++) stops.add(f[0] + "," + i + "," + s[i]);
        }
        Files.createDirectories(dir);
        Files.write(dir.resolve("routes.csv"), stops);
        Files.write(dir.resolve(RouteBook.NAMES_FILE), names);
        return new RouteBook(dir);
    }

    @Test
    void oneWayRouteIsNeverRiddenBackwards() throws Exception {
        TripPlanner p = new TripPlanner(book(tempDir, "R1|true|A,14.60,121.00;B,14.61,121.00;C,14.62,121.00"));
        assertFalse(p.plan("A", "C", new RegularFare()).isEmpty());
        assertTrue(p.plan("C", "A", new RegularFare()).isEmpty(), "C -> A would ride R1 backwards");
    }

    @Test
    void twoWayRouteWorksBothWays() throws Exception {
        TripPlanner p = new TripPlanner(book(tempDir, "R1|false|A,14.60,121.00;B,14.61,121.00;C,14.62,121.00"));
        assertFalse(p.plan("C", "A", new RegularFare()).isEmpty());
    }

    @Test
    void walksAShortGapToChangeJeeps() throws Exception {
        // R1 ends at B; R2 starts at B2, ~150 m east. No shared stop, so the only way is to walk.
        TripPlanner p = new TripPlanner(book(tempDir,
                "R1|true|A,14.60,121.00;B,14.61,121.00",
                "R2|true|B2,14.61,121.0014;C,14.63,121.0014"));
        List<TripPlanner.Plan> plans = p.plan("A", "C", new RegularFare());
        assertFalse(plans.isEmpty());
        List<TripPlanner.Leg> legs = plans.get(0).legs();
        assertEquals(List.of("R1", TripPlanner.WALK, "R2"), legs.stream().map(TripPlanner.Leg::route).toList());
        assertEquals(1, plans.get(0).transfers());
        assertEquals(0.15 * TripPlanner.WALK_DETOUR, legs.get(1).km(), 0.02);
    }

    @Test
    void walksNeverChain() throws Exception {
        // X, Y, Z are 200 m apart in a row, each on a route going far away; only R3 leaves Z for W.
        // Reaching Z from X takes two walks in a row (X -> Y -> Z): not allowed, so no trip.
        TripPlanner p = new TripPlanner(book(tempDir,
                "R1|true|X,14.6000,121.00;FarX,14.70,121.10",
                "R2|true|Y,14.6018,121.00;FarY,14.70,121.20",
                "R3|true|Z,14.6036,121.00;W,14.62,121.00"));
        assertTrue(p.plan("X", "W", new RegularFare()).isEmpty(), "walked X -> Y -> Z to reach R3");
        // one walk is fine: from Y, walk to Z and ride R3
        List<TripPlanner.Plan> fromY = p.plan("Y", "W", new RegularFare());
        assertFalse(fromY.isEmpty());
        assertEquals(List.of(TripPlanner.WALK, "R3"), fromY.get(0).legs().stream().map(TripPlanner.Leg::route).toList());
    }

    @Test
    void startAndEndAnywhereWithAWalk() throws Exception {
        TripPlanner p = new TripPlanner(book(tempDir, "R1|true|A,14.60,121.00;B,14.62,121.00"));
        // 300 m south of A to 200 m north of B
        List<TripPlanner.Plan> plans = p.plan(14.5973, 121.00, "Home", 14.6218, 121.00, "School", new RegularFare());
        assertFalse(plans.isEmpty());
        List<TripPlanner.Leg> legs = plans.get(0).legs();
        assertEquals(List.of(TripPlanner.WALK, "Home", "A"), List.of(legs.get(0).route(), legs.get(0).from(), legs.get(0).to()));
        assertEquals(14.5973, legs.get(0).fromAt().lat(), 1e-9, "legs say where their ends are");
        assertEquals("R1", legs.get(1).route());
        assertEquals(TripPlanner.WALK, legs.get(2).route());
        assertEquals("School", legs.get(2).to());
        assertEquals(0.30 * TripPlanner.WALK_DETOUR, legs.get(0).km(), 0.02);
        assertTrue(plans.get(0).minutes() > 6, "walking time counts");
        assertEquals(0, plans.get(0).transfers(), "walking to the jeep is not a transfer");
    }

    // ----- flagging a jeep down on its road, between stops -----

    private static List<String> routes(TripPlanner.Plan p) {
        return p.legs().stream().map(TripPlanner.Leg::route).toList();
    }

    @Test
    void flagsTheJeepDownRightWhereYouAre() throws Exception {
        // R1 runs 2.2 km due north, stops only at its ends. Home is on its road halfway (~50 m off):
        // no walk to a terminal, just wait there and ride.
        TripPlanner p = new TripPlanner(book(tempDir, "R1|true|A,14.60,121.00;B,14.62,121.00"));
        List<TripPlanner.Plan> plans = p.plan(14.61, 121.0005, "Home", 14.62, 121.00, "B", new RegularFare());
        assertFalse(plans.isEmpty());
        for (TripPlanner.Plan plan : plans) {
            assertEquals(List.of("R1"), routes(plan), plan.label() + ": " + plan.legs());
            TripPlanner.Leg ride = plan.legs().get(0);
            assertEquals("Home", ride.from(), "boards where you are");
            assertEquals(14.61, ride.fromAt().lat(), 1e-4, "on the road beside you");
            assertEquals(121.00, ride.fromAt().lng(), 1e-6);
            assertEquals(1.1, ride.km(), 0.05, "rides only the half it needs");
        }
    }

    @Test
    void flagsDownOnTheRoadWhenThatBeatsTheNearestStop() throws Exception {
        // Home is 300 m east of R1's road, halfway: the nearest stop is 1.1 km away, the road 300 m
        TripPlanner p = new TripPlanner(book(tempDir, "R1|true|A,14.60,121.00;B,14.62,121.00"));
        TripPlanner.Plan plan = p.plan(14.61, 121.0028, "Home", 14.62, 121.00, "B", new RegularFare()).get(0);
        assertEquals(List.of(TripPlanner.WALK, "R1"), routes(plan));
        assertEquals("the roadside near A", plan.legs().get(0).to(), "ties go to the earlier stop");
        assertEquals(0.30 * TripPlanner.WALK_DETOUR, plan.legs().get(0).km(), 0.02);
    }

    @Test
    void getsOffWhereTheRoadPassesTheDestination() throws Exception {
        TripPlanner p = new TripPlanner(book(tempDir, "R1|true|A,14.60,121.00;B,14.62,121.00"));
        TripPlanner.Plan plan = p.plan(14.60, 121.00, "A", 14.615, 121.0009, "Office", new RegularFare()).get(0);
        assertEquals(List.of("R1", TripPlanner.WALK), routes(plan));
        assertEquals("the roadside near B", plan.legs().get(0).to());
        assertEquals(1.67, plan.legs().get(0).km(), 0.05, "gets off before B, not at it");
        assertEquals("Office", plan.legs().get(1).to());
    }

    @Test
    void flaggedOneWayJeepsOnlyGoForward() throws Exception {
        // From halfway, back to A: a one-way route cannot take you there, a two-way one can
        String stops = "A,14.60,121.00;B,14.62,121.00";
        assertTrue(new TripPlanner(book(tempDir.resolve("one"), "R1|true|" + stops))
                .plan(14.61, 121.0005, "Home", 14.6001, 121.00, "A", new RegularFare()).isEmpty());
        List<TripPlanner.Plan> twoWay = new TripPlanner(book(tempDir.resolve("two"), "R1|false|" + stops))
                .plan(14.61, 121.0005, "Home", 14.6001, 121.00, "A", new RegularFare());
        assertFalse(twoWay.isEmpty());
        assertEquals(List.of("R1"), routes(twoWay.get(0)));
    }

    @Test
    void passingListsRoutesWhoseRoadIsNear() throws Exception {
        TripPlanner p = new TripPlanner(book(tempDir,
                "R1|true|A,14.60,121.00;B,14.62,121.00",
                "R2|false|C,14.61,120.99;D,14.61,121.01",
                "R3|true|E,14.70,121.10;F,14.71,121.10"));
        List<TripPlanner.Passing> here = p.passing(14.6101, 121.0003, TripPlanner.PASSING_M);
        assertEquals(List.of("R2", "R1"), here.stream().map(TripPlanner.Passing::routeId).toList(), "nearest first, R3 too far");
        assertEquals("A", here.get(1).firstStop());
        assertEquals("B", here.get(1).lastStop());
        assertTrue(here.get(1).oneway());
    }

    @Test
    void tooFarFromAnyStopSaysSo() throws Exception {
        TripPlanner p = new TripPlanner(book(tempDir, "R1|true|A,14.60,121.00;B,14.62,121.00"));
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
                () -> p.plan(14.70, 121.10, "Far", 14.62, 121.00, "B", new RegularFare()));
        assertTrue(e.getMessage().contains("start"), e.getMessage());
    }

    @Test
    void bidirectionalDijkstraMatchesPlainDijkstraEverywhere() throws Exception {
        // Random network: 40 stops on a ~6 km grid, 12 routes, some one-way, some sharing stops.
        Random rng = new Random(42);
        int n = 40;
        double[][] at = new double[n][];
        for (int i = 0; i < n; i++) at[i] = new double[]{14.60 + rng.nextDouble() * 0.05, 121.00 + rng.nextDouble() * 0.05};
        List<String> routes = new ArrayList<>();
        for (int r = 0; r < 12; r++) {
            List<Integer> ids = new ArrayList<>();
            for (int i = 0; i < n; i++) ids.add(i);
            Collections.shuffle(ids, rng);
            StringBuilder s = new StringBuilder();
            for (int k = 0; k < 3 + rng.nextInt(6); k++) {
                int i = ids.get(k);
                if (k > 0) s.append(';');
                s.append("S").append(i).append(',').append(at[i][0]).append(',').append(at[i][1]);
            }
            routes.add("R" + r + "|" + rng.nextBoolean() + "|" + s);
        }
        TripPlanner p = new TripPlanner(book(tempDir, routes.toArray(new String[0])));

        int pairs = 0, reachable = 0;
        long plainSettled = 0, biSettled = 0;
        for (String a : p.places()) for (String b : p.places()) {
            if (a.equals(b)) continue;
            for (boolean fastest : new boolean[]{true, false}) {
                double[] plain = p.compare(a, b, fastest, false);
                double[] bi = p.compare(a, b, fastest, true);
                if (plain[0] == Double.MAX_VALUE) {
                    assertEquals(Double.MAX_VALUE, bi[0], a + " -> " + b + ": plain finds nothing, bidirectional must not either");
                } else {
                    assertEquals(plain[0], bi[0], 1e-9, a + " -> " + b + (fastest ? " fastest" : " fewest transfers"));
                    reachable++;
                }
                plainSettled += (long) plain[1];
                biSettled += (long) bi[1];
                pairs++;
            }
        }
        assertTrue(reachable > pairs / 4, "network should be mostly connected: " + reachable + " of " + pairs);

        // and between random points, where flag-downs on the roads add their own edges
        int pointPairs = 0;
        for (int k = 0; k < 150; k++) {
            double[] a = {14.60 + rng.nextDouble() * 0.05, 121.00 + rng.nextDouble() * 0.05};
            double[] b = {14.60 + rng.nextDouble() * 0.05, 121.00 + rng.nextDouble() * 0.05};
            for (boolean fastest : new boolean[]{true, false}) {
                double[] plain, bi;
                try {
                    plain = p.comparePoints(a, b, fastest, false);
                    bi = p.comparePoints(a, b, fastest, true);
                } catch (IllegalArgumentException noRouteNear) {
                    continue;
                }
                assertEquals(plain[0], bi[0], 1e-9, Arrays.toString(a) + " -> " + Arrays.toString(b));
                if (plain[0] != Double.MAX_VALUE) pointPairs++;
            }
        }
        assertTrue(pointPairs > 20, "point searches with a trip: " + pointPairs);
        System.out.printf("    %d searches (%d reachable): plain Dijkstra settled %d nodes, bidirectional %d%n",
                pairs, reachable, plainSettled, biSettled);
    }
}
