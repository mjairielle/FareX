package FareX;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Takes raw candidate journeys from RouteGraph and turns them into the
 * short, sensible list a commuter actually wants to see.
 */
public class TripPlanner {

    /** What the commuter is optimising for. */
    public enum Preference {
        CHEAPEST,
        FASTEST,
        FEWEST_TRANSFERS
    }

    private static final int MAX_OPTIONS = 3;

    private final RouteGraph graph;
    private Preference preference = Preference.CHEAPEST;

    public TripPlanner(RouteGraph graph) {
        this.graph = graph;
    }

    public Preference getPreference() { return preference; }
    public void setPreference(Preference preference) { this.preference = preference; }

    /**
     * Full planning call: ask the graph for candidates, then rank them.
     * Will throw until RouteGraph.findPaths is implemented.
     */
    public List<TripOption> plan(Stop origin, Stop destination) {
        return rank(graph.findPaths(origin, destination));
    }

    /**
     * Dedupe, sort by the current preference, cap at 3.
     * Null-safe because findPaths may legitimately return nothing.
     */
    public List<TripOption> rank(List<TripOption> candidates) {
        List<TripOption> ranked = new ArrayList<>();
        if (candidates == null || candidates.isEmpty()) return ranked;

        Set<String> seen = new HashSet<>();
        for (TripOption option : candidates) {
            if (option == null) continue;
            if (option.getLegs().isEmpty()) continue;
            if (seen.add(option.signature())) {
                ranked.add(option);
            }
        }

        ranked.sort(comparatorFor(preference));

        if (ranked.size() > MAX_OPTIONS) {
            return new ArrayList<>(ranked.subList(0, MAX_OPTIONS));
        }
        return ranked;
    }

    /**
     * Each preference leads with its own metric, then falls back to the
     * others so ties break sensibly instead of arbitrarily.
     */
    private Comparator<TripOption> comparatorFor(Preference pref) {
        switch (pref) {
            case FASTEST:
                return Comparator
                        .comparingDouble(TripOption::getEstimatedTime)
                        .thenComparingDouble(TripOption::getTotalFare)
                        .thenComparingInt(TripOption::getTransfers);

            case FEWEST_TRANSFERS:
                return Comparator
                        .comparingInt(TripOption::getTransfers)
                        .thenComparingDouble(TripOption::getTotalFare)
                        .thenComparingDouble(TripOption::getEstimatedTime);

            case CHEAPEST:
            default:
                return Comparator
                        .comparingDouble(TripOption::getTotalFare)
                        .thenComparingDouble(TripOption::getEstimatedTime)
                        .thenComparingInt(TripOption::getTransfers);
        }
    }

    /**
     * Run all three preferences and merge the winners.
     * This is how you show "3 alternatives" instead of 3 near-identical routes.
     */
    public List<TripOption> planAllPreferences(Stop origin, Stop destination) {
        List<TripOption> candidates = graph.findPaths(origin, destination);
        if (candidates == null || candidates.isEmpty()) return new ArrayList<>();

        List<TripOption> merged = new ArrayList<>();
        Set<String> seen = new HashSet<>();

        Preference original = this.preference;
        for (Preference pref : Preference.values()) {
            this.preference = pref;
            List<TripOption> best = rank(candidates);
            if (!best.isEmpty() && seen.add(best.get(0).signature())) {
                merged.add(best.get(0));
            }
        }
        this.preference = original;

        return merged;
    }

    /**
     * Can this card afford the trip? Returns the shortfall in pesos,
     * or 0.0 if the balance covers it.
     *
     * This is the seam where the planner meets the ticketing half of FareX.
     */
    public static double shortfall(TripOption option, double cardBalance) {
        double needed = option.getTotalFare() - cardBalance;
        return needed > 0 ? needed : 0.0;
    }
}
