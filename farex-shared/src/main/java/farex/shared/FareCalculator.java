package farex.shared;

import farex.shared.Enums.PassengerType;
import farex.shared.Model.*;
import java.util.Map;

/** One place for fare math. Operator phone, trip planner, and server all call this. */
public final class FareCalculator {
    private FareCalculator() {}

    /** Fare = base + rate per km x km, minus 20% for student, senior, PWD. Result in centavos. */
    public static long fare(double km, PassengerType type, FareTable ft) {
        long full = Math.round(ft.baseFare() + ft.ratePerKm() * km);
        return type == PassengerType.REGULAR ? full : (full * 80 + 50) / 100;
    }

    /** Distance along the route's stop order from one stop to another. Not straight line. */
    public static double distanceKm(Route route, Map<String, Stop> stops, String fromStopId, String toStopId) {
        // TODO: sum haversine between consecutive stops from fromStopId to toStopId
        throw new UnsupportedOperationException("TODO");
    }

    /** Fallback fare when passenger never taps out: full route end to end, no discount. */
    public static long maxFare(Route route, Map<String, Stop> stops, FareTable ft) {
        // TODO: distanceKm from first to last stop, then fare(..., REGULAR, ft)
        throw new UnsupportedOperationException("TODO");
    }
}
