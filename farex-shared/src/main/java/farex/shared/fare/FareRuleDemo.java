package farex.shared.fare;

public class FareRuleDemo {
    public static void main(String[] args) {
        long full = 1300 + 150 * 10;
        FareRule[] rules = { new RegularFare(), new DiscountedFare() };
        for (FareRule r : rules) {
            System.out.println(r.discountPercent() + "% off -> " + r.apply(full));
        }
    }
}