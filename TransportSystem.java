package FareX;
public class Stop { private String stopID, name; private double lat, lng; }

public class Route {
    private String routeID, name;
    private List<Stop> stopsInOrder;
    private List<Vehicle> activeVehicles;

    public double getDistance(Stop from, Stop to) {}
    public double getMaxFare() {}
}

public class Vehicle { 
    private String plateNo; 
    private Route route; 
    private Operator operator; 
    private boolean retired; }

public class FareTable { 
    private double baseFare, ratePerKm; 
    private LocalDate effectiveDate; }

public class FareCalculator {
    public double compute(double distanceKm, PassengerType type, FareTable ft) {}
}