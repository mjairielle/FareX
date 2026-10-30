package FareX;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * One public transport route: an ordered sequence of stops plus its fare rules.
 */
public class Route {

    private final String routeID;
    private final String name;
    private final List<Stop> stopsInOrder;
    private final List<Vehicle> activeVehicles;

    /** Shared and stateless; the formula itself lives in FareCalculator. */
    private static final FareCalculator FARES = new FareCalculator();

    // Fare scale. Loaded from routes.csv so rates can change
    // without recompiling. See fareFor().
    private final FareTable fareTable;

    // Average operating speed in km/h, used to estimate travel time.
    private final double averageSpeedKph;

    private boolean active = true;

    public Route(String routeID,
                 String name,
                 List<Stop> stopsInOrder,
                 double baseFare,
                 double baseKm,
                 double perKmRate,
                 double averageSpeedKph) {
        this.routeID = routeID;
        this.name = name;
        this.stopsInOrder = new ArrayList<>(stopsInOrder);

        // indexOf() returns the first match, so a stop that appears twice
        // would give wrong distances and fares. Refuse it loudly instead.
        // Supporting loop routes means keying nodes by position, not stop ID.
        Set<String> seen = new HashSet<>();
        for (Stop stop : this.stopsInOrder) {
            if (!seen.add(stop.getStopID())) {
                throw new IllegalArgumentException("route " + routeID + " visits stop "
                        + stop.getStopID() + " more than once; loop routes are not supported");
            }
        }
        this.activeVehicles = new ArrayList<>();
        // No effective date: routes.csv carries rates only. See TRIP_PLANNER.md.
        this.fareTable = new FareTable(baseFare, baseKm, perKmRate, null);
        this.averageSpeedKph = averageSpeedKph;
    }

    public String getRouteID() { return routeID; }
    public String getName()    { return name; }
    public boolean isActive()  { return active; }
    public void setActive(boolean active) { this.active = active; }

    public FareTable getFareTable()    { return fareTable; }
    public double getBaseFare()        { return fareTable.getBaseFare(); }
    public double getBaseKm()          { return fareTable.getBaseKm(); }
    public double getPerKmRate()       { return fareTable.getRatePerKm(); }
    public double getAverageSpeedKph() { return averageSpeedKph; }

    /** Unmodifiable so nobody can reorder the sequence by accident. */
    public List<Stop> getStopsInOrder() {
        return Collections.unmodifiableList(stopsInOrder);
    }

    public List<Vehicle> getActiveVehicles() {
        return Collections.unmodifiableList(activeVehicles);
    }

    public void addVehicle(Vehicle v) {
        activeVehicles.add(v);
    }

    /** Position of a stop in this route's sequence, or -1 if the route doesn't serve it. */
    public int indexOf(Stop stop) {
        for (int i = 0; i < stopsInOrder.size(); i++) {
            if (stopsInOrder.get(i).getStopID().equals(stop.getStopID())) {
                return i;
            }
        }
        return -1;
    }

    public boolean serves(Stop stop) {
        return indexOf(stop) >= 0;
    }

    /**
     * Distance along this route between two of its stops, summed stop by stop.
     * Returns 0.0 if either stop isn't on the route.
     * Direction-agnostic: distance A to B equals B to A.
     */
    public double getDistance(Stop from, Stop to) {
        int i = indexOf(from);
        int j = indexOf(to);
        if (i < 0 || j < 0) return 0.0;

        int lo = Math.min(i, j);
        int hi = Math.max(i, j);

        double total = 0.0;
        for (int k = lo; k < hi; k++) {
            total += stopsInOrder.get(k).distanceKmTo(stopsInOrder.get(k + 1));
        }
        return total;
    }

    /** Full end-to-end distance of the route. */
    public double getTotalDistance() {
        if (stopsInOrder.size() < 2) return 0.0;
        return getDistance(stopsInOrder.get(0), stopsInOrder.get(stopsInOrder.size() - 1));
    }

    /** Regular fare for riding a given distance on this route. */
    public double fareFor(double km) {
        return fareFor(km, PassengerType.REGULAR);
    }

    /**
     * Fare for riding a given distance on this route as a given passenger.
     * A thin delegate: FareCalculator owns the formula, so there is one copy.
     */
    public double fareFor(double km, PassengerType type) {
        return FARES.compute(km, type, fareTable);
    }

    /** Fare for riding the route end to end. */
    public double getMaxFare() {
        return fareFor(getTotalDistance());
    }

    /** Estimated minutes to travel a given distance on this route. */
    public double minutesFor(double km) {
        if (averageSpeedKph <= 0) return 0.0;
        return (km / averageSpeedKph) * 60.0;
    }

    @Override
    public String toString() {
        return name + " (" + routeID + ")";
    }
}
