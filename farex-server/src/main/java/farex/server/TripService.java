package farex.server;

import farex.shared.Enums.PassengerType;
import farex.shared.Model.TripOption;
import java.util.List;

/**
 * Trip planner. Replaces TripPlanner, RouteGraph, TripHandler.
 * Graph is built from routes.csv and stops.csv per request. ponytail: rebuild per request, cache when routes grow.
 */
public class TripService {
    /** Up to 3 options: shortest distance, fewest transfers, cheapest fare. Tie goes to shorter distance. Same route twice shows once. */
    public List<TripOption> plan(String fromStopId, String toStopId, PassengerType type) {
        // TODO: candidates() then pick the 3 winners then dedupe
        throw new UnsupportedOperationException("TODO");
    }

    /** Nodes are stops, edges are jeepney legs with distance and fare from FareCalculator. */
    private List<TripOption> candidates(String fromStopId, String toStopId, PassengerType type) {
        throw new UnsupportedOperationException("TODO");
    }
}
