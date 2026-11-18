package com.farex.ui.trip;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

import com.farex.Api;

// One jeepney route option with its stop-by-stop steps. Either a real plan from the FareX server
// (fromPlan, e.g. Philcoa -> Fairview Terminal: 12.8 km, 51 min, ₱31.75) or, when the server
// cannot be reached, one of the Figma samples from Quiapo Church to Rizal Park.
public class TripRoute {

    // One step: "Board at Quiapo Church" + "Quiapo–Kalaw jeepney · estimated ₱13"
    public static class Leg {
        private final String title;
        private final String detail;

        public Leg(String title, String detail) {
            this.title = title;
            this.detail = detail;
        }

        public String getTitle() {
            return title;
        }

        public String getDetail() {
            return detail;
        }
    }

    // ----- The three sample routes from the Figma (offline preview only) -----

    public static final List<TripRoute> SAMPLES = Arrays.asList(
            new TripRoute("Shortest distance", 4.2, 25, 26, "1 transfer · Walk 300 m", "Quiapo–Kalaw jeepney",
                    new Leg("Board at Quiapo Church", "Quiapo–Kalaw jeepney · estimated ₱13"),
                    new Leg("Pass Manila City Hall", "Stay onboard toward Kalaw Avenue"),
                    new Leg("Transfer at Kalaw Avenue", "Kalaw–Taft jeepney · estimated ₱13"),
                    new Leg("Alight near Rizal Park", "Walk 300 m total · 1 transfer")),
            new TripRoute("Fewest transfers", 4.8, 32, 28, "Direct jeepney · Walk 200 m", "Quiapo–Taft jeepney",
                    new Leg("Board at Quiapo Church", "Quiapo–Taft jeepney · estimated ₱28"),
                    new Leg("Pass Manila City Hall", "Stay onboard · no transfer"),
                    new Leg("Continue along Kalaw Avenue", "Check the signboard before boarding"),
                    new Leg("Alight near Rizal Park", "Walk 200 m total · direct ride")),
            new TripRoute("Cheapest fare", 4.6, 35, 24, "1 transfer · Walk 450 m", "Quiapo–Pier jeepney",
                    new Leg("Board at Quiapo Church", "Quiapo–Pier jeepney · estimated ₱12"),
                    new Leg("Transfer at Lawton", "Pier–Kalaw jeepney · estimated ₱12"),
                    new Leg("Pass Intramuros", "Stay onboard toward Kalaw Avenue"),
                    new Leg("Alight near Rizal Park", "Walk 450 m total · 1 transfer")));

    private final String title;
    private final double km;
    private final int minutes;
    private final long fareCents;
    private final String summary;
    private final String jeepney;
    private final List<Leg> legs;
    private final List<Api.PlanRide> rides;   // empty for the samples (they have no map data)

    // Sample route; fare in whole pesos
    public TripRoute(String title, double km, int minutes, int fare, String summary, String jeepney, Leg... legs) {
        this(title, km, minutes, fare * 100L, summary, jeepney, Arrays.asList(legs), List.of());
    }

    private TripRoute(String title, double km, int minutes, long fareCents, String summary, String jeepney,
                      List<Leg> legs, List<Api.PlanRide> rides) {
        this.title = title;
        this.km = km;
        this.minutes = minutes;
        this.fareCents = fareCents;
        this.summary = summary;
        this.jeepney = jeepney;
        this.legs = legs;
        this.rides = rides;
    }

    // A real plan from the server, e.g. "Fewest transfers + Cheapest" with rides R2 then R1:
    //   01 Board at Cubao            Route R2 jeepney to Philcoa · 6.2 km
    //   02 Transfer at Philcoa       Route R1 jeepney to Fairview Terminal · 12.8 km
    //   03 Alight at Fairview Terminal   1 transfer · 19.0 km in total
    public static TripRoute fromPlan(Api.PlanCard plan) {
        List<Leg> legs = new ArrayList<>();
        List<Api.PlanRide> rides = plan.rides();
        for (int i = 0; i < rides.size(); i++) {
            Api.PlanRide r = rides.get(i);
            String km = i < plan.legs().size() ? rideKm(plan.legs().get(i)) : "";
            legs.add(new Leg((i == 0 ? "Board at " : "Transfer at ") + r.from(),
                    "Route " + r.route() + " jeepney to " + r.to() + km));
        }
        String last = rides.isEmpty() ? "your destination" : rides.get(rides.size() - 1).to();
        legs.add(new Leg("Alight at " + last, transfers(plan.transfers()) + " · " + plan.km() + " km in total"));

        List<String> routeIds = new ArrayList<>();
        for (Api.PlanRide r : rides) routeIds.add(r.route());
        String summary = (plan.transfers() == 0 ? "Direct jeepney" : transfers(plan.transfers()))
                + " · " + String.join(" → ", routeIds);
        String jeepney = rides.isEmpty() ? "" : "Route " + rides.get(0).route() + " jeepney";

        return new TripRoute(title(plan.label()), Double.parseDouble(plan.km()), plan.minutes(), plan.fareCents(),
                summary, jeepney, legs, List.copyOf(rides));
    }

    // Server label -> card title. One ranking uses the Figma wording ("Cheapest" -> "Cheapest fare");
    // a plan that wins several keeps the short names so the title fits ("Shortest · Cheapest").
    private static String title(String label) {
        String[] parts = label.split("\\s*\\+\\s*");
        if (parts.length > 1) return String.join(" · ", parts);
        return switch (label.trim()) {
            case "Shortest" -> "Shortest distance";
            case "Cheapest" -> "Cheapest fare";
            default -> label.trim();
        };
    }

    // "R1: Philcoa > Tandang Sora (1.6 km)" -> " · 1.6 km"
    private static String rideKm(String leg) {
        int open = leg.lastIndexOf('(');
        int close = leg.lastIndexOf(')');
        return (open >= 0 && close > open) ? " · " + leg.substring(open + 1, close) : "";
    }

    private static String transfers(int n) {
        return n + (n == 1 ? " transfer" : " transfers");
    }

    public String getTitle() {
        return title;
    }

    public String getSummary() {
        return summary;
    }

    // "Quiapo–Kalaw jeepney" / "Route R1 jeepney" (first ride, shown on the tracking cards)
    public String getJeepney() {
        return jeepney;
    }

    public List<Leg> getLegs() {
        return legs;
    }

    // The rides of a real plan, for drawing it on the map; empty for a sample
    public List<Api.PlanRide> getRides() {
        return rides;
    }

    // "4.2 km · 25 min · ₱26" / "12.8 km · 51 min · ₱31.75"
    public String getStats() {
        return km + " km · " + minutes + " min · " + peso(fareCents);
    }

    // 2600 -> "₱26", 3175 -> "₱31.75"
    static String peso(long cents) {
        return cents % 100 == 0
                ? "₱" + (cents / 100)
                : String.format(Locale.ROOT, "₱%d.%02d", cents / 100, cents % 100);
    }

    // 0 -> "01"
    public static String number(int index) {
        return String.format("%02d", index + 1);
    }
}
