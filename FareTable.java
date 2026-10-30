package FareX;

import java.time.LocalDate;

/**
 * One published LTFRB fare scale: a flat fare for the first baseKm, then
 * ratePerKm for every kilometre after. Immutable - a new matrix is a new
 * FareTable with a new effectiveDate, never an edit to the old one.
 *
 * Per-km-only tiers (provincial aircon, UV Express) are baseFare 0, baseKm 0.
 */
public class FareTable {
    private final double baseFare, baseKm, ratePerKm;
    private final LocalDate effectiveDate;

    public FareTable(double baseFare, double baseKm, double ratePerKm, LocalDate effectiveDate) {
        if (baseFare < 0 || baseKm < 0 || ratePerKm < 0) {
            throw new IllegalArgumentException("fare table values must not be negative");
        }
        this.baseFare = baseFare;
        this.baseKm = baseKm;
        this.ratePerKm = ratePerKm;
        this.effectiveDate = effectiveDate;
    }

    public double getBaseFare()         { return baseFare; }
    public double getBaseKm()           { return baseKm; }
    public double getRatePerKm()        { return ratePerKm; }
    public LocalDate getEffectiveDate() { return effectiveDate; }

    @Override
    public String toString() {
        return String.format("PHP %.2f first %.0f km + PHP %.2f/km (from %s)",
                baseFare, baseKm, ratePerKm, effectiveDate);
    }
}
