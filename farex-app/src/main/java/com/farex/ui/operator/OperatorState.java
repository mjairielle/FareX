package com.farex.ui.operator;

import java.util.ArrayList;
import java.util.Collections;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.Random;

import javafx.animation.Animation;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.util.Duration;

import com.farex.Api;
import com.farex.operator.GpsSimulator;
import com.farex.operator.OfflineQueue;
import com.farex.operator.ShiftSession;

import farex.shared.user.Operator;

import com.farex.ui.data.DemoAccount;
import com.farex.ui.data.MockData;

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

    // Fares collected this shift, newest first
    private static final List<ShiftRecord> records = new ArrayList<>();

    // Fares from the 16 Nov shift (sample data), shown under this shift in History
    private static final List<ShiftRecord> earlierRecords = new ArrayList<>();

    // The jeep's connection. Offline: app taps are saved but not confirmed (cards still work)
    private static boolean online = true;

    // ----- GPS clock -----
    // Ticks once a second during a shift. With Auto Drive on, Jay's GpsSimulator moves the jeep
    // one stop every 6 ticks (6 s). The open Ready Mode screen listens so it can redraw.
    private static final Timeline gpsClock = new Timeline(new KeyFrame(Duration.seconds(1), e -> onGpsTick()));
    private static Runnable gpsListener = null;
    private static double accuracyMeters = 3.0;
    private static final Random random = new Random();

    static {
        // Same stops as Jay's OperatorScreen uses when the server is off
        gps.setRouteStops(List.of(
                new Api.StopCoord(0, "Philcoa", 14.6537, 121.0594),
                new Api.StopCoord(1, "Tandang Sora", 14.6700, 121.0540),
                new Api.StopCoord(2, "Ever Gotesco", 14.6810, 121.0480),
                new Api.StopCoord(3, "Batasan", 14.6850, 121.0870),
                new Api.StopCoord(4, "Dona Carmen", 14.6930, 121.0800),
                new Api.StopCoord(5, "Fairview Terminal", 14.7300, 121.0560)));

        // 12 records from the 16 Nov shift that were never synced
        for (ShiftRecord record : SampleShift.records(gps.getRouteStops())) {
            earlierRecords.add(record);
            offlineQueue.enqueue(toRow(record));
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
        records.clear();
        gps.setCurrentStopIdx(0);
        gps.setAutoDriving(false);
        gpsClock.setCycleCount(Animation.INDEFINITE);
        gpsClock.play();
    }

    // The totals stay until the next START SHIFT, so the summary and dashboard can show them
    public static void endShift() {
        shift.end();
        gps.setAutoDriving(false);
        gpsClock.stop();
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

    // ----- Recording taps (called by TapTerminal) -----

    // A passenger tapped in: "TOTAL BOARDED" goes up by 1
    public static void addBoarded() {
        passengers++;
        shift.recordBoarded();
    }

    // A fare was charged. Confirmed money goes into the Card or App total right away;
    // an offline app fare waits for sync. Every record also waits in the offline queue.
    public static void addFare(ShiftRecord record) {
        records.add(0, record);
        if (!record.isPaymentPending()) {
            if (record.isApp()) {
                appCents += record.getCents();
            } else {
                cardCents += record.getCents();
            }
        }
        if (record.isMaxFare()) {
            maxFareCount++;
        }
        offlineQueue.enqueue(toRow(record));
    }

    // One line for the offline queue: "18:24,Card,**** 6767,Philcoa,Ever Gotesco,1566"
    private static String[] toRow(ShiftRecord record) {
        return new String[] {record.getTime().toString(), record.isApp() ? "App" : "Card",
                record.getWho(), record.getFrom(), record.getTo(), String.valueOf(record.getCents())};
    }

    public static List<ShiftRecord> getRecords() {
        return Collections.unmodifiableList(records);
    }

    public static List<ShiftRecord> getEarlierRecords() {
        return Collections.unmodifiableList(earlierRecords);
    }

    public static boolean isOnline() {
        return online;
    }

    public static void toggleOnline() {
        online = !online;
    }

    // ----- Moving along the route -----

    // The Ready Mode screen registers itself here; null = nobody is listening
    public static void setGpsListener(Runnable listener) {
        gpsListener = listener;
    }

    // "Next Stop": Philcoa (0) -> Tandang Sora (1) ... Reaching Fairview Terminal (5) completes a trip
    public static void nextStop() {
        if (gps.hasNextStop()) {
            gps.advanceStop();
            if (isAtTerminal()) {
                arrivedAtTerminal();
            }
        }
    }

    public static void previousStop() {
        gps.rewindStop();
    }

    // At the terminal the jeep turns around: back to Philcoa for a new trip
    public static void startNewTrip() {
        gps.setCurrentStopIdx(0);
        gps.setAutoDriving(false);
    }

    public static boolean isAtTerminal() {
        return !gps.hasNextStop();
    }

    public static void toggleAutoDrive() {
        if (isAtTerminal()) {
            return;     // nowhere left to drive
        }
        gps.toggleAutoDriving();
    }

    // "Fix GPS": take a fresh reading, accuracy between ±1.5 m and ±4.0 m
    public static void fixGps() {
        accuracyMeters = 1.5 + random.nextInt(6) * 0.5;
    }

    // "±3.0m"
    public static String accuracyText() {
        return String.format(Locale.US, "±%.1fm", accuracyMeters);
    }

    private static void onGpsTick() {
        boolean moved = gps.tick();
        if (moved && isAtTerminal()) {
            gps.setAutoDriving(false);
            arrivedAtTerminal();
        }
        if (moved && gpsListener != null) {
            gpsListener.run();
        }
    }

    // One full run of the route is done
    private static void arrivedAtTerminal() {
        trips++;
    }

    // ----- Sync -----

    public static int pendingRecords() {
        return offlineQueue.size();
    }

    public static String getLastSynced() {
        return lastSynced;
    }

    // A sync went through: every record is on the server, offline app payments are confirmed
    // (this shift's ones now count in the App total), and the offline queue is emptied
    public static void syncAll() {
        for (ShiftRecord record : records) {
            if (!record.isSynced() && record.isPaymentPending()) {
                appCents += record.getCents();
            }
            record.markSynced();
        }
        for (ShiftRecord record : earlierRecords) {
            record.markSynced();
        }
        offlineQueue.clear();
        lastSynced = "Today, " + LocalTime.now().format(DateTimeFormatter.ofPattern("h:mm a", Locale.US));
    }
}
