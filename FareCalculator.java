package FareX;

/**
 * The one place the fare formula lives. Route.fareFor() delegates here, so
 * the planner's search, the displayed Leg fare and the tap side all agree.
 *
 * Reproduces the LTFRB fare guides effective 2026-09-28:
 *
 *   regular     base + rate * (km - baseKm)                  then round to PHP 0.25
 *   discounted  base' + rate' * (km - baseKm)                then round to PHP 0.25
 *               where base' = 0.8 * base, rate' = 0.8 * rate, each rounded to the centavo
 *
 * The discount is applied to the SCALE, not to the rounded regular fare.
 * That is how the guides print their student/senior/PWD column, and the two
 * methods disagree: an aircon city bus at 20 km is PHP 50.00 by the guide,
 * but 20% off the rounded regular PHP 62.75 would give PHP 50.25.
 */
public class FareCalculator {

    /** RA 9994 (seniors), RA 7277 (PWD), LTFRB fare guides (students). */
    public static final double DISCOUNT_RATE = 0.20;

    public double compute(double distanceKm, PassengerType type, FareTable ft) {
        double base = ft.getBaseFare();
        double rate = ft.getRatePerKm();

        if (isDiscounted(type)) {
            base = roundToCentavo(base * (1.0 - DISCOUNT_RATE));
            rate = roundToCentavo(rate * (1.0 - DISCOUNT_RATE));
        }

        double extraKm = Math.max(0.0, distanceKm - ft.getBaseKm());
        return roundToQuarter(base + extraKm * rate);
    }

    /** Students, seniors and PWD get the concession; null is treated as regular. */
    public static boolean isDiscounted(PassengerType type) {
        return type != null && type != PassengerType.REGULAR;
    }

    /**
     * LTFRB rounds every published fare to the nearest 25 centavos, half up:
     * a computed PHP 52.35 is collected as 52.25, PHP 11.20 as 11.25.
     * The epsilon keeps 0.125-boundary values from flooring on float error.
     */
    public static double roundToQuarter(double pesos) {
        return Math.floor(pesos * 4.0 + 0.5 + 1e-9) / 4.0;
    }

    private static double roundToCentavo(double pesos) {
        return Math.round(pesos * 100.0) / 100.0;
    }
}
