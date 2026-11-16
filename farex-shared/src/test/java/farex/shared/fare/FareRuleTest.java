package farex.shared.fare;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class FareRuleTest {

    @Test
    void testRegularFareNoDiscount() {
        FareRule regular = new RegularFare();
        assertEquals(0, regular.discountPercent());
        assertEquals(2800, regular.apply(2800));
    }

    @Test
    void testDiscountedFareTwentyPercent() {
        FareRule discounted = new DiscountedFare();
        assertEquals(20, discounted.discountPercent());
        // 20% discount on 2800 centavos -> 2800 * 0.8 = 2240 centavos
        assertEquals(2240, discounted.apply(2800));
    }

    @Test
    void testHaversineDistanceCalculation() {
        // Manila area coordinates
        double lat1 = 14.6549, lon1 = 121.0505; // Philcoa
        double lat2 = 14.6738, lon2 = 121.0583; // Tandang Sora
        double dist = Fares.haversineKm(lat1, lon1, lat2, lon2);
        assertTrue(dist > 1.5 && dist < 3.0, "Philcoa to Tandang Sora is approx 2km");
    }
}
