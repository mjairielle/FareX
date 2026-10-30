package FareX;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/** Throwaway. Run from the repo root so data/ resolves. Do not submit this file. */
public class StartupTest {

    static int passed = 0, failed = 0;

    static void check(String label, boolean condition, String detail) {
        if (condition) { passed++; System.out.println("  PASS  " + label); }
        else           { failed++; System.out.println("  FAIL  " + label + "  <- " + detail); }
    }

    public static void main(String[] args) throws IOException {

        // =============== 1. the real data folder ===============
        System.out.println("1. Startup from data/");
        ServerApp app = new ServerApp();
        check("not started before start()", !app.isStarted(), "started early");

        boolean early = false;
        try { app.getTripPlanner(); } catch (IllegalStateException e) { early = true; }
        check("getTripPlanner before start() fails loudly", early, "returned something");

        app.start();
        check("started", app.isStarted(), "not started");
        check("4 routes, 9 stops", app.getRoutes().size() == 4 && app.getStops().size() == 9,
              app.getRoutes().size() + " routes, " + app.getStops().size() + " stops");
        check("shipped data loads with no warnings", app.getWarnings().isEmpty(),
              app.getWarnings().toString());

        TripPlanner first = app.getTripPlanner();
        app.start();
        check("second start() keeps the same planner", app.getTripPlanner() == first, "rebuilt");

        Passenger p = new Passenger("ACC-1", "Juan", PassengerType.SENIOR);
        app.registerPassenger(p);
        List<TripOption> trips = p.planTrip(app.findStop("S01"), app.findStop("S07"));
        check("registered passenger can plan Zapote -> Ayala", !trips.isEmpty(), "no trips");
        check("findStop unknown ID -> null", app.findStop("NOPE") == null, "found something");
        System.out.println();

        // =============== 2. failures are loud ===============
        System.out.println("2. Bad data folders");
        ServerApp missing = new ServerApp("no-such-folder");
        boolean threw = false;
        String msg = "";
        try { missing.start(); } catch (IllegalStateException e) { threw = true; msg = e.getMessage(); }
        check("missing folder throws", threw, "started");
        check("message names the folder", msg.contains("no-such-folder"), msg);
        check("failed start leaves app not started", !missing.isStarted(), "started");

        Path empty = Files.createTempDirectory("farex-empty");
        write(empty, "stops.csv", "stop_id,name,lat,lng\n");
        write(empty, "routes.csv", "route_id,name,base_fare,base_km,per_km_rate,avg_speed_kph,active\n");
        write(empty, "route_stops.csv", "route_id,stop_id,sequence\n");
        threw = false;
        try { new ServerApp(empty.toString()).start(); } catch (IllegalStateException e) { threw = true; }
        check("headers-only data throws 'no usable routes'", threw, "started with nothing");

        // =============== 3. dirty but usable data ===============
        System.out.println();
        System.out.println("3. Dirty data still starts, with warnings");
        Path dirty = Files.createTempDirectory("farex-dirty");
        write(dirty, "stops.csv", "﻿stop_id,name,lat,lng\n"
                + "A,\"Alabang, Muntinlupa\",14.42,121.03\nB,Ayala,14.55,121.02\nC,Bad,x,y\n");
        write(dirty, "routes.csv", "route_id,name,base_fare,base_km,per_km_rate,avg_speed_kph,active\n"
                + "R1,Test,17.00,4.0,2.40,15,true\n");
        write(dirty, "route_stops.csv", "route_id,stop_id,sequence\n"
                + "R1,B,2\nR1,A,1\nR1,C,3\nR9,A,1\n");
        ServerApp dirtyApp = new ServerApp(dirty.toString());
        dirtyApp.start();
        check("started", dirtyApp.isStarted(), "did not start");
        check("BOM + quoted comma: stop A loaded with its full name",
              dirtyApp.findStop("A") != null && dirtyApp.findStop("A").getName().equals("Alabang, Muntinlupa"),
              String.valueOf(dirtyApp.findStop("A")));
        check("3 warnings (bad coords, unknown stop, unknown route)", dirtyApp.getWarnings().size() == 3,
              dirtyApp.getWarnings().toString());
        System.out.println();

        // =============== 4. everything disabled ===============
        System.out.println("4. All routes inactive");
        Path off = Files.createTempDirectory("farex-off");
        write(off, "stops.csv", "stop_id,name,lat,lng\nA,A,14.42,121.03\nB,B,14.55,121.02\n");
        write(off, "routes.csv", "route_id,name,base_fare,base_km,per_km_rate,avg_speed_kph,active\n"
                + "R1,Test,17.00,4.0,2.40,15,false\n");
        write(off, "route_stops.csv", "route_id,stop_id,sequence\nR1,A,1\nR1,B,2\n");
        ServerApp offApp = new ServerApp(off.toString());
        offApp.start();
        Passenger q = new Passenger("ACC-2", "Maria", PassengerType.REGULAR);
        offApp.registerPassenger(q);
        check("starts, but plans nothing", q.planTrip(offApp.findStop("A"), offApp.findStop("B")).isEmpty(),
              "planned a trip on a disabled route");
        check("all-inactive is reported as a warning", offApp.getWarnings().size() == 1,
              offApp.getWarnings().toString());
        System.out.println();

        System.out.println("================================");
        System.out.printf("PASSED %d   FAILED %d%n", passed, failed);
        System.out.println("================================");
        if (failed > 0) System.exit(1);
    }

    static void write(Path dir, String name, String content) throws IOException {
        Files.write(dir.resolve(name), content.getBytes(java.nio.charset.StandardCharsets.UTF_8));
    }
}
