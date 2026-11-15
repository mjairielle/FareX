package farex.shared.plan;

import farex.shared.data.RouteBook;
import farex.shared.fare.DiscountedFare;
import farex.shared.fare.RegularFare;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.List;

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
    void testInvalidOriginOrDestination() {
        assertThrows(IllegalArgumentException.class, () -> planner.plan("Cubao", "Cubao", new RegularFare()));
        assertThrows(IllegalArgumentException.class, () -> planner.plan("Cubao", "NonExistentStop", new RegularFare()));
    }
}
