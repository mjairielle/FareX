package FareX;

import java.util.List;

public class Route {
    private String routeID, name;
    private List<Stop> stopsInOrder;
    private List<Vehicle> activeVehicles;

    public double getDistance(Stop from, Stop to) { return 0.0; }
    public double getMaxFare() { return 0.0; }
}
