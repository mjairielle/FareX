package com.farex.ui.operator;

import java.util.ArrayList;
import java.util.Collections;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.Random;

import javafx.application.Platform;
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
import com.farex.ui.common.Navigator;
import com.farex.ui.common.PopupDialog;
import com.farex.ui.common.WarningDialog;
import com.farex.ui.data.MockData;

// Everything Operator Mode remembers until the app restarts:
// whether we are in Operator Mode, the shift (Jay's ShiftSession), the GPS stops (Jay's GpsSimulator),
// records waiting to sync (Jay's OfflineQueue) and the money collected this shift.
// Login, Register, OTP, Congrats and Account Information are shared with the passenger side;
// they ask this class where to go next.
public final class OperatorState {

    public static final String PLATE = "ABC-123";
    // The route the jeep runs: the sample line, or (live) the one this operator chose, which the admin can change
    private static String routeName = "Philcoa - Fairview Terminal";
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
                new Api.StopCoord(0, "Philcoa", 14.65400, 121.05377),
                new Api.StopCoord(1, "Tandang Sora", 14.66223, 121.06619),
                new Api.StopCoord(2, "Ever Gotesco", 14.67959, 121.08409),
                new Api.StopCoord(3, "Batasan", 14.68766, 121.08730),
                new Api.StopCoord(4, "Dona Carmen", 14.70517, 121.07843),
                new Api.StopCoord(5, "Fairview Terminal", 14.73306, 121.05840)));

        // 12 records from the 16 Nov shift that were never synced
        for (ShiftRecord record : SampleShift.records(gps.getRouteStops())) {
            earlierRecords.add(record);
            offlineQueue.enqueue(toRow(record));
        }
        sampleQueued = offlineQueue.size();
    }

    private static int sampleQueued;   // sample rows still in the queue (0 after a sync)

    private OperatorState() {
    }

    // ----- Operator Mode on/off -----

    public static boolean isOperatorMode() {
        return operatorMode;
    }

    public static void enter() {
        operatorMode = true;
        loadRoute();
    }

    public static String routeName() {
        return routeName;
    }

    // "Fairview Terminal": where the jeep is headed in this direction (the route's last stop)
    public static String headedTo() {
        List<Api.StopCoord> stops = gps.getRouteStops();
        return stops.get(stops.size() - 1).name();
    }

    // The jeep turns around: the route runs the other way (Philcoa > Fairview becomes Fairview > Philcoa), back at
    // its first stop. Any number of jeeps may run the same route either way. Live: the server turns too, as it prices
    // taps by stop, and refuses while riders are aboard. null = done, else why not.
    public static String turnAround() {
        if (MockData.isLive()) {
            try {
                Api.OperatorRoute r = Api.turnRoute();
                gps.setRouteStops(r.stops());
                routeName = r.name();
            } catch (Api.ApiException e) {
                return e.getMessage();
            }
        } else {
            List<Api.StopCoord> now = gps.getRouteStops(), back = new ArrayList<>();
            for (int i = now.size() - 1; i >= 0; i--) {
                back.add(new Api.StopCoord(back.size(), now.get(i).name(), now.get(i).lat(), now.get(i).lng()));
            }
            gps.setRouteStops(back);
        }
        gps.setCurrentStopIdx(0);
        gps.setAutoDriving(false);
        sentStop = -2;                     // the server forgot the jeep's stop on turning: send it again
        return null;
    }

    // Live: the stops of this operator's route from the server (/operator/route), loaded when Operator Mode opens.
    // Not during a shift, so a running trip keeps its stops; a route the admin changed applies from the next shift.
    // Offline: the stops already loaded stay.
    static void loadRoute() {
        if (!MockData.isLive() || shift.isOnShift()) {
            return;
        }
        try {
            Api.OperatorRoute r = Api.operatorRoute();
            if (r.stops().size() >= 2) {
                gps.setRouteStops(r.stops());
                gps.setCurrentStopIdx(0);
                routeName = r.name();
            }
        } catch (Api.ApiException offline) {
            // keep the stops we have
        }
    }

    public static void leave() {
        operatorMode = false;
    }

    // ----- Where shared screens go next -----

    // Back arrow on Account Information: Operator Dashboard or the Profile tab
    public static String homeRoute() {
        return operatorMode ? "operatorDashboard" : "profile";
    }

    // After login or the "Log in as" chooser: that role's home. The server token already acts as it.
    public static void openRole(String role) {
        if ("OPERATOR".equals(role)) {
            enter();
            Navigator.go("operatorDashboard");
        } else {
            leave();
            Navigator.go("dashboard");
        }
    }

    // Switch to a role of the same account (no password again), or show why not
    public static void switchTo(String role) {
        String error = MockData.useRole(role);
        if (error != null) {
            new PopupDialog(WarningDialog.warningIcon(), "Couldn't switch", error).button("OK", () -> { }).show();
        } else {
            openRole(role);
        }
    }

    // "Exit Operator Mode": back to the passenger side. An operator-only account is asked first,
    // then gets a passenger side (no admin approval needed for riding).
    public static void toPassenger() {
        if (MockData.hasRole("PASSENGER")) {
            switchTo("PASSENGER");
            return;
        }
        new PopupDialog("Use FareX as a passenger?",
                "This account is for driving only. Add a passenger side to ride, load cards and plan trips with the same login.")
                .button("Add passenger side", () -> {
                    String error = MockData.addRole("PASSENGER", "");
                    if (error != null) {
                        new PopupDialog(WarningDialog.warningIcon(), "Couldn't add it", error).button("OK", () -> { }).show();
                    } else {
                        switchTo("PASSENGER");
                    }
                })
                .link("Cancel", () -> { })
                .show();
    }

    // The live operator's own jeep, else the sample one
    public static String plate() {
        return MockData.plate() != null ? MockData.plate() : PLATE;
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
        return new Operator("o1", name, email, plate());
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
        loadRoute();                       // a route the admin changed since Operator Mode opened
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
        syncStop(-1);   // QR scans are refused until the next shift
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
        if (MockData.isLive()) {
            record.markSynced();   // the server charged it already (card tap or a passenger's QR scan): nothing to upload
        } else {
            offlineQueue.enqueue(toRow(record));
        }
    }

    // One line for the offline queue: "18:24,Card,**** 6767,Philcoa,Ever Gotesco,1566"
    private static String[] toRow(ShiftRecord record) {
        return new String[] {record.getTime().toString(), record.isApp() ? "App" : "Card",
                record.getWho(), record.getFrom(), record.getTo(), String.valueOf(record.getCents())};
    }

    public static List<ShiftRecord> getRecords() {
        return Collections.unmodifiableList(records);
    }

    // The 16 Nov sample shift: mock demo only, a live operator has no fake history
    public static List<ShiftRecord> getEarlierRecords() {
        return MockData.isLive() ? List.of() : Collections.unmodifiableList(earlierRecords);
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

    // At the terminal the jeep turns around for a new trip: Fairview back to Philcoa. null = done, else why not
    public static String startNewTrip() {
        return turnAround();
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

    // Live: the server needs the jeep's stop for passengers' QR scans. Sent when it changes (checked every second,
    // so Next Stop, Auto Drive and Fix GPS are all covered); a failed send is retried on the next tick.
    private static volatile int sentStop = -2;

    private static void syncStop(int stop) {
        if (!MockData.isLive() || stop == sentStop) {
            return;
        }
        sentStop = stop;
        Thread t = new Thread(() -> {
            try {
                Api.setJeepStop(stop);
            } catch (Api.ApiException offline) {
                sentStop = -2;
            }
        }, "farex-jeep-stop");
        t.setDaemon(true);
        t.start();
    }

    // Live: every 5 s on shift, ask the server about passengers who paid by QR (see TapTerminal.mergeServer)
    private static int ticks;
    private static volatile boolean polling;

    private static void pollQrRiders() {
        if (!MockData.isLive() || polling || ++ticks % 5 != 0) {
            return;
        }
        polling = true;
        Thread t = new Thread(() -> {
            try {
                Object[] server = TapTerminal.fetchServer();
                Platform.runLater(() -> {
                    polling = false;
                    if (shift.isOnShift() && TapTerminal.mergeServer(server) && gpsListener != null) {
                        gpsListener.run();
                    }
                });
            } catch (Api.ApiException offline) {
                polling = false;
            }
        }, "farex-qr-riders");
        t.setDaemon(true);
        t.start();
    }

    private static void onGpsTick() {
        syncStop(gps.getCurrentStopIdx());
        pollQrRiders();
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

    // Live: the sample shift's 12 queued records are not this operator's, so only real ones count
    public static int pendingRecords() {
        return MockData.isLive() ? offlineQueue.size() - sampleQueued : offlineQueue.size();
    }

    public static String getLastSynced() {
        return MockData.isLive() && pendingRecords() == 0 ? "Up to date" : lastSynced;
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
        sampleQueued = 0;
        lastSynced = "Today, " + LocalTime.now().format(DateTimeFormatter.ofPattern("h:mm a", Locale.US));
    }
}
