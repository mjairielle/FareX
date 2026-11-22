package com.farex.ui;

// One stop in the Trip Planner, e.g. "Quiapo Church", "Manila", "Quezon Boulevard, Manila"
public class TripPlace {

    private final String name;
    private final String city;
    private final String address;

    public TripPlace(String name, String city, String address) {
        this.name = name;
        this.city = city;
        this.address = address;
    }

    public String getName() {
        return name;
    }

    public String getAddress() {
        return address;
    }

    // "Quiapo Church, Manila"
    public String getFullName() {
        return name + ", " + city;
    }
}
