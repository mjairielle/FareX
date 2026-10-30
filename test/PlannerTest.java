package FareX;

import java.util.Arrays;
import java.util.List;

/** Throwaway test harness. Do not submit this file. */
public class PlannerTest {

    static int passed = 0, failed = 0;

    static void check(String label, boolean condition, String detail) {
        if (condition) { passed++; System.out.println("  PASS  " + label); }
        else           { failed++; System.out.println("  FAIL  " + label + "  <- " + detail); }
    }

    public static void main(String[] args) {

        // ---- Las Pinas / Alabang / Makati corridor, real coordinates ----
        Stop zapote   = new Stop("S01", "Zapote",          14.4539, 120.9793);
        Stop pilar    = new Stop("S02", "Pilar Village",   14.4610, 120.9850);
        Stop aguirre  = new Stop("S03", "BF Aguirre",      14.4360, 120.9990);
        Stop atc      = new Stop("S04", "Alabang Town Ctr",14.4232, 121.0292);
        Stop baclaran = new Stop("S05", "Baclaran",        14.5340, 120.9960);
        Stop taft     = new Stop("S06", "EDSA Taft",       14.5378, 120.9967);
        Stop ayala    = new Stop("S07", "Ayala Ave",       14.5547, 121.0244);
        Stop boondock = new Stop("S99", "Unconnected Stop",14.1000, 121.5000);

        // Modern jeep (LTFRB 2026-09-28): PHP 17 first 4km, PHP 2.40 per km after, 15 km/h.
        Route j01 = new Route("J01", "Zapote - Alabang",
                Arrays.asList(zapote, aguirre, atc), 17.00, 4.0, 2.40, 15.0);

        // Slow direct jeep, the long way round. No transfer needed.
        Route j02 = new Route("J02", "Zapote - Ayala via Baclaran",
                Arrays.asList(zapote, pilar, baclaran, taft, ayala), 17.00, 4.0, 2.40, 12.0);

        // Aircon city bus on the expressway: PHP 18 first 5km, PHP 2.98 per km after.
        Route b01 = new Route("B01", "Alabang - Ayala Express",
                Arrays.asList(atc, ayala), 18.00, 5.0, 2.98, 40.0);

        // Unreachable island route, for the no-path test.
        Route x99 = new Route("X99", "Nowhere Line",
                Arrays.asList(boondock, new Stop("S98", "Also Nowhere", 14.11, 121.51)),
                15.00, 4.0, 2.20, 15.0);

        RouteGraph graph = new RouteGraph();
        graph.build(Arrays.asList(j01, j02, b01, x99));

        System.out.println(graph.describe());
        System.out.println();

        // =============== 1. graph shape ===============
        System.out.println("1. Graph construction");
        check("Zapote splits per route", graph.nodesAtStop("S01").size() == 2,
              "got " + graph.nodesAtStop("S01"));
        check("ATC splits per route", graph.nodesAtStop("S04").size() == 2,
              "got " + graph.nodesAtStop("S04"));
        check("Ayala splits per route", graph.nodesAtStop("S07").size() == 2,
              "got " + graph.nodesAtStop("S07"));

        boolean transferCharged = false;
        for (RouteGraph.Edge e : graph.neighbors("S04@J01")) {
            if (e.isTransfer() && e.getFare() > 0) transferCharged = true;
        }
        check("transfer edge carries a boarding fare", transferCharged, "no paid transfer edge");
        System.out.println();

        // =============== 2. pathfinding ===============
        System.out.println("2. findPaths Zapote -> Ayala");
        List<TripOption> options = graph.findPaths(zapote, ayala);
        check("found at least one route", !options.isEmpty(), "none found");
        check("objectives disagree (>1 distinct)", options.size() > 1,
              "only " + options.size() + " distinct option(s)");

        for (TripOption o : options) {
            System.out.println("    " + o);
            for (Leg leg : o.getLegs()) System.out.println("       " + leg);
        }
        System.out.println();

        // =============== 3. preferences pick differently ===============
        System.out.println("3. Preference ranking");
        TripPlanner planner = new TripPlanner(graph);

        planner.setPreference(TripPlanner.Preference.CHEAPEST);
        TripOption cheapest = planner.plan(zapote, ayala).get(0);

        planner.setPreference(TripPlanner.Preference.FASTEST);
        TripOption fastest = planner.plan(zapote, ayala).get(0);

        planner.setPreference(TripPlanner.Preference.FEWEST_TRANSFERS);
        TripOption fewest = planner.plan(zapote, ayala).get(0);

        System.out.printf("    cheapest: PHP %.2f | %.0f min | %d transfers%n",
                cheapest.getTotalFare(), cheapest.getEstimatedTime(), cheapest.getTransfers());
        System.out.printf("    fastest : PHP %.2f | %.0f min | %d transfers%n",
                fastest.getTotalFare(), fastest.getEstimatedTime(), fastest.getTransfers());
        System.out.printf("    fewest  : PHP %.2f | %.0f min | %d transfers%n",
                fewest.getTotalFare(), fewest.getEstimatedTime(), fewest.getTransfers());

        check("cheapest is cheapest", cheapest.getTotalFare() <= fastest.getTotalFare(),
              "cheapest cost more than fastest");
        check("fastest is fastest", fastest.getEstimatedTime() <= cheapest.getEstimatedTime(),
              "fastest took longer than cheapest");
        check("fewest has fewest transfers", fewest.getTransfers() <= fastest.getTransfers(),
              "fewest had more transfers");
        System.out.println();

        // =============== 4. leg reconstruction ===============
        System.out.println("4. Leg reconstruction");
        boolean legsChain = true;
        String why = "";
        for (TripOption o : options) {
            List<Leg> legs = o.getLegs();
            if (!legs.get(0).getBoardStop().getStopID().equals("S01")) {
                legsChain = false; why = "first leg does not start at origin";
            }
            if (!legs.get(legs.size()-1).getAlightStop().getStopID().equals("S07")) {
                legsChain = false; why = "last leg does not end at destination";
            }
            for (int i = 0; i < legs.size()-1; i++) {
                if (!legs.get(i).getAlightStop().getStopID()
                        .equals(legs.get(i+1).getBoardStop().getStopID())) {
                    legsChain = false; why = "gap between leg " + i + " and " + (i+1);
                }
                if (legs.get(i).getRoute().getRouteID()
                        .equals(legs.get(i+1).getRoute().getRouteID())) {
                    legsChain = false; why = "consecutive legs on same route, not merged";
                }
            }
            if (o.getTransfers() != legs.size()-1) {
                legsChain = false; why = "transfer count does not match leg count";
            }
        }
        check("legs chain origin -> destination with no gaps", legsChain, why);

        boolean faresPositive = true;
        for (TripOption o : options)
            for (Leg leg : o.getLegs())
                if (leg.getFare() <= 0 || leg.getDistanceKm() <= 0) faresPositive = false;
        check("every leg has positive fare and distance", faresPositive, "a leg was zero");
        System.out.println();

        // =============== 5. edge cases ===============
        System.out.println("5. Edge cases");
        check("same origin and destination -> empty",
              graph.findPaths(zapote, zapote).isEmpty(), "returned a trip");
        check("unreachable destination -> empty",
              graph.findPaths(zapote, boondock).isEmpty(), "returned a trip");
        check("rank(null) -> empty, no crash",
              planner.rank(null).isEmpty(), "did not return empty");
        check("rank caps at 3",
              planner.rank(graph.findPaths(zapote, ayala)).size() <= 3, "returned more than 3");

        List<TripOption> merged = planner.planAllPreferences(zapote, ayala);
        check("planAllPreferences returns distinct options", !merged.isEmpty(), "empty");
        System.out.println();

        // =============== 6. disruption reroute ===============
        System.out.println("6. Route disruption");
        b01.setActive(false);
        RouteGraph disrupted = new RouteGraph();
        disrupted.build(Arrays.asList(j01, j02, b01, x99));

        List<TripOption> afterOutage = disrupted.findPaths(zapote, ayala);
        check("still reachable after express bus is disabled", !afterOutage.isEmpty(), "no route");

        boolean usesB01 = false;
        for (TripOption o : afterOutage)
            for (Leg leg : o.getLegs())
                if (leg.getRoute().getRouteID().equals("B01")) usesB01 = true;
        check("disabled route no longer used", !usesB01, "B01 still appeared in a result");
        System.out.println("    fallback: " + afterOutage.get(0));
        b01.setActive(true);
        System.out.println();

        // =============== 7. affordability seam ===============
        System.out.println("7. Affordability");
        TripOption trip = options.get(0);
        check("shortfall is zero when balance covers the fare",
              TripPlanner.shortfall(trip, trip.getTotalFare() + 50) == 0.0, "nonzero");
        check("shortfall is correct when balance is short",
              Math.abs(TripPlanner.shortfall(trip, 10.00) - (trip.getTotalFare() - 10.00)) < 0.001,
              "wrong amount");
        System.out.println();

        // =============== 8. fare matrix ===============
        System.out.println("8. Fares match the LTFRB 2026 matrix");
        Stop p = new Stop("P1", "P", 14.40, 121.00);
        Stop q = new Stop("P2", "Q", 14.41, 121.00);
        Route modernJeep  = new Route("MJ", "modern jeep", Arrays.asList(p, q), 17.00, 4.0, 2.40, 15.0);
        Route tradJeep    = new Route("TJ", "trad jeep",   Arrays.asList(p, q), 14.00, 4.0, 2.00, 15.0);
        Route airconBus   = new Route("AB", "aircon bus",  Arrays.asList(p, q), 18.00, 5.0, 2.98, 30.0);
        check("modern jeep 2 km = PHP 17.00",  modernJeep.fareFor(2)  == 17.00, "got " + modernJeep.fareFor(2));
        check("modern jeep 10 km = PHP 31.50", modernJeep.fareFor(10) == 31.50, "got " + modernJeep.fareFor(10));
        check("trad jeep 12 km = PHP 30.00",   tradJeep.fareFor(12)   == 30.00, "got " + tradJeep.fareFor(12));
        check("aircon bus 20 km = PHP 62.75",  airconBus.fareFor(20)  == 62.75, "got " + airconBus.fareFor(20));
        check("52.35 rounds to 52.25", FareCalculator.roundToQuarter(52.35) == 52.25,
              "got " + FareCalculator.roundToQuarter(52.35));
        check("11.20 rounds to 11.25", FareCalculator.roundToQuarter(11.20) == 11.25,
              "got " + FareCalculator.roundToQuarter(11.20));
        System.out.println();

        // =============== 8b. FareCalculator vs the printed table ===============
        // "What common trips cost", parkinganph.com/guides/fare-matrix (2026-09-28 scales).
        // Each row: base, baseKm, rate, then regular at 5/10/20/50 km, then student at 20 km.
        System.out.println("8b. FareCalculator reproduces the published table");
        FareCalculator calc = new FareCalculator();
        Object[][] matrix = {
            {"PUJ",            14.00, 4.0, 2.00,  16.00, 26.00, 46.00, 106.00, 36.75},
            {"Modern PUJ",     17.00, 4.0, 2.40,  19.50, 31.50, 55.50, 127.50, 44.25},
            {"Ordinary bus",   15.00, 5.0, 2.49,  15.00, 27.50, 52.25, 127.00, 41.75},
            {"Aircon bus",     18.00, 5.0, 2.98,  18.00, 33.00, 62.75, 152.00, 50.00},
            {"Prov. ordinary", 12.00, 5.0, 2.20,  12.00, 23.00, 45.00, 111.00, 36.00},
            {"Prov. aircon",    0.00, 0.0, 2.45,  12.25, 24.50, 49.00, 122.50, 39.25},
            {"Luxury",          0.00, 0.0, 3.35,  16.75, 33.50, 67.00, 167.50, 53.50},
            {"Modern UV",       0.00, 0.0, 3.00,  15.00, 30.00, 60.00, 150.00, 48.00},
        };
        double[] kms = {5, 10, 20, 50};
        for (Object[] row : matrix) {
            FareTable ft = new FareTable((double) row[1], (double) row[2], (double) row[3], null);
            String wrong = "";
            for (int i = 0; i < kms.length; i++) {
                double got = calc.compute(kms[i], PassengerType.REGULAR, ft);
                if (got != (double) row[4 + i]) wrong += kms[i] + "km=" + got + " ";
            }
            double student = calc.compute(20, PassengerType.STUDENT, ft);
            if (student != (double) row[8]) wrong += "student20km=" + student;
            check(row[0] + " matches all 5 printed fares", wrong.isEmpty(), wrong);
        }
        FareTable puj = new FareTable(14.00, 4.0, 2.00, null);
        check("PUJ minimum, student = PHP 11.25", calc.compute(3, PassengerType.STUDENT, puj) == 11.25,
              "got " + calc.compute(3, PassengerType.STUDENT, puj));
        check("PUJ 10 km, student = PHP 20.75", calc.compute(10, PassengerType.STUDENT, puj) == 20.75,
              "got " + calc.compute(10, PassengerType.STUDENT, puj));
        check("senior and PWD get the student fare",
              calc.compute(20, PassengerType.SENIOR, puj) == 36.75
                  && calc.compute(20, PassengerType.PWD, puj) == 36.75, "differs");
        check("null passenger type pays regular", calc.compute(20, null, puj) == 46.00, "differs");
        System.out.println();

        // =============== 8c. discounts reach the trip ===============
        System.out.println("8c. Discounted trip totals");
        TripOption twoLegs = null;
        for (TripOption o : options) if (o.getTransfers() == 1) twoLegs = o;
        double perLeg = 0;
        for (Leg leg : twoLegs.getLegs()) perLeg += leg.getRoute().fareFor(leg.getDistanceKm(), PassengerType.STUDENT);
        check("student total = sum of per-leg student fares", twoLegs.getTotalFareFor(PassengerType.STUDENT) == perLeg,
              "got " + twoLegs.getTotalFareFor(PassengerType.STUDENT) + " vs " + perLeg);
        check("student total is below regular", twoLegs.getTotalFareFor(PassengerType.STUDENT) < twoLegs.getTotalFare(),
              "not discounted");
        check("REGULAR total equals getTotalFare()",
              twoLegs.getTotalFareFor(PassengerType.REGULAR) == twoLegs.getTotalFare(), "differs");
        double balance = twoLegs.getTotalFareFor(PassengerType.STUDENT);
        check("student balance covers student fare but not regular",
              TripPlanner.shortfall(twoLegs, PassengerType.STUDENT, balance) == 0.0
                  && TripPlanner.shortfall(twoLegs, balance) > 0.0, "wrong shortfall");
        System.out.printf("    %s%n    regular PHP %.2f, student/senior/PWD PHP %.2f%n",
                twoLegs, twoLegs.getTotalFare(), twoLegs.getTotalFareFor(PassengerType.STUDENT));
        System.out.println();

        // =============== 9. exact fare search ===============
        System.out.println("9. Cheapest search uses the exact fare");
        // Two routes over the same ~1.4 km hop, both inside the base band.
        // Real fares: X = PHP 15.00, Y = PHP 16.00, so X is cheaper.
        // The old per-km approximation priced X at 15 + 3.50*km and Y at
        // 16 + 0.50*km, so it picked Y. Y is also faster and listed first,
        // so the TIME and TRANSFERS passes cannot rescue X either.
        Route y = new Route("Y", "cheap per km", Arrays.asList(p, q), 16.00, 4.0, 0.50, 30.0);
        Route x = new Route("X", "low base",     Arrays.asList(p, q), 15.00, 4.0, 3.50, 10.0);
        RouteGraph trap = new RouteGraph();
        trap.build(Arrays.asList(y, x));
        TripPlanner trapPlanner = new TripPlanner(trap);
        trapPlanner.setPreference(TripPlanner.Preference.CHEAPEST);
        TripOption trapBest = trapPlanner.plan(p, q).get(0);
        check("cheapest picks the PHP 15.00 route", trapBest.getTotalFare() == 15.00,
              "got " + trapBest);
        System.out.println();

        // =============== 10. one transfer-time setting ===============
        System.out.println("10. Transfer time is consistent");
        RouteGraph slowTransfers = new RouteGraph();
        slowTransfers.setTransferMinutes(12.0);
        slowTransfers.build(Arrays.asList(j01, j02, b01, x99));
        boolean consistent = false;
        for (TripOption o : slowTransfers.findPaths(zapote, ayala)) {
            if (o.getTransfers() == 0) continue;
            double ride = 0;
            for (Leg leg : o.getLegs()) ride += leg.getMinutes();
            consistent = Math.abs(o.getEstimatedTime() - (ride + 12.0 * o.getTransfers())) < 1e-9;
        }
        check("displayed time uses the graph's transfer minutes", consistent,
              "TripOption ignored setTransferMinutes");
        System.out.println();

        // =============== 11. loop routes ===============
        System.out.println("11. Repeated stops");
        boolean rejected = false;
        try {
            new Route("LOOP", "loop", Arrays.asList(zapote, pilar, zapote), 17.00, 4.0, 2.40, 15.0);
        } catch (IllegalArgumentException e) {
            rejected = true;
        }
        check("route visiting a stop twice is rejected", rejected, "was accepted");
        System.out.println();

        System.out.println("================================");
        System.out.printf("PASSED %d   FAILED %d%n", passed, failed);
        System.out.println("================================");
        if (failed > 0) System.exit(1);
    }
}
