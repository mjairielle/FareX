package FareX;
public class RouteGraph {
    public void build(List<Route> routes) {}
    public List<TripOption> findPaths(Stop a, Stop b) {}
}

public class TripOption {
    private List<Route> legs;
    private double totalDistance, totalFare, estimatedTime;
    private int transfers;
}

public class TripPlanner {
    public List<TripOption> rank(List<TripOption> candidates) {}   // max 3, dedupe
}