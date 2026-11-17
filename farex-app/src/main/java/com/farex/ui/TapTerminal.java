package com.farex.ui;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import com.farex.Api;
import com.farex.operator.GpsSimulator;

import farex.shared.fare.Fares;
import farex.shared.fare.RegularFare;

// The jeep's tap reader (no UI). Same rules as farex-shared's TapDesk, but on the demo cards
// so it works without the server:
// - first tap = tap in at the current stop, second tap = tap out and pay
// - fare = farex-shared Fares.fare(km, rule): ₱13 + ₱2 per km, students 20% off
// - still on board when the jeep leaves the terminal (New trip) or the shift ends = max fare
final class TapTerminal {

    private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("h:mm a", Locale.US);

    // Kenn's card is a student card; every other demo card is regular
    private static final Set<String> STUDENT_CARDS = Set.of("0011111111");

    // App users have no card: their balance is on the server
    private static final List<TapRider> APP_RIDERS = List.of(
            new TapRider("APP-0821", "App user", "REGULAR", 240.00),
            new TapRider("APP-0632", "App user", "STUDENT", 85.00));

    // Who is on board: rider ID -> where they boarded
    private record Boarding(TapRider rider, int stopIdx, String stopName) {
    }

    private static final Map<String, Boarding> boarded = new LinkedHashMap<>();

    // Max fares charged when leaving the terminal, waiting for Ready Mode to show them
    private static TapResult unseenMaxFare = null;

    private TapTerminal() {
    }

    // ----- Riders -----

    // Every demo card (Aron 6767, Kenn 1111, Andrei 1313, ...) and the two app users
    static List<TapRider> riders() {
        List<TapRider> list = new ArrayList<>();
        for (DemoCard card : MockData.allCards().keySet()) {
            list.add(riderFor(card));
        }
        list.addAll(APP_RIDERS);
        return list;
    }

    private static TapRider riderFor(DemoCard card) {
        Boarding onBoard = boarded.get(card.getCardNumber());
        if (onBoard != null) {
            return onBoard.rider();
        }
        return new TapRider(card, STUDENT_CARDS.contains(card.getCardNumber()) ? "STUDENT" : "REGULAR");
    }

    static boolean isOnBoard(TapRider rider) {
        return boarded.containsKey(rider.getId());
    }

    // "Philcoa", or null when not on board
    static String boardedAt(TapRider rider) {
        Boarding b = boarded.get(rider.getId());
        return (b == null) ? null : b.stopName();
    }

    static int onBoardCount() {
        return boarded.size();
    }

    // ----- Tapping -----

    static TapResult tap(TapRider rider) {
        return isOnBoard(rider) ? tapOut(rider) : tapIn(rider);
    }

    private static TapResult tapIn(TapRider rider) {
        GpsSimulator gps = OperatorState.getGps();
        Api.StopCoord stop = gps.getCurrentStop();

        if (rider.isFrozen()) {
            return TapResult.blocked("Card is frozen",
                    "This card was reported lost. Ask the passenger to use their new card.");
        }
        if (OperatorState.isAtTerminal()) {
            return TapResult.blocked("End of route",
                    "Tap New trip on the GPS card before boarding passengers.");
        }
        if (onBoardCount() >= OperatorState.CAPACITY) {
            return TapResult.blocked("Jeepney is full",
                    OperatorState.CAPACITY + " of " + OperatorState.CAPACITY
                            + " seats are taken. Wait for someone to tap out.");
        }

        boolean offlineApp = rider.isApp() && !OperatorState.isOnline();
        // Must at least cover the shortest ride: ₱13.00 regular, ₱10.40 student
        if (!offlineApp && !rider.canPay(Fares.fare(0, rider.getFareRule()))) {
            return TapResult.insufficient();
        }

        boarded.put(rider.getId(), new Boarding(rider, stop.index(), stop.name()));
        OperatorState.addBoarded();

        if (offlineApp) {
            return TapResult.appOffline(rider, stop.name());
        }
        String note = rider.isApp() ? "Server confirmed · GPS saved"
                : "Logged: " + LocalTime.now().format(TIME) + " · GPS saved";
        return TapResult.tapIn(rider, stop.name(), note);
    }

    // Philcoa (0) -> Ever Gotesco (2): 3.29 km -> regular ₱19.57, student ₱15.66
    private static TapResult tapOut(TapRider rider) {
        Boarding b = boarded.get(rider.getId());
        Api.StopCoord stop = OperatorState.getGps().getCurrentStop();
        double km = pathKm(b.stopIdx(), stop.index());
        long fare = Fares.fare(km, rider.getFareRule());
        long base = Fares.fare(km, new RegularFare());

        boolean offlineApp = rider.isApp() && !OperatorState.isOnline();
        if (!offlineApp && !rider.canPay(fare)) {
            return TapResult.insufficient();    // stays on board, can try again after topping up
        }

        ShiftRecord record = charge(rider, b.stopName(), stop.name(), km, fare, base, false, offlineApp);
        boarded.remove(rider.getId());
        return TapResult.tapOut(record);
    }

    // ----- Max fare -----

    // Called by "New trip" and "End shift": everyone still on board pays the whole route
    // at the regular fare (like TapDesk.missedTapOut), e.g. Philcoa -> Fairview 13.53 km = ₱40.05
    static void chargeMaxFares() {
        List<Api.StopCoord> stops = OperatorState.getGps().getRouteStops();
        double km = pathKm(0, stops.size() - 1);
        long fare = Fares.fare(km, new RegularFare());

        ShiftRecord first = null;
        for (Boarding b : boarded.values()) {
            boolean offlineApp = b.rider().isApp() && !OperatorState.isOnline();
            ShiftRecord record = charge(b.rider(), b.stopName(), "Route end", km, fare, fare, true, offlineApp);
            if (first == null) {
                first = record;
            }
        }
        if (first != null) {
            unseenMaxFare = TapResult.maxFare(first, boarded.size() - 1);
        }
        boarded.clear();
    }

    // The max-fare pop-up to show once (null if none)
    static TapResult takeMaxFareResult() {
        TapResult result = unseenMaxFare;
        unseenMaxFare = null;
        return result;
    }

    // ----- Helpers -----

    // Takes the money (unless the app user is offline), saves the record, and adds
    // "Pay Fare" to the card owner's Transactions tab
    private static ShiftRecord charge(TapRider rider, String from, String to, double km, long fare,
                                      long base, boolean maxFare, boolean offlineApp) {
        if (!offlineApp) {
            rider.pay(fare);
        }
        Double balance = offlineApp ? null : rider.getBalance();
        ShiftRecord record = new ShiftRecord(rider.isApp(), rider.getShortId(), from, to, km, fare, base,
                balance, maxFare, offlineApp);
        OperatorState.addFare(record);

        if (!rider.isApp()) {
            DemoAccount owner = MockData.allCards().get(rider.getCard());
            if (owner != null) {
                owner.addTransaction(DemoTransaction.FARE, maxFare ? "Max Fare" : "Pay Fare",
                        from + " → " + to, -fare / 100.0);
            }
        }
        return record;
    }

    // Distance along the stops, not a straight line (same idea as Fares.pathKm)
    private static double pathKm(int a, int b) {
        List<Api.StopCoord> stops = OperatorState.getGps().getRouteStops();
        int from = Math.min(a, b);
        int to = Math.max(a, b);
        double km = 0;
        for (int i = from; i < to; i++) {
            Api.StopCoord p = stops.get(i);
            Api.StopCoord q = stops.get(i + 1);
            km += Fares.haversineKm(p.lat(), p.lng(), q.lat(), q.lng());
        }
        return km;
    }
}
