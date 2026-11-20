package com.farex.ui.trip;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.function.Consumer;

import javafx.application.Platform;

import com.farex.Api;
import farex.shared.data.RouteStop;

// Remembers the Trip Planner choices between screens.
// Live: stops and route plans come from the FareX server (Api.routes / Api.plan); start and
// destination can also be pins dropped on the map.
// Offline: if the server cannot be reached, the Figma preview (Quiapo Church -> Rizal Park and
// the three sample routes) is used instead, so the planner still works in a demo.
public final class TripState {

    public static final TripPlace QUIAPO =
            new TripPlace("Quiapo Church", "Manila", "Quezon Boulevard, Manila");
    public static final TripPlace RIZAL_PARK =
            new TripPlace("Rizal Park", "Manila", "Kalaw Avenue, Ermita · in service area");

    // Suggested on "No route found" in the offline preview
    public static final TripPlace KALAW =
            new TripPlace("Kalaw Avenue", "Manila", "Near Rizal Park · walk to your destination");
    public static final TripPlace CITY_HALL =
            new TripPlace("Manila City Hall", "Manila", "Central transfer area · choose another route");

    private static TripPlace start;
    private static TripPlace destination;
    private static int routeIndex = 0;   // which result is picked on "Choose your route"

    // Real stops once loaded; null until then
    private static List<TripPlace> places;
    private static Map<String, String> routeNames = Map.of();   // route id -> "Cubao–Calumpang"
    private static boolean live = false;

    // "Katipunan/LRT–North EDSA (to SM City North EDSA)"; the id if unknown
    public static String routeName(String routeId) {
        return routeNames.getOrDefault(routeId, routeId);
    }

    // Stops whose name contains every word typed (any order, any case); at most `max`
    public static List<TripPlace> search(String query, int max) {
        List<TripPlace> out = new ArrayList<>();
        if (places == null) return out;
        String[] words = query.toLowerCase().trim().split("\\s+");
        for (TripPlace p : places) {
            String name = p.getName().toLowerCase();
            boolean all = true;
            for (String w : words) if (!w.isEmpty() && !name.contains(w)) { all = false; break; }
            if (all) out.add(p);
            if (out.size() == max) break;
        }
        return out;
    }

    // Results of the last search (the samples in the offline preview)
    private static List<TripRoute> routes = TripRoute.SAMPLES;

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

    // ----- Stops -----

    // True when stops and plans come from the server; false = offline preview with sample data
    public static boolean isLive() {
        return live;
    }

    // Gives the stops to pick from, on the FX thread. Fetched once in the background (every stop of
    // every route, with where it is and how many routes serve it); if the server cannot be reached
    // this time, the preview stops are given and the next call retries.
    public static void loadPlaces(Consumer<List<TripPlace>> done) {
        if (places != null) {
            done.accept(places);
            return;
        }
        Thread t = new Thread(() -> {
            List<TripPlace> loaded = new ArrayList<>();
            Map<String, String> names = new HashMap<>();
            try {
                Map<String, double[]> at = new TreeMap<>();
                Map<String, Integer> routesAt = new HashMap<>();
                for (Api.RouteDetail r : Api.routes()) {
                    names.put(r.id(), r.name());
                    Set<String> seen = new HashSet<>();
                    for (RouteStop s : r.stops()) {
                        at.putIfAbsent(s.name(), new double[]{s.lat(), s.lng()});
                        if (seen.add(s.name())) routesAt.merge(s.name(), 1, Integer::sum);
                    }
                }
                at.forEach((name, ll) -> {
                    int n = routesAt.getOrDefault(name, 1);
                    loaded.add(new TripPlace(name, "Quezon City", "On " + n + (n == 1 ? " jeepney route" : " jeepney routes"),
                            ll[0], ll[1], false));
                });
            } catch (RuntimeException offline) {
                loaded.clear();
            }
            Platform.runLater(() -> {
                if (loaded.isEmpty()) {
                    live = false;
                    done.accept(List.of(QUIAPO, RIZAL_PARK));
                } else {
                    places = List.copyOf(loaded);
                    routeNames = Map.copyOf(names);
                    live = true;
                    done.accept(places);
                }
            });
        }, "farex-trip-places");
        t.setDaemon(true);
        t.start();
    }

    // Two other stops to suggest when nothing matched ("No route found")
    public static List<TripPlace> suggestions() {
        if (!live || places == null) return List.of(KALAW, CITY_HALL);
        List<TripPlace> out = new ArrayList<>();
        for (TripPlace p : places) {
            if (out.size() == 2) break;
            if (!same(p, start) && !same(p, destination)) out.add(p);
        }
        return out;
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

    // ----- Results -----

    public static List<TripRoute> getRoutes() {
        return routes;
    }

    public static void setRoutes(List<TripRoute> found) {
        routes = found.isEmpty() ? TripRoute.SAMPLES : List.copyOf(found);
        routeIndex = 0;
    }

    public static int getRouteIndex() {
        return routeIndex;
    }

    public static void setRouteIndex(int index) {
        routeIndex = index;
    }

    // The route picked on "Choose your route"; later "back" buttons return to its details
    public static TripRoute getRoute() {
        return routes.get(Math.min(routeIndex, routes.size() - 1));
    }

    // Both chosen and not the same place
    public static boolean isReady() {
        return start != null && destination != null && !same(start, destination);
    }

    private static boolean same(TripPlace a, TripPlace b) {
        return a != null && b != null && a.getName().equals(b.getName());
    }

    public static void clear() {
        start = null;
        destination = null;
        routeIndex = 0;
        routes = TripRoute.SAMPLES;
    }

    // "Quiapo Church → Rizal Park"
    public static String summary() {
        String from = (start == null) ? "starting point" : start.getName();
        String to = (destination == null) ? "selected destination" : destination.getName();
        return from + " → " + to;
    }
}
