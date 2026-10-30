package FareX;

/**
 * One continuous ride on a single route: board here, stay on, get off there.
 *
 * This is the class the original skeleton was missing. TripOption held a
 * List<Route>, which tells you WHICH routes to take but not WHERE you get on
 * and off them - so distance and fare were impossible to compute.
 */
public class Leg {

    private final Route route;
    private final Stop boardStop;
    private final Stop alightStop;

    private final double distanceKm;
    private final double fare;
    private final double minutes;

    public Leg(Route route, Stop boardStop, Stop alightStop) {
        this.route = route;
        this.boardStop = boardStop;
        this.alightStop = alightStop;

        this.distanceKm = route.getDistance(boardStop, alightStop);
        this.fare = route.fareFor(this.distanceKm);
        this.minutes = route.minutesFor(this.distanceKm);
    }

    public Route getRoute()      { return route; }
    public Stop getBoardStop()   { return boardStop; }
    public Stop getAlightStop()  { return alightStop; }
    public double getDistanceKm(){ return distanceKm; }
    public double getFare()      { return fare; }
    public double getMinutes()   { return minutes; }

    /** Fare for this ride as a given passenger (student/senior/PWD get the concession). */
    public double getFareFor(PassengerType type) {
        return FareCalculator.isDiscounted(type) ? route.fareFor(distanceKm, type) : fare;
    }

    /** How many stops you stay on board for. Useful for the UI. */
    public int getStopCount() {
        int i = route.indexOf(boardStop);
        int j = route.indexOf(alightStop);
        if (i < 0 || j < 0) return 0;
        return Math.abs(j - i);
    }

    @Override
    public String toString() {
        return describe(PassengerType.REGULAR);
    }

    /** One-line summary priced for a given passenger. */
    public String describe(PassengerType type) {
        return String.format("%s: %s -> %s (%.1f km, PHP %.2f, %.0f min)",
                route.getName(),
                boardStop.getName(),
                alightStop.getName(),
                distanceKm, getFareFor(type), minutes);
    }
}
