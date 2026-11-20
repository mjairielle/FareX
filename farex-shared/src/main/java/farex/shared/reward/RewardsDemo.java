package farex.shared.reward;

import java.nio.file.*;
import java.time.Instant;

/** Self-check. Run: java -cp ... farex.shared.reward.RewardsDemo */
public final class RewardsDemo {
    static void check(boolean ok, String name) { if (!ok) throw new AssertionError(name); System.out.println("PASS " + name); }

    public static void main(String[] a) throws Exception {
        Rewards r = new Rewards(Files.createTempDirectory("rw"));
        Instant now = Instant.parse("2026-10-09T10:00:00Z");
        check(r.balance("a") == 0, "new account has 0 points");
        check(r.earn("a", "t1", 13.5, now) == 13, "13.5 km earns 13 points");
        check(r.earn("a", "t1", 13.5, now) == 0, "same trip earns once");
        check(r.balance("a") == 13, "balance 13");
        check(r.earn("a", "t2", 0.4, now) == 0, "tiny trip earns 0");
        r.addPromotion("Double", 2.0, now.minusSeconds(3600), now.plusSeconds(3600));
        check(r.multiplier(now) == 2.0, "promotion active");
        check(r.multiplier(now.plusSeconds(7200)) == 1.0, "promotion expired");
        check(r.earn("a", "t3", 10, now) == 20, "promotion doubles points");
        try { r.addPromotion("Bad", 0.5, now, now.plusSeconds(1)); check(false, "x"); } catch (IllegalArgumentException e) { check(true, "multiplier below 1 rejected"); }
        try { r.check("a", r.perk("P10")); check(false, "x"); } catch (IllegalStateException e) { check(true, "33 points cannot redeem 100"); }
        r.earn("a", "t4", 100, now.plusSeconds(7200));
        r.check("a", r.perk("P10"));
        r.spend("a", r.perk("P10"), now);
        check(r.balance("a") == 33 + 100 - 100, "redeem deducts points");
        check(r.balance("b") == 0, "accounts are separate");
        try { r.perk("P99"); check(false, "x"); } catch (IllegalArgumentException e) { check(true, "unknown perk rejected"); }
    }
}
