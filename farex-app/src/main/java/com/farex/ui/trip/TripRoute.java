package com.farex.ui.trip;

import java.util.Arrays;
import java.util.List;

// One sample jeepney route from Quiapo Church to Rizal Park, with its stop-by-stop steps.
// Example: 4.2 km, 25 min, ₱26, "1 transfer · Walk 300 m"
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

    // ----- The three sample routes from the Figma -----

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
    private final int fare;
    private final String summary;
    private final String jeepney;
    private final List<Leg> legs;

    public TripRoute(String title, double km, int minutes, int fare, String summary, String jeepney, Leg... legs) {
        this.title = title;
        this.km = km;
        this.minutes = minutes;
        this.fare = fare;
        this.summary = summary;
        this.jeepney = jeepney;
        this.legs = Arrays.asList(legs);
    }

    public String getTitle() {
        return title;
    }

    public String getSummary() {
        return summary;
    }

    // "Quiapo–Kalaw jeepney" (first ride, shown on the tracking cards)
    public String getJeepney() {
        return jeepney;
    }

    public List<Leg> getLegs() {
        return legs;
    }

    // "4.2 km · 25 min · ₱26"
    public String getStats() {
        return km + " km · " + minutes + " min · ₱" + fare;
    }

    // 0 -> "01"
    public static String number(int index) {
        return String.format("%02d", index + 1);
    }
}
