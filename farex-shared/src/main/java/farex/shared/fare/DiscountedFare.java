package farex.shared.fare;

public class DiscountedFare extends FareRule {
    @Override
    public int discountPercent() {
        return 20;
    }
}