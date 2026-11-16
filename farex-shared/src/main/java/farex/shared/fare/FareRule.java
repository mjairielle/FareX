package farex.shared.fare;

public abstract class FareRule {
    public abstract int discountPercent();

    public long apply(long fullFare) {
        return (fullFare * (100 - discountPercent()) + 50) / 100;
    }

    /** REGULAR pays full. STUDENT, SENIOR, PWD share one 20% rule. */
    public static FareRule of(String passengerType) {
        return "REGULAR".equals(passengerType) ? new RegularFare() : new DiscountedFare();
    }
}
