package com.farex.ui.trip;

// One place in the Trip Planner: a FareX stop ("Philcoa", "Quezon City", "On 4 jeepney routes"),
// a pin dropped on the map, or a Figma sample stop ("Quiapo Church", "Manila", ...).
// Stops and pins know where they are; the samples do not (lat/lng NaN).
public class TripPlace {

    private final String name;
    private final String city;
    private final String address;
    private final double lat;
    private final double lng;
    private final boolean pin;

    public TripPlace(String name, String city, String address) {
        this(name, city, address, Double.NaN, Double.NaN, false);
    }

    public TripPlace(String name, String city, String address, double lat, double lng, boolean pin) {
        this.name = name;
        this.city = city;
        this.address = address;
        this.lat = lat;
        this.lng = lng;
        this.pin = pin;
    }

    // A point the user tapped on the map, e.g. "Pinned start" at 14.6532, 121.0681
    public static TripPlace pinned(String name, double lat, double lng) {
        return new TripPlace(name, "Quezon City",
                String.format(java.util.Locale.ROOT, "Map pin · %.5f, %.5f", lat, lng), lat, lng, true);
    }

    public String getName() {
        return name;
    }

    public String getAddress() {
        return address;
    }

    public double getLat() {
        return lat;
    }

    public double getLng() {
        return lng;
    }

    // A map pin, not a stop: the trip starts or ends with a walk
    public boolean isPin() {
        return pin;
    }

    // "Quiapo Church, Manila"
    public String getFullName() {
        return name + ", " + city;
    }
}
