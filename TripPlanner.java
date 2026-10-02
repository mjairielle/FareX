package FareX;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class TripPlanner {

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
    
    public List<TripOption> plan(Stop origin, Stop destination) {
        return rank(graph.findPaths(origin, destination));
    }

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

    public static double shortfall(TripOption option, double cardBalance) {
        double needed = option.getTotalFare() - cardBalance;
        return needed > 0 ? needed : 0.0;
    }
}
