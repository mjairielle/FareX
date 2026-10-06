package farex.shared.fare;

public abstract class FareRule {
    public abstract int discountPercent();

    public long apply(long fullFare) {
        return (fullFare * (100 - discountPercent()) + 50) / 100;
    }
}