package farex.shared.reward;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;

public class RewardsTest {

    @TempDir
    Path tempDir;

    private Rewards rewards;
    private final Instant now = Instant.parse("2026-10-09T10:00:00Z");

    @BeforeEach
    void setUp() {
        rewards = new Rewards(tempDir);
    }

    @Test
    void testInitialBalanceAndTripEarning() {
        assertEquals(0, rewards.balance("user1"));
        assertEquals(13, rewards.earn("user1", "trip1", 13.5, now));
        assertEquals(0, rewards.earn("user1", "trip1", 13.5, now), "Idempotent earning per trip");
        assertEquals(13, rewards.balance("user1"));
        assertEquals(0, rewards.earn("user1", "trip2", 0.4, now), "Distances < 1km earn 0");
    }

    @Test
    void testPromotionalMultiplier() {
        rewards.addPromotion("DoublePoints", 2.0, now.minusSeconds(3600), now.plusSeconds(3600));
        assertEquals(2.0, rewards.multiplier(now));
        assertEquals(1.0, rewards.multiplier(now.plusSeconds(7200)), "Expired promotion reverts to 1.0");

        assertEquals(20, rewards.earn("user1", "trip3", 10.0, now));
    }

    @Test
    void testPerkRedemption() {
        rewards.earn("user1", "trip4", 150.0, now);
        Rewards.Perk perk10 = rewards.perk("P10");

        rewards.check("user1", perk10);
        rewards.spend("user1", perk10, now);
        assertEquals(50, rewards.balance("user1"));
    }

    @Test
    void testInsufficientPointsThrows() {
        Rewards.Perk perk50 = rewards.perk("P50");
        assertThrows(IllegalStateException.class, () -> rewards.check("user1", perk50));
    }
}
