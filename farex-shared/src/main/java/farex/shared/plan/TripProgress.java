package farex.shared.plan;

import farex.shared.data.RouteShapes.Point;
import farex.shared.fare.Fares;
import java.util.*;

/**
 * Where a passenger is along a planned trip, from position fixes (phone GPS or a simulator).
 *
 * The trip is its rides' road paths joined end to end, with checkpoints where you board,
 * transfer and get off. Each fix is projected onto that path to give km done, the next
 * checkpoint, minutes left, arrival and off-route. Progress never goes backwards: GPS jitter
 * near a stop must not make "next stop" flicker.
 */
public final class TripProgress {
    public static final double JEEP_KPH = 15.0;          // same average speed as TripPlanner's estimate
    public static final int TRANSFER_MINUTES = 5;
    public static final double ARRIVED_METERS = 80;
    public static final double OFF_ROUTE_METERS = 300;
    private static final double BACKTRACK_KM = 0.3;      // how far back a fix may match (U-turn loops, jitter)

    /** One ride: board `from`, ride `route` along `path`, get off at `to`. */
    public record Ride(String route, String from, String to, List<Point> path) {}

    public enum Kind { BOARD, TRANSFER, ALIGHT }

    /** A place on the trip where something happens. route = the jeep to ride from here (null when getting off). */
    public record Checkpoint(Kind kind, String stop, String route, double atKm) {}

    public record Status(double doneKm, double totalKm, double offRouteMeters, Checkpoint next,
                         double toNextKm, int minutesLeft, boolean arrived) {
        public boolean offRoute() { return offRouteMeters > OFF_ROUTE_METERS; }
        public double fraction() { return totalKm <= 0 ? 1 : Math.min(1, doneKm / totalKm); }
    }

    private final List<Point> path = new ArrayList<>();
    private final double[] cumKm;                       // km along the path at each point
    private final List<Checkpoint> checkpoints = new ArrayList<>();
    private double doneKm = 0;

    public TripProgress(List<Ride> rides) {
        if (rides.isEmpty()) throw new IllegalArgumentException("A trip needs at least one ride");
        int[] firstPoint = new int[rides.size()];
        for (int i = 0; i < rides.size(); i++) {
            Ride r = rides.get(i);
            if (r.path().size() < 2) throw new IllegalArgumentException("Ride " + r.route() + " has no path");
            firstPoint[i] = path.size();
            path.addAll(r.path());
        }
        cumKm = new double[path.size()];
        for (int i = 1; i < path.size(); i++) cumKm[i] = cumKm[i - 1] + km(path.get(i - 1), path.get(i));

        for (int i = 0; i < rides.size(); i++) {
            Ride r = rides.get(i);
            checkpoints.add(new Checkpoint(i == 0 ? Kind.BOARD : Kind.TRANSFER, r.from(), r.route(), cumKm[firstPoint[i]]));
        }
        checkpoints.add(new Checkpoint(Kind.ALIGHT, rides.get(rides.size() - 1).to(), null, totalKm()));
    }

    public double totalKm() { return cumKm[cumKm.length - 1]; }
    public List<Checkpoint> checkpoints() { return Collections.unmodifiableList(checkpoints); }
    public List<Point> path() { return Collections.unmodifiableList(path); }

    /** The point `km` along the trip (clamped to its ends). Used by the desktop simulator. */
    public Point pointAt(double km) {
        if (km <= 0) return path.get(0);
        if (km >= totalKm()) return path.get(path.size() - 1);
        int i = 1;
        while (cumKm[i] < km) i++;
        double seg = cumKm[i] - cumKm[i - 1];
        double t = seg <= 0 ? 0 : (km - cumKm[i - 1]) / seg;
        Point a = path.get(i - 1), b = path.get(i);
        return new Point(a.lat() + t * (b.lat() - a.lat()), a.lng() + t * (b.lng() - a.lng()));
    }

    /** Take one position fix and say where the passenger is. */
    public Status update(double lat, double lng) {
        Point p = new Point(lat, lng);
        Match best = nearest(p, Math.max(0, doneKm - BACKTRACK_KM));
        if (best.meters > OFF_ROUTE_METERS) {               // lost the thread (GPS jump, detour): look everywhere
            Match anywhere = nearest(p, 0);
            if (anywhere.meters < best.meters) best = anywhere;
        }
        if (best.meters <= OFF_ROUTE_METERS) doneKm = Math.max(doneKm, best.km);   // off route: hold progress

        double total = totalKm();
        double left = Math.max(0, total - doneKm);
        boolean arrived = left * 1000 <= ARRIVED_METERS || km(p, path.get(path.size() - 1)) * 1000 <= ARRIVED_METERS;
        if (arrived) doneKm = total;

        // Checkpoints are in trip order: the next one is the first not yet passed (50 m slack).
        // Getting off is never "passed", so there is always a next one.
        Checkpoint next = null;
        int transfersLeft = 0;
        for (Checkpoint c : checkpoints) {
            if (c.kind() != Kind.ALIGHT && c.atKm() + 0.05 <= doneKm) continue;
            if (next == null) next = c;
            if (c.kind() == Kind.TRANSFER) transfersLeft++;
        }
        int minutes = arrived ? 0 : (int) Math.ceil(left / JEEP_KPH * 60) + transfersLeft * TRANSFER_MINUTES;
        return new Status(doneKm, total, best.meters, next, Math.max(0, next.atKm() - doneKm), minutes, arrived);
    }

    private record Match(double km, double meters) {}

    // Nearest point on the path to p, among segments that end past `fromKm`
    private Match nearest(Point p, double fromKm) {
        Match best = new Match(0, Double.MAX_VALUE);
        double k = Math.cos(Math.toRadians(p.lat()));      // flat projection is fine over a few km
        for (int i = 1; i < path.size(); i++) {
            if (cumKm[i] < fromKm) continue;
            Point a = path.get(i - 1), b = path.get(i);
            double ax = a.lng() * k, ay = a.lat(), bx = b.lng() * k, by = b.lat(), px = p.lng() * k, py = p.lat();
            double dx = bx - ax, dy = by - ay, len2 = dx * dx + dy * dy;
            double t = len2 == 0 ? 0 : Math.max(0, Math.min(1, ((px - ax) * dx + (py - ay) * dy) / len2));
            Point q = new Point(ay + t * dy, (ax + t * dx) / k);
            double m = km(p, q) * 1000;
            if (m < best.meters) best = new Match(cumKm[i - 1] + t * (cumKm[i] - cumKm[i - 1]), m);
        }
        return best;
    }

    private static double km(Point a, Point b) {
        return Fares.haversineKm(a.lat(), a.lng(), b.lat(), b.lng());
    }
}
