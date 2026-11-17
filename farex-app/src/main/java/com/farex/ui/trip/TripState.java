package com.farex.ui.trip;

// Remembers the Trip Planner choices between screens. Sample stops only (no real map data yet).
public final class TripState {

    public static final TripPlace QUIAPO =
            new TripPlace("Quiapo Church", "Manila", "Quezon Boulevard, Manila");
    public static final TripPlace RIZAL_PARK =
            new TripPlace("Rizal Park", "Manila", "Kalaw Avenue, Ermita · in service area");

    // Suggested on "No route found"
    public static final TripPlace KALAW =
            new TripPlace("Kalaw Avenue", "Manila", "Near Rizal Park · walk to your destination");
    public static final TripPlace CITY_HALL =
            new TripPlace("Manila City Hall", "Manila", "Central transfer area · choose another route");

    private static TripPlace start;
    private static TripPlace destination;
    private static int routeIndex = 0;   // 0 = Shortest distance, 1 = Fewest transfers, 2 = Cheapest fare

    // Answer to "Allow FareX to use your location?"; kept until the app closes
    private static boolean locationAllowed = false;

    public static boolean isLocationAllowed() {
        return locationAllowed;
    }

    public static void setLocationAllowed(boolean allowed) {
        locationAllowed = allowed;
    }

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

    public static int getRouteIndex() {
        return routeIndex;
    }

    public static void setRouteIndex(int index) {
        routeIndex = index;
    }

    // The route picked on "Choose your route"; later "back" buttons return to its details
    public static TripRoute getRoute() {
        return TripRoute.SAMPLES.get(routeIndex);
    }

    // Both chosen and not the same place
    public static boolean isReady() {
        return start != null && destination != null && start != destination;
    }

    public static void clear() {
        start = null;
        destination = null;
        routeIndex = 0;
    }

    // "Quiapo Church → Rizal Park"
    public static String summary() {
        String from = (start == null) ? "starting point" : start.getName();
        String to = (destination == null) ? "selected destination" : destination.getName();
        return from + " → " + to;
    }
}
