package com.farex.ui;

// Remembers the Trip Planner choices between screens. Sample stops only (no real map data yet).
public final class TripState {

    public static final TripPlace QUIAPO =
            new TripPlace("Quiapo Church", "Manila", "Quezon Boulevard, Manila");
    public static final TripPlace RIZAL_PARK =
            new TripPlace("Rizal Park", "Manila", "Kalaw Avenue, Ermita · in service area");

    private static TripPlace start;
    private static TripPlace destination;

    private TripState() {
    }

    public static TripPlace getStart() {
        return start;
    }

    public static void setStart(TripPlace place) {
        start = place;
    }

    public static TripPlace getDestination() {
        return destination;
    }

    public static void setDestination(TripPlace place) {
        destination = place;
    }

    // Both chosen and not the same place
    public static boolean isReady() {
        return start != null && destination != null && start != destination;
    }

    public static void clear() {
        start = null;
        destination = null;
    }

    // "Quiapo Church → Rizal Park"
    public static String summary() {
        String from = (start == null) ? "starting point" : start.getName();
        String to = (destination == null) ? "selected destination" : destination.getName();
        return from + " → " + to;
    }
}
