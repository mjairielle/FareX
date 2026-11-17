package com.farex.ui;

import java.util.List;

import com.farex.Api;
import com.farex.operator.GpsSimulator;
import com.farex.operator.OfflineQueue;
import com.farex.operator.ShiftSession;

import farex.shared.user.Operator;

// Everything Operator Mode remembers until the app restarts:
// whether we are in Operator Mode, the shift (Jay's ShiftSession), the GPS stops (Jay's GpsSimulator),
// records waiting to sync (Jay's OfflineQueue) and the money collected this shift.
// Login, Register, OTP, Congrats and Account Information are shared with the passenger side;
// they ask this class where to go next.
public final class OperatorState {

    public static final String PLATE = "ABC-123";
    public static final String ROUTE_NAME = "Philcoa - Fairview Terminal";
    public static final int CAPACITY = 22;

    private static boolean operatorMode = false;
    private static int lastTab = 0;   // 0 Dashboard, 1 Ready Mode, 2 History

    private static final ShiftSession shift = new ShiftSession();
    private static final GpsSimulator gps = new GpsSimulator();
    private static final OfflineQueue offlineQueue = new OfflineQueue();
    private static String lastSynced = "Today, 6:10 PM";

    // ----- This shift's totals (in centavos, like farex-shared's Fares) -----
    // ShiftSession counts every fare as a "trip"; here a trip is one full run of the route,
    // so the totals are kept here and ShiftSession is used for start time and running state.
    private static long cardCents = 0;
    private static long appCents = 0;
    private static int passengers = 0;
    private static int trips = 0;
    private static int maxFareCount = 0;

    static {
        // Same stops as Jay's OperatorScreen uses when the server is off
        gps.setRouteStops(List.of(
                new Api.StopCoord(0, "Philcoa", 14.6537, 121.0594),
                new Api.StopCoord(1, "Tandang Sora", 14.6700, 121.0540),
                new Api.StopCoord(2, "Ever Gotesco", 14.6810, 121.0480),
                new Api.StopCoord(3, "Batasan", 14.6850, 121.0870),
                new Api.StopCoord(4, "Dona Carmen", 14.6930, 121.0800),
                new Api.StopCoord(5, "Fairview Terminal", 14.7300, 121.0560)));

        // 12 records from the last shift that were never synced
        String[] stops = {"Philcoa", "Tandang Sora", "Ever Gotesco", "Batasan"};
        for (int i = 0; i < 12; i++) {
            String cents = String.valueOf(1300 + (i % 4) * 200);
            offlineQueue.enqueue(new String[] {"16 Nov", i % 3 == 0 ? "App" : "Card",
                    stops[i % 4], "Fairview Terminal", cents});
        }
    }

    private OperatorState() {
    }

    // ----- Operator Mode on/off -----

    public static boolean isOperatorMode() {
        return operatorMode;
    }

    public static void enter() {
        operatorMode = true;
    }

    public static void leave() {
        operatorMode = false;
    }

    // ----- Where shared screens go next -----

    // Back arrow on Account Information: Operator Dashboard or the Profile tab
    public static String homeRoute() {
        return operatorMode ? "operatorDashboard" : "profile";
    }

    // After a correct login
    public static String afterLoginRoute() {
        return operatorMode ? "operatorDashboard" : "dashboard";
    }

    public static String loginRoute() {
        return operatorMode ? "operatorLogin" : "login";
    }

    public static String registerRoute() {
        return operatorMode ? "operatorRegister" : "register";
    }

    // The operator tab last shown, so "Keep driving" returns to it
    public static int getLastTab() {
        return lastTab;
    }

    public static void setLastTab(int tab) {
        lastTab = tab;
    }

    // ----- Who is driving -----

    // The logged-in account as a farex-shared Operator, e.g. ("o1", "Aron Ferrancullo", ..., "ABC-123")
    public static Operator getOperator() {
        DemoAccount account = MockData.getCurrentAccount();
        String name = (account == null) ? "FareX Operator" : account.getFullName();
        String email = (account == null) ? "" : account.getEmail();
        return new Operator("o1", name, email, PLATE);
    }

    public static GpsSimulator getGps() {
        return gps;
    }

    public static int stopCount() {
        return gps.getRouteStops().size();
    }

    // ----- Shift -----

    public static boolean isOnShift() {
        return shift.isOnShift();
    }

    // START SHIFT: new totals, jeep back at the first stop
    public static void startShift() {
        shift.start();
        cardCents = 0;
        appCents = 0;
        passengers = 0;
        trips = 0;
        maxFareCount = 0;
        gps.setCurrentStopIdx(0);
    }

    // The totals stay until the next START SHIFT, so the summary and dashboard can show them
    public static void endShift() {
        shift.end();
        gps.setAutoDriving(false);
    }

    // "1h 05m" or "12m"
    public static String shiftDuration() {
        return shift.getFormattedDuration();
    }

    // ----- Totals -----

    // Confirmed fares only (an app tap saved offline is not counted until it syncs)
    public static long collectedCents() {
        return cardCents + appCents;
    }

    public static long getCardCents() {
        return cardCents;
    }

    public static long getAppCents() {
        return appCents;
    }

    public static int getPassengers() {
        return passengers;
    }

    public static int getTrips() {
        return trips;
    }

    public static int getMaxFareCount() {
        return maxFareCount;
    }

    // ----- Sync -----

    public static int pendingRecords() {
        return offlineQueue.size();
    }

    public static String getLastSynced() {
        return lastSynced;
    }
}
