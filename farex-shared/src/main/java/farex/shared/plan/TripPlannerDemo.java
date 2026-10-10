package farex.shared.plan;

import farex.shared.data.RouteBook;
import farex.shared.fare.*;
import java.nio.file.*;
import java.util.List;

/** Self-check. Run: java -cp ... farex.shared.plan.TripPlannerDemo */
public final class TripPlannerDemo {
    static void check(boolean ok, String name) { if (!ok) throw new AssertionError(name); System.out.println("PASS " + name); }

    public static void main(String[] a) throws Exception {
        Path dir = Files.createTempDirectory("tp");
        TripPlanner tp = new TripPlanner(new RouteBook(dir));
        List<TripPlanner.Plan> plans = tp.plan("Cubao", "Fairview Terminal", new RegularFare());
        plans.forEach(p -> System.out.printf("  %s: %.1f km, %d transfers, P%.2f, %d min, %s%n", p.label(), p.km(), p.transfers(), p.fareCents() / 100.0, p.minutes(), p.legs()));
        check(plans.size() >= 2 && plans.size() <= 3, "2 to 3 distinct plans");
        TripPlanner.Plan shortest = plans.stream().filter(p -> p.label().contains("Shortest")).findFirst().get();
        TripPlanner.Plan fewest = plans.stream().filter(p -> p.label().contains("Fewest")).findFirst().get();
        TripPlanner.Plan cheapest = plans.stream().filter(p -> p.label().contains("Cheapest")).findFirst().get();
        check(plans.stream().allMatch(p -> p.km() >= shortest.km() - 1e-9), "shortest has least km");
        check(plans.stream().allMatch(p -> p.transfers() >= fewest.transfers()), "fewest has least transfers");
        check(plans.stream().allMatch(p -> p.fareCents() >= cheapest.fareCents()), "cheapest has least fare");
        check(plans.stream().map(p -> p.legs().toString()).distinct().count() == plans.size(), "no duplicate plans");
        check(tp.plan("Philcoa", "Fairview Terminal", new RegularFare()).stream().filter(p -> p.label().contains("Fewest")).findFirst().get().transfers() == 0, "direct ride has 0 transfers");
        long reg = tp.plan("Cubao", "Philcoa", new RegularFare()).get(0).fareCents();
        long dis = tp.plan("Cubao", "Philcoa", new DiscountedFare()).get(0).fareCents();
        check(dis < reg, "discounted fare is lower");
        check(tp.plan("Fairview Terminal", "Cubao", new RegularFare()).get(0).km() > 0, "reverse direction works");
        try { tp.plan("Cubao", "Cubao", new RegularFare()); check(false, "same place"); } catch (IllegalArgumentException e) { check(true, "same place rejected"); }
        try { tp.plan("Cubao", "Mars", new RegularFare()); check(false, "unknown"); } catch (IllegalArgumentException e) { check(true, "unknown place rejected"); }
    }
}
