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
        assertEquals(new TripPlanner.Leg(TripPlanner.WALK, "Home", "A", legs.get(0).km()), legs.get(0));
        assertEquals("R1", legs.get(1).route());
        assertEquals(TripPlanner.WALK, legs.get(2).route());
        assertEquals("School", legs.get(2).to());
        assertEquals(0.30 * TripPlanner.WALK_DETOUR, legs.get(0).km(), 0.02);
        assertTrue(plans.get(0).minutes() > 6, "walking time counts");
        assertEquals(0, plans.get(0).transfers(), "walking to the jeep is not a transfer");
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
        System.out.printf("    %d searches (%d reachable): plain Dijkstra settled %d nodes, bidirectional %d%n",
                pairs, reachable, plainSettled, biSettled);
    }
}
