package farex.shared.fare;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

public class FareRuleTest {

    @TempDir
    Path tempDir;

    /** Fares holds the active table in static fields; tests that change it put it back. */
    private static void withTable(long base, double baseKm, long rate, Runnable body) {
        long b = Fares.BASE, r = Fares.RATE_PER_KM;
        double k = Fares.BASE_KM;
        Fares.BASE = base; Fares.BASE_KM = baseKm; Fares.RATE_PER_KM = rate;
        try { body.run(); } finally { Fares.BASE = b; Fares.BASE_KM = k; Fares.RATE_PER_KM = r; }
    }

    // Expected values: LTFRB fare guides effective 2026-09-28, as printed in the
    // "What common trips cost" table at parkinganph.com/guides/fare-matrix.

    @Test
    void testJeepneyFaresMatchLtfrbMatrix() {
        withTable(1400, 4.0, 200, () -> {
            RegularFare reg = new RegularFare();
            assertEquals(1400, Fares.fare(0, reg), "minimum");
            assertEquals(1400, Fares.fare(4, reg), "base covers the first 4 km");
            assertEquals(1600, Fares.fare(5, reg));
            assertEquals(2600, Fares.fare(10, reg));
            assertEquals(3000, Fares.fare(12, reg));
            assertEquals(4600, Fares.fare(20, reg));
            assertEquals(10600, Fares.fare(50, reg));
        });
    }

    @Test
    void testStudentFaresMatchLtfrbMatrix() {
        withTable(1400, 4.0, 200, () -> {
            DiscountedFare stu = new DiscountedFare();
            assertEquals(1125, Fares.fare(2, stu), "P11.20 rounds to P11.25");
            assertEquals(2075, Fares.fare(10, stu));
            assertEquals(3675, Fares.fare(20, stu));
            assertEquals(8475, Fares.fare(50, stu));
        });
    }

    @Test
    void testDiscountAppliesToScaleNotRoundedTotal() {
        // Aircon city bus: P18.00 first 5 km, P2.98/km. Guide prints P62.75 / P50.00 at 20 km.
        // 20% off the rounded P62.75 would give P50.25, so the order of operations matters.
        withTable(1800, 5.0, 298, () -> {
            assertEquals(6275, Fares.fare(20, new RegularFare()));
            assertEquals(5000, Fares.fare(20, new DiscountedFare()));
        });
        // Ordinary city bus: P15.00 first 5 km, P2.49/km -> P52.25 / P41.75 at 20 km.
        withTable(1500, 5.0, 249, () -> {
            assertEquals(5225, Fares.fare(20, new RegularFare()));
            assertEquals(4175, Fares.fare(20, new DiscountedFare()));
        });
    }

    @Test
    void testRoundToQuarterHalfUp() {
        assertEquals(5225, Fares.roundToQuarter(5235));
        assertEquals(1125, Fares.roundToQuarter(1120));
        assertEquals(1125, Fares.roundToQuarter(1112.5));
        assertEquals(1100, Fares.roundToQuarter(1112.4));
    }

    @Test
    void testOldFareRowsLoadAndBaseKmPublishes() {
        withTable(Fares.BASE, Fares.BASE_KM, Fares.RATE_PER_KM, () -> {
            try {
                // A fares.txt written before baseKm existed: 4 columns.
                Files.writeString(tempDir.resolve("fares.txt"),
                        "effective,base,ratePerKm,published\n2026-01-01,1300,200,2026-01-01T00:00:00Z\n");
                Fares.load(tempDir);
                assertEquals(1300, Fares.BASE);
                assertEquals(Fares.DEFAULT_BASE_KM, Fares.BASE_KM, 1e-9, "old rows default to 4 km");

                Fares.publish(tempDir, LocalDate.of(2026, 9, 28), 1800, 298, 5.0);
                assertEquals(1800, Fares.BASE);
                assertEquals(5.0, Fares.BASE_KM, 1e-9);
                assertEquals(6275, Fares.fare(20, new RegularFare()));

                assertThrows(IllegalArgumentException.class,
                        () -> Fares.publish(tempDir, LocalDate.of(2026, 9, 28), 1800, 298, -1));
            } catch (java.io.IOException e) {
                throw new RuntimeException(e);
            }
        });
    }

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
