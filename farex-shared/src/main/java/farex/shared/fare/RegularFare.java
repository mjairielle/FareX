package farex.shared.fare;

public class RegularFare extends FareRule {
    @Override
    public int discountPercent() {
        return 0;
    }
}
