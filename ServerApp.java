package FareX;

import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Application startup. Loads the route network once and owns the single
 * TripPlanner every Passenger shares.
 *
 *   ServerApp app = new ServerApp("data");
 *   app.start();                    // load CSVs -> build graph -> planner
 *   app.registerPassenger(p);       // p.planTrip(...) now works
 *
 * Route data is read from local CSVs only - no network calls at startup.
 */
public class ServerApp {

    public static final String DEFAULT_DATA_DIR = "data";

    private final String dataDir;

    private RouteLoader loader;
    private List<Route> routes = Collections.emptyList();
    private RouteGraph graph;
    private TripPlanner tripPlanner;

    public ServerApp() {
        this(DEFAULT_DATA_DIR);
    }

    public ServerApp(String dataDir) {
        this.dataDir = dataDir;
    }

    /**
     * Load stops.csv, routes.csv and route_stops.csv from the data folder,
     * build the graph and the shared planner, and print a summary plus every
     * loader warning. Safe to call twice; the second call does nothing.
     *
     * Fails loudly rather than starting with an empty network: a missing
     * file or a file with no usable routes throws IllegalStateException.
     */
    public void start() {
        if (isStarted()) return;

        RouteLoader newLoader = new RouteLoader();
        List<Route> loaded;
        try {
            loaded = newLoader.load(path("stops.csv"), path("routes.csv"), path("route_stops.csv"));
        } catch (IOException e) {
            throw new IllegalStateException("cannot read route data from '" + dataDir + "': "
                    + e.getMessage(), e);
        }

        if (loaded.isEmpty()) {
            for (String w : newLoader.getWarnings()) System.out.println("  WARN: " + w);
            throw new IllegalStateException("no usable routes in '" + dataDir + "'");
        }

        RouteGraph newGraph = new RouteGraph();
        newGraph.build(loaded);
        if (newGraph.getAllRoutes().isEmpty()) {
            newLoader.getWarnings().add("every route is inactive; no trips can be planned");
        }
        for (String w : newLoader.getWarnings()) System.out.println("  WARN: " + w);

        this.loader = newLoader;
        this.routes = loaded;
        this.graph = newGraph;
        this.tripPlanner = new TripPlanner(newGraph);

        System.out.println("FareX started: " + newGraph.describe()
                + ", " + newLoader.getWarnings().size() + " warning(s)");
    }

    public boolean isStarted() { return tripPlanner != null; }

    /** Give a passenger the shared planner so planTrip() works. */
    public void registerPassenger(Passenger passenger) {
        passenger.setTripPlanner(getTripPlanner());
    }

    public TripPlanner getTripPlanner() { requireStarted(); return tripPlanner; }
    public RouteGraph getGraph()        { requireStarted(); return graph; }
    public List<Route> getRoutes()      { return Collections.unmodifiableList(routes); }
    public String getDataDir()          { return dataDir; }

    /** Every loaded stop by ID, for turning a UI selection into a Stop. */
    public Map<String, Stop> getStops() {
        requireStarted();
        return Collections.unmodifiableMap(loader.getStops());
    }

    /** A stop by ID, or null if there is no such stop. */
    public Stop findStop(String stopID) {
        return getStops().get(stopID);
    }

    /** What the loader skipped or defaulted. Empty means the data is clean. */
    public List<String> getWarnings() {
        return loader == null ? Collections.emptyList()
                              : Collections.unmodifiableList(loader.getWarnings());
    }

    private String path(String file) {
        return new File(dataDir, file).getPath();
    }

    private void requireStarted() {
        if (!isStarted()) throw new IllegalStateException("ServerApp.start() has not been called");
    }

    /** java -cp out FareX.ServerApp [dataDir] */
    public static void main(String[] args) {
        new ServerApp(args.length > 0 ? args[0] : DEFAULT_DATA_DIR).start();
    }
}
