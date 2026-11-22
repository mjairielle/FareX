package farex.shared.plan;

import farex.shared.data.RouteShapes.Point;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class TripProgressTest {

    // Two straight rides due north along longitude 121.0: A -> B on R1 (~2.2 km), B -> C on R2 (~2.2 km).
    private static List<Point> line(double fromLat, double toLat) {
        List<Point> pts = new ArrayList<>();
        for (int i = 0; i <= 20; i++) pts.add(new Point(fromLat + (toLat - fromLat) * i / 20.0, 121.0));
        return pts;
    }

    private static TripProgress trip() {
        return new TripProgress(List.of(
                new TripProgress.Ride("R1", "A", "B", line(14.60, 14.62)),
                new TripProgress.Ride("R2", "B", "C", line(14.62, 14.64))));
    }

    @Test
    void checkpointsAreBoardTransferAlightInOrder() {
        TripProgress t = trip();
        List<TripProgress.Checkpoint> c = t.checkpoints();
        assertEquals(3, c.size());
        assertEquals(TripProgress.Kind.BOARD, c.get(0).kind());
        assertEquals("R1", c.get(0).route());
        assertEquals(TripProgress.Kind.TRANSFER, c.get(1).kind());
        assertEquals("B", c.get(1).stop());
        assertEquals("R2", c.get(1).route());
        assertEquals(TripProgress.Kind.ALIGHT, c.get(2).kind());
        assertEquals("C", c.get(2).stop());
        assertEquals(t.totalKm() / 2, c.get(1).atKm(), 0.01, "transfer is halfway");
    }

    @Test
    void progressNextStopAndMinutes() {
        TripProgress t = trip();
        TripProgress.Status s = t.update(14.60, 121.0);
        assertEquals(TripProgress.Kind.BOARD, s.next().kind(), "at the start: board first");

        s = t.update(14.61, 121.0);                       // halfway through ride 1
        assertEquals(TripProgress.Kind.TRANSFER, s.next().kind());
        assertEquals(t.totalKm() / 4, s.doneKm(), 0.02);
        assertEquals(t.totalKm() / 4, s.toNextKm(), 0.02);
        // 3/4 of the trip left at 15 km/h, plus one transfer
        int expected = (int) Math.ceil(t.totalKm() * 0.75 / TripProgress.JEEP_KPH * 60) + TripProgress.TRANSFER_MINUTES;
        assertEquals(expected, s.minutesLeft(), 1);
        assertFalse(s.offRoute());
        assertFalse(s.arrived());

        s = t.update(14.63, 121.0);                       // on ride 2: next is getting off, no transfer left
        assertEquals(TripProgress.Kind.ALIGHT, s.next().kind());
        assertEquals((int) Math.ceil(t.totalKm() * 0.25 / TripProgress.JEEP_KPH * 60), s.minutesLeft(), 1);
    }

    @Test
    void jitterNeverMovesProgressBack() {
        TripProgress t = trip();
        double ahead = t.update(14.615, 121.0).doneKm();
        double after = t.update(14.612, 121.0).doneKm();  // GPS wobbles 300 m back
        assertEquals(ahead, after, 1e-9);
    }

    @Test
    void offRouteIsReportedAndHoldsProgress() {
        TripProgress t = trip();
        double before = t.update(14.61, 121.0).doneKm();
        TripProgress.Status s = t.update(14.63, 121.01); // ~1 km east of the road
        assertTrue(s.offRoute(), "meters off: " + s.offRouteMeters());
        assertEquals(before, s.doneKm(), 1e-9, "progress held while off route");
    }

    @Test
    void arrivesNearTheEnd() {
        TripProgress t = trip();
        t.update(14.63, 121.0);
        TripProgress.Status s = t.update(14.6396, 121.0);  // ~45 m before C
        assertTrue(s.arrived());
        assertEquals(0, s.minutesLeft());
        assertEquals(1.0, s.fraction(), 1e-9);
    }

    @Test
    void pointAtWalksThePath() {
        TripProgress t = trip();
        Point mid = t.pointAt(t.totalKm() / 2);
        assertEquals(14.62, mid.lat(), 1e-3);
        assertEquals(t.path().get(0), t.pointAt(-1));
        assertEquals(t.path().get(t.path().size() - 1), t.pointAt(t.totalKm() + 5));
    }

    // Walk 0.5 km to A, ride R1 to B, walk 0.5 km on: walks are slower and have their own checkpoint
    private static TripProgress walkRideWalk() {
        return new TripProgress(List.of(
                new TripProgress.Ride(TripPlanner.WALK, "Home", "A", List.of(new Point(14.5955, 121.0), new Point(14.60, 121.0))),
                new TripProgress.Ride("R1", "A", "B", line(14.60, 14.62)),
                new TripProgress.Ride(TripPlanner.WALK, "B", "Office", List.of(new Point(14.62, 121.0), new Point(14.6245, 121.0)))));
    }

    @Test
    void walksHaveCheckpointsAndTheFirstRideIsStillBoarding() {
        List<TripProgress.Checkpoint> c = walkRideWalk().checkpoints();
        assertEquals(List.of(TripProgress.Kind.WALK, TripProgress.Kind.BOARD, TripProgress.Kind.WALK, TripProgress.Kind.ALIGHT),
                c.stream().map(TripProgress.Checkpoint::kind).toList());
        assertEquals("A", c.get(0).stop(), "walk checkpoint names where you walk to");
        assertEquals("Office", c.get(3).stop());
    }

    @Test
    void walkingTakesLongerPerKmThanRiding() {
        TripProgress t = walkRideWalk();
        TripProgress.Status s = t.update(14.5955, 121.0);
        double walkKm = 2 * 0.5, rideKm = t.totalKm() - walkKm;
        int expected = (int) Math.ceil(walkKm / TripProgress.WALK_KPH * 60 + rideKm / TripProgress.JEEP_KPH * 60);
        assertEquals(expected, s.minutesLeft(), 1);
        assertEquals(TripProgress.Kind.WALK, s.next().kind());
        assertTrue(t.legAt(0.1).walk());
        assertEquals("R1", t.legAt(1.0).route());
        assertTrue(t.legAt(t.totalKm() - 0.1).walk());
    }

    @Test
    void rejectsEmptyTripsAndPathlessRides() {
        assertThrows(IllegalArgumentException.class, () -> new TripProgress(List.of()));
        assertThrows(IllegalArgumentException.class, () -> new TripProgress(List.of(
                new TripProgress.Ride("R1", "A", "B", List.of(new Point(14.6, 121.0))))));
    }
}
