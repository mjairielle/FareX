package farex.shared.fare;

import farex.shared.data.RoadDistances;
import farex.shared.data.RouteBook;
import farex.shared.data.RouteStop;
import farex.shared.data.Store;
import farex.shared.data.TapDesk;
import farex.shared.plan.TripPlanner;
import farex.shared.txn.Txn;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/** Fares on road distances (route_km.csv) instead of straight lines. Fares.ROAD_KM is global: always reset. */
public class RoadKmTest {

    @TempDir
    Path tempDir;

    private static final RouteStop A = new RouteStop("A", 14.6000, 121.0000);
    private static final RouteStop B = new RouteStop("B", 14.6100, 121.0000);
    private static final RouteStop C = new RouteStop("C", 14.6200, 121.0000);

    @Test
    void pathKmUsesRoadKmThenReverseThenStraightLine() {
        try {
            Fares.setRoadKm(Map.of(RoadDistances.key(A, B), 2.5));
            List<RouteStop> line = List.of(A, B, C);
            assertEquals(2.5, Fares.pathKm(line, 0, 1), 1e-9, "known stretch: road km");
            assertEquals(2.5, Fares.pathKm(List.of(B, A), 0, 1), 1e-9, "only the other direction known: use it");
            assertEquals(Fares.haversineKm(B, C), Fares.pathKm(line, 1, 2), 1e-9, "unknown stretch: straight line");
            assertEquals(2.5 + Fares.haversineKm(B, C), Fares.pathKm(line, 0, 2), 1e-9);
        } finally {
            Fares.setRoadKm(Map.of());
        }
    }

    @Test
    void loadMapsSegmentsToStopCoordinatesAndSkipsBadRows() throws Exception {
        Files.writeString(tempDir.resolve(RoadDistances.FILE), String.join("\n",
                RoadDistances.HEADER, "R1,0,2.5", "R1,1,oops", "R1,7,9.9", "NOPE,0,1.0", "R1,1,-3", ""));
        Map<String, Double> km = RoadDistances.load(tempDir, Map.of("R1", List.of(A, B, C)));
        assertEquals(Map.of(RoadDistances.key(A, B), 2.5), km);
    }

    @Test
    void missingFileMeansStraightLines() {
        assertTrue(RoadDistances.load(tempDir, Map.of("R1", List.of(A, B))).isEmpty());
    }

    @Test
    void sharedRoadDoesNotTemptAPointlessTransfer() throws Exception {
        try {
            // data/route_km.csv as committed: R1 and R3 share Commonwealth Ave from Tandang Sora to
            // Fairview, so Philcoa -> Fairview on R1 and R1 -> R3 at Tandang Sora tie on distance.
            RouteBook book = new RouteBook(tempDir);
            Files.writeString(tempDir.resolve(RoadDistances.FILE), String.join("\n", RoadDistances.HEADER,
                    "R1,0,1.619", "R1,1,2.774", "R1,2,0.960", "R1,3,2.592", "R1,4,4.872",
                    "R2,0,2.135", "R2,1,4.042", "R3,0,5.167", "R3,1,11.198", ""));
            Fares.setRoadKm(RoadDistances.load(tempDir, book.routes()));

            for (TripPlanner.Plan p : new TripPlanner(book).plan("Philcoa", "Fairview Terminal", new RegularFare())) {
                assertEquals(0, p.transfers(), p.label() + " should ride R1 straight through");
            }
        } finally {
            Fares.setRoadKm(Map.of());
        }
    }

    @Test
    void plannerAndTapOutChargeTheSameRoadDistance() throws Exception {
        try {
            // Seeded network: R1 starts Philcoa -> Tandang Sora, and Store's operator stops are R1's.
            RouteBook book = new RouteBook(tempDir);
            List<RouteStop> r1 = book.routes().get("R1");
            Files.writeString(tempDir.resolve(RoadDistances.FILE), RoadDistances.HEADER + "\nR1,0,6.0\n");
            Fares.setRoadKm(RoadDistances.load(tempDir, book.routes()));

            TripPlanner.Plan plan = new TripPlanner(book).plan(r1.get(0).name(), r1.get(1).name(), new RegularFare()).get(0);
            assertEquals(6.0, plan.km(), 1e-9, "planner uses the 6 km in route_km.csv, not the ~1.6 km straight line");
            assertEquals(Fares.fare(6.0, new RegularFare()), plan.fareCents());

            Store store = new Store(tempDir);
            TapDesk desk = new TapDesk(store, "o1");
            desk.tapIn("0067676767", 0);
            Txn t = desk.tapOut("0067676767", 1);                     // seeded card of Ana, a STUDENT
            assertEquals(Fares.fare(6.0, new DiscountedFare()), t.getAmount(), "tap-out charges the same road distance");
            assertNotEquals(Fares.fare(Fares.haversineKm(r1.get(0), r1.get(1)), new DiscountedFare()), t.getAmount(),
                    "and not the straight-line fare");
        } finally {
            Fares.setRoadKm(Map.of());
        }
    }
}
