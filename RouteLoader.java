package FareX;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Loads the transit network from three CSV files.
 *
 * Column order is read from each file's HEADER ROW, not assumed by position.
 * Reordering a column in the CSV will not silently corrupt the data.
 *
 *   stops.csv        stop_id,name,lat,lng
 *   routes.csv       route_id,name,base_fare,base_km,per_km_rate,avg_speed_kph,active
 *   route_stops.csv  route_id,stop_id,sequence
 */
public class RouteLoader {

    private final Map<String, Stop> stops = new LinkedHashMap<>();
    private final List<String> warnings = new ArrayList<>();

    public Map<String, Stop> getStops() { return stops; }
    public List<String> getWarnings()   { return warnings; }

    /** Load everything and return the routes, ready for RouteGraph.build(). */
    public List<Route> load(String stopsCsv, String routesCsv, String routeStopsCsv)
            throws IOException {

        loadStops(stopsCsv);
        Map<String, RouteSpec> specs = loadRouteSpecs(routesCsv);
        attachStops(routeStopsCsv, specs);

        List<Route> routes = new ArrayList<>();
        for (RouteSpec spec : specs.values()) {
            if (spec.stops.size() < 2) {
                warnings.add("route " + spec.routeID + " has fewer than 2 stops, skipped");
                continue;
            }
            Route route = new Route(spec.routeID, spec.name, spec.stops,
                    spec.baseFare, spec.baseKm, spec.perKmRate, spec.avgSpeedKph);
            route.setActive(spec.active);
            routes.add(route);
        }
        return routes;
    }

    private void loadStops(String path) throws IOException {
        for (Map<String, String> row : readCsv(path)) {
            String id = row.get("stop_id");
            if (id == null || id.isEmpty()) continue;

            try {
                stops.put(id, new Stop(
                        id,
                        row.getOrDefault("name", id),
                        Double.parseDouble(row.get("lat")),
                        Double.parseDouble(row.get("lng"))));
            } catch (NumberFormatException | NullPointerException e) {
                warnings.add("stop " + id + " has bad coordinates, skipped");
            }
        }
    }

    private Map<String, RouteSpec> loadRouteSpecs(String path) throws IOException {
        Map<String, RouteSpec> specs = new LinkedHashMap<>();

        for (Map<String, String> row : readCsv(path)) {
            String id = row.get("route_id");
            if (id == null || id.isEmpty()) continue;

            RouteSpec spec = new RouteSpec();
            spec.routeID = id;
            spec.name = row.getOrDefault("name", id);
            spec.baseFare    = parseDouble(row.get("base_fare"), 15.00, id, "base_fare");
            spec.baseKm      = parseDouble(row.get("base_km"), 4.0, id, "base_km");
            spec.perKmRate   = parseDouble(row.get("per_km_rate"), 2.20, id, "per_km_rate");
            spec.avgSpeedKph = parseDouble(row.get("avg_speed_kph"), 15.0, id, "avg_speed_kph");

            String active = row.get("active");
            spec.active = (active == null) || active.trim().equalsIgnoreCase("true")
                                           || active.trim().equals("1");
            specs.put(id, spec);
        }
        return specs;
    }

    private void attachStops(String path, Map<String, RouteSpec> specs) throws IOException {
        // Sort each route's stops by the sequence column rather than trusting
        // the row order in the file.
        Map<String, List<int[]>> ordering = new HashMap<>();
        Map<String, List<String>> stopIDs = new HashMap<>();

        List<Map<String, String>> rows = readCsv(path);

        for (Map<String, String> row : rows) {
            String routeID = row.get("route_id");
            String stopID = row.get("stop_id");
            if (routeID == null || stopID == null) continue;

            if (!specs.containsKey(routeID)) {
                warnings.add("route_stops references unknown route " + routeID);
                continue;
            }
            if (!stops.containsKey(stopID)) {
                warnings.add("route_stops references unknown stop " + stopID);
                continue;
            }

            int seq;
            try {
                seq = Integer.parseInt(row.get("sequence").trim());
            } catch (NumberFormatException | NullPointerException e) {
                warnings.add("bad sequence for " + routeID + "/" + stopID + ", skipped");
                continue;
            }

            ordering.computeIfAbsent(routeID, k -> new ArrayList<>())
                    .add(new int[]{seq, stopIDs.computeIfAbsent(routeID, k -> new ArrayList<>()).size()});
            stopIDs.get(routeID).add(stopID);
        }

        for (Map.Entry<String, List<int[]>> entry : ordering.entrySet()) {
            String routeID = entry.getKey();
            List<int[]> pairs = entry.getValue();
            pairs.sort((x, y) -> Integer.compare(x[0], y[0]));

            List<String> ids = stopIDs.get(routeID);
            RouteSpec spec = specs.get(routeID);
            Set<String> placed = new HashSet<>();
            for (int[] pair : pairs) {
                String stopID = ids.get(pair[1]);
                // Route cannot represent a stop visited twice (loop routes),
                // so keep the first visit and report the rest.
                if (!placed.add(stopID)) {
                    warnings.add("route " + routeID + " visits stop " + stopID
                            + " more than once; loop routes are not supported, later visit skipped");
                    continue;
                }
                spec.stops.add(stops.get(stopID));
            }
        }
    }

    private double parseDouble(String value, double fallback, String routeID, String column) {
        try {
            return Double.parseDouble(value.trim());
        } catch (NumberFormatException | NullPointerException e) {
            warnings.add("route " + routeID + ": bad " + column + ", using " + fallback);
            return fallback;
        }
    }

    // ---------------------------------------------------------------
    // CSV reading
    // ---------------------------------------------------------------

    /**
     * Read a CSV into a list of header-keyed maps.
     * Blank lines and lines starting with # are ignored.
     */
    private List<Map<String, String>> readCsv(String path) throws IOException {
        List<Map<String, String>> rows = new ArrayList<>();

        try (BufferedReader reader = new BufferedReader(new FileReader(path))) {
            String headerLine = reader.readLine();
            if (headerLine == null) {
                warnings.add(path + " is empty");
                return rows;
            }

            // Strip a UTF-8 BOM if Excel added one - it silently breaks the
            // first column name and is painful to spot by eye.
            if (!headerLine.isEmpty() && headerLine.charAt(0) == '﻿') {
                headerLine = headerLine.substring(1);
            }

            String[] headers = splitCsvLine(headerLine);
            for (int i = 0; i < headers.length; i++) {
                headers[i] = headers[i].trim().toLowerCase();
            }

            String line;
            int lineNo = 1;
            while ((line = reader.readLine()) != null) {
                lineNo++;
                if (line.trim().isEmpty() || line.trim().startsWith("#")) continue;

                String[] values = splitCsvLine(line);
                if (values.length != headers.length) {
                    warnings.add(path + " line " + lineNo + ": expected "
                            + headers.length + " columns, got " + values.length + ", skipped");
                    continue;
                }

                Map<String, String> row = new HashMap<>();
                for (int i = 0; i < headers.length; i++) {
                    row.put(headers[i], values[i].trim());
                }
                rows.add(row);
            }
        }
        return rows;
    }

    /**
     * Split one CSV line, respecting double quotes so a stop name containing
     * a comma ("Alabang, Muntinlupa") does not break the row.
     * String.split(",") cannot do this.
     */
    private String[] splitCsvLine(String line) {
        List<String> fields = new ArrayList<>();
        StringBuilder field = new StringBuilder();
        boolean inQuotes = false;

        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);

            if (c == '"') {
                if (inQuotes && i + 1 < line.length() && line.charAt(i + 1) == '"') {
                    field.append('"');   // escaped quote ""
                    i++;
                } else {
                    inQuotes = !inQuotes;
                }
            } else if (c == ',' && !inQuotes) {
                fields.add(field.toString());
                field.setLength(0);
            } else {
                field.append(c);
            }
        }
        fields.add(field.toString());
        return fields.toArray(new String[0]);
    }

    /** Mutable holder used only while loading. */
    private static class RouteSpec {
        String routeID;
        String name;
        double baseFare, baseKm, perKmRate, avgSpeedKph;
        boolean active = true;
        final List<Stop> stops = new ArrayList<>();
    }
}
