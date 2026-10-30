package FareX;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * One complete door-to-door journey made of one or more Legs.
 * Totals are computed once at construction - no stale fields to keep in sync.
 */
public class TripOption {

    /**
     * Default minutes lost waiting and walking each time you change vehicles.
     * RouteGraph starts from this value and passes its own (possibly changed)
     * setting into every TripOption it builds, so search and display agree.
     */
    public static final double DEFAULT_TRANSFER_MINUTES = 5.0;

    private final List<Leg> legs;
    private final double totalDistance;
    private final double totalFare;
    private final double estimatedTime;
    private final int transfers;

    public TripOption(List<Leg> legs) {
        this(legs, DEFAULT_TRANSFER_MINUTES);
    }

    public TripOption(List<Leg> legs, double transferMinutes) {
        this.legs = new ArrayList<>(legs);

        double distance = 0.0;
        double fare = 0.0;
        double time = 0.0;

        for (Leg leg : this.legs) {
            distance += leg.getDistanceKm();
            fare += leg.getFare();
            time += leg.getMinutes();
        }

        this.transfers = Math.max(0, this.legs.size() - 1);
        this.totalDistance = distance;
        this.totalFare = fare;
        this.estimatedTime = time + (this.transfers * transferMinutes);
    }

    public List<Leg> getLegs()       { return Collections.unmodifiableList(legs); }
    public double getTotalDistance() { return totalDistance; }
    public double getTotalFare()     { return totalFare; }
    public double getEstimatedTime() { return estimatedTime; }
    public int getTransfers()        { return transfers; }

    /**
     * Total fare as a given passenger: each leg is discounted and rounded on
     * its own, because each boarding is a separate fare paid to a separate
     * conductor. getTotalFare() is the regular-passenger figure.
     */
    public double getTotalFareFor(PassengerType type) {
        if (!FareCalculator.isDiscounted(type)) return totalFare;
        double total = 0.0;
        for (Leg leg : legs) total += leg.getFareFor(type);
        return total;
    }

    public Stop getOrigin() {
        return legs.isEmpty() ? null : legs.get(0).getBoardStop();
    }

    public Stop getDestination() {
        return legs.isEmpty() ? null : legs.get(legs.size() - 1).getAlightStop();
    }

    /**
     * Identity of this trip for deduplication: the ordered route IDs plus
     * where you board and alight each one. Two options with the same
     * signature are the same journey.
     */
    public String signature() {
        StringBuilder sb = new StringBuilder();
        for (Leg leg : legs) {
            sb.append(leg.getRoute().getRouteID())
              .append(':').append(leg.getBoardStop().getStopID())
              .append('>').append(leg.getAlightStop().getStopID())
              .append('|');
        }
        return sb.toString();
    }

    /** Per-leg breakdown for the UI, at the regular fare. */
    public String toDetailedString() {
        return toDetailedString(PassengerType.REGULAR);
    }

    /** Per-leg breakdown for the UI, priced for a given passenger. */
    public String toDetailedString(PassengerType type) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < legs.size(); i++) {
            sb.append("  ").append(i + 1).append(". ").append(legs.get(i).describe(type)).append('\n');
            if (i < legs.size() - 1) {
                sb.append("     [transfer]\n");
            }
        }
        String who = FareCalculator.isDiscounted(type) ? " (" + type + " fare)" : "";
        sb.append(String.format("  TOTAL: PHP %.2f%s | %.1f km | %.0f min | %d transfer(s)",
                getTotalFareFor(type), who, totalDistance, estimatedTime, transfers));
        return sb.toString();
    }

    @Override
    public String toString() {
        return String.format("TripOption[PHP %.2f, %.1f km, %.0f min, %d transfers]",
                totalFare, totalDistance, estimatedTime, transfers);
    }
}
