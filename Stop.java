package FareX;

/**
 * A single boarding/alighting point on one or more routes.
 */
public class Stop {

    private static final double EARTH_RADIUS_KM = 6371.0;

    /**
     * Straight-line distance underestimates real road distance.
     * 1.3 is a common rule-of-thumb correction for urban street grids.
     * Tune this if you get real road distances later.
     */
    private static final double ROAD_WINDING_FACTOR = 1.3;

    private final String stopID;
    private final String name;
    private final double lat;
    private final double lng;

    public Stop(String stopID, String name, double lat, double lng) {
        this.stopID = stopID;
        this.name = name;
        this.lat = lat;
        this.lng = lng;
    }

    public String getStopID() { return stopID; }
    public String getName()   { return name; }
    public double getLat()    { return lat; }
    public double getLng()    { return lng; }

    /**
     * Great-circle distance to another stop, in kilometres,
     * scaled up to approximate travel along roads.
     */
    public double distanceKmTo(Stop other) {
        return straightLineKmTo(other) * ROAD_WINDING_FACTOR;
    }

    /** Raw great-circle distance, no road correction. Useful for walking radius checks. */
    public double straightLineKmTo(Stop other) {
        double dLat = Math.toRadians(other.lat - this.lat);
        double dLng = Math.toRadians(other.lng - this.lng);

        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                 + Math.cos(Math.toRadians(this.lat))
                 * Math.cos(Math.toRadians(other.lat))
                 * Math.sin(dLng / 2) * Math.sin(dLng / 2);

        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return EARTH_RADIUS_KM * c;
    }

    @Override
    public String toString() {
        return name + " (" + stopID + ")";
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Stop)) return false;
        return stopID.equals(((Stop) o).stopID);
    }

    @Override
    public int hashCode() {
        return stopID.hashCode();
    }
}
