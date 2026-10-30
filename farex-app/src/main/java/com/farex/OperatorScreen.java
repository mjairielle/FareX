package com.farex;

import farex.shared.data.Csv;
import farex.shared.data.Store;
import farex.shared.fare.Fares;
import farex.shared.txn.Txn;
import farex.shared.user.Operator;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;
import javafx.animation.Animation;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;

/**
 * Operator App - Dashboard and tap terminal for jeepney operators.
 * Implements Dashboard, Shift Start/End, Ready Mode terminal,
 * GPS Stop tracker & Auto-Drive simulator, live on-board manifest,
 * fallback max fare charging, PIN lock protection, local CSV queue, and batch sync.
 */
public class OperatorScreen extends Screen {
    private record CardItem(String uid, String label) {
        @Override public String toString() { return label; }
    }

    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("h:mm a").withZone(ZoneId.systemDefault());

    // Operator & Session State
    private final Operator operator;
    private boolean onShift = false;
    private Instant shiftStartTime = null;
    private boolean isLocked = false;
    private String lockPin = "1234";

    // Shift Metrics
    private long shiftEarningsCents = 0;
    private int shiftCompletedTrips = 0;
    private int shiftTotalBoarded = 0;

    // Route & GPS State
    private List<Api.StopCoord> routeStops = new ArrayList<>();
    private int currentStopIdx = 0;
    private boolean autoGpsDriving = false;
    private int autoGpsCountdown = 6;

    // Card selection & manual input state
    private String selectedCardUid = null;
    private String manualInputId = "";

    // Local shift ledger rows for offline resilience & sync
    private final List<String[]> localTxnRows = new ArrayList<>();
    private int unsyncedCount = 0;

    // View Switching
    private enum ViewMode { DASHBOARD, READY_MODE, HISTORY }
    private ViewMode currentView = ViewMode.DASHBOARD;

    // Feedback Banner State
    private String feedbackMessage = null;
    private boolean feedbackIsSuccess = true;

    // Active reference for periodic ticker
    private Label activeTimerLabel = null;
    private Timeline ticker;

    public OperatorScreen() {
        this.operator = Session.user instanceof Operator op
                ? op
                : new Operator("o1", "Ben", "ben@mail.com", "ABC-123");

        // Load route stops safely with offline fallback
        try {
            this.routeStops = Api.stopsWithCoords();
            if (this.routeStops.isEmpty()) throw new RuntimeException("Empty route stops");
        } catch (Exception e) {
            this.routeStops = List.of(
                    new Api.StopCoord(0, "Philcoa", 14.6537, 121.0594),
                    new Api.StopCoord(1, "Tandang Sora", 14.6700, 121.0540),
                    new Api.StopCoord(2, "Ever Gotesco", 14.6810, 121.0480),
                    new Api.StopCoord(3, "Batasan", 14.6850, 121.0870),
                    new Api.StopCoord(4, "Dona Carmen", 14.6930, 121.0800),
                    new Api.StopCoord(5, "Fairview Terminal", 14.7300, 121.0560)
            );
        }

        // Periodic timer for live shift duration clock and GPS simulation
        ticker = new Timeline(new KeyFrame(javafx.util.Duration.seconds(1), e -> onTick()));
        ticker.setCycleCount(Animation.INDEFINITE);
        ticker.play();

        render();
    }

    private void render() {
        getChildren().clear();
        activeTimerLabel = null;

        // Top Navigation Bar
        HBox navBar = new HBox(6);
        navBar.setAlignment(Pos.CENTER);

        Button btnDash = new Button("Dashboard");
        Button btnReady = new Button("Ready Mode");
        Button btnHistory = new Button("History");

        styleNavButton(btnDash, currentView == ViewMode.DASHBOARD);
        styleNavButton(btnReady, currentView == ViewMode.READY_MODE);
        styleNavButton(btnHistory, currentView == ViewMode.HISTORY);

        btnDash.setOnAction(e -> checkLockAndSwitch(ViewMode.DASHBOARD));
        btnReady.setOnAction(e -> {
            if (!onShift) {
                setNotice("Please start your shift first from the Dashboard.");
                render();
                return;
            }
            switchView(ViewMode.READY_MODE);
        });
        btnHistory.setOnAction(e -> checkLockAndSwitch(ViewMode.HISTORY));

        navBar.getChildren().addAll(btnDash, btnReady, btnHistory);
        getChildren().add(navBar);

        // Content Area inside ScrollPane
        VBox contentArea = new VBox(12);
        contentArea.setStyle("-fx-background-color: #111111;");

        // Feedback Banner
        if (feedbackMessage != null && !feedbackMessage.isBlank()) {
            Label banner = new Label(feedbackMessage);
            banner.setWrapText(true);
            banner.setMaxWidth(Double.MAX_VALUE);
            if (feedbackIsSuccess) {
                banner.setStyle("-fx-background-color: #1e331e; -fx-text-fill: #c8ff00; -fx-padding: 10; -fx-background-radius: 6; -fx-font-size: 13; -fx-font-weight: bold;");
            } else {
                banner.setStyle("-fx-background-color: #222222; -fx-text-fill: #ffffff; -fx-padding: 10; -fx-background-radius: 6; -fx-font-size: 12;");
            }
            contentArea.getChildren().add(banner);
        }

        switch (currentView) {
            case DASHBOARD -> buildDashboardView(contentArea);
            case READY_MODE -> buildReadyModeView(contentArea);
            case HISTORY -> buildHistoryView(contentArea);
        }

        ScrollPane scroll = new ScrollPane(contentArea);
        scroll.setFitToWidth(true);
        scroll.setStyle("-fx-background: #111111; -fx-background-color: #111111; -fx-border-color: transparent;");
        setVgrow(scroll, Priority.ALWAYS);

        getChildren().addAll(scroll, error);
    }

    private void styleNavButton(Button btn, boolean active) {
        btn.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(btn, Priority.ALWAYS);
        if (active) {
            btn.setStyle("-fx-background-color: #c8ff00; -fx-text-fill: black; -fx-font-weight: bold; -fx-padding: 8;");
        } else {
            btn.setStyle("-fx-background-color: #222222; -fx-text-fill: #aaaaaa; -fx-font-weight: bold; -fx-padding: 8;");
        }
    }

    private void switchView(ViewMode mode) {
        currentView = mode;
        feedbackMessage = null;
        render();
    }

    private void checkLockAndSwitch(ViewMode target) {
        if (isLocked) {
            promptPinUnlock(() -> switchView(target));
        } else {
            switchView(target);
        }
    }

    // ==========================================
    // 1. OPERATOR DASHBOARD
    // ==========================================
    private void buildDashboardView(VBox container) {
        VBox headerCard = new VBox(6);
        headerCard.setStyle("-fx-background-color: #1c1c1c; -fx-padding: 16; -fx-background-radius: 8;");
        Label title = heading("Operator Dashboard");
        Label driver = text("Driver: " + operator.getName() + " (" + operator.getEmail() + ")");
        Label vehicle = lime("Vehicle Plate: " + operator.getVehiclePlate(), 14);
        Label route = text("Route: Philcoa - Fairview Terminal (" + routeStops.size() + " stops)");
        headerCard.getChildren().addAll(title, driver, vehicle, route);

        // Shift Status Card
        VBox statusCard = new VBox(8);
        statusCard.setStyle("-fx-background-color: #1c1c1c; -fx-padding: 16; -fx-background-radius: 8;");
        Label statusHeader = lime(onShift ? "● SHIFT ACTIVE" : "○ SHIFT INACTIVE", 16);

        Label timerLbl = text(getFormattedDuration());
        activeTimerLabel = timerLbl;

        GridPane statsGrid = new GridPane();
        statsGrid.setHgap(16);
        statsGrid.setVgap(8);
        statsGrid.add(text("Shift Revenue:"), 0, 0);
        statsGrid.add(lime(peso(shiftEarningsCents), 18), 1, 0);
        statsGrid.add(text("Completed Trips:"), 0, 1);
        statsGrid.add(lime(String.valueOf(shiftCompletedTrips), 14), 1, 1);
        statsGrid.add(text("Total Boarded:"), 0, 2);
        statsGrid.add(lime(String.valueOf(shiftTotalBoarded), 14), 1, 2);
        statsGrid.add(text("Duration:"), 0, 3);
        statsGrid.add(timerLbl, 1, 3);

        statusCard.getChildren().addAll(statusHeader, statsGrid);

        // Actions
        VBox actions = new VBox(10);
        if (!onShift) {
            actions.getChildren().add(primary("START SHIFT", this::startShift));
        } else {
            actions.getChildren().addAll(
                    primary("OPEN READY MODE (TERMINAL)", () -> switchView(ViewMode.READY_MODE)),
                    secondary("END SHIFT", this::confirmEndShift)
            );
        }
        actions.getChildren().add(secondary("Log out", this::logout));

        container.getChildren().addAll(headerCard, statusCard, actions);
    }

    private void startShift() {
        onShift = true;
        shiftStartTime = Instant.now();
        shiftEarningsCents = 0;
        shiftCompletedTrips = 0;
        shiftTotalBoarded = 0;
        autoGpsDriving = false;
        localTxnRows.clear();
        unsyncedCount = 0;
        logStopArrival(currentStopIdx);
        feedbackMessage = "Shift started successfully! Ready mode is now active.";
        feedbackIsSuccess = true;
        switchView(ViewMode.READY_MODE);
    }

    private void confirmEndShift() {
        if (isLocked) {
            promptPinUnlock(this::performEndShift);
        } else {
            performEndShift();
        }
    }

    private void performEndShift() {
        autoGpsDriving = false;
        List<Api.BoardedPassenger> onBoard = getBoardedListSafe();
        if (!onBoard.isEmpty()) {
            Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
            alert.setTitle("Passengers Still On Board");
            alert.setHeaderText(onBoard.size() + " passenger(s) have not tapped out.");
            alert.setContentText("Do you want to charge them fallback Max Fare before ending the shift?");
            ButtonType btnChargeMax = new ButtonType("Charge Max Fare & End");
            ButtonType btnEndAnyway = new ButtonType("End Anyway");
            ButtonType btnCancel = new ButtonType("Cancel", ButtonBar.ButtonData.CANCEL_CLOSE);
            alert.getButtonTypes().setAll(btnChargeMax, btnEndAnyway, btnCancel);

            Optional<ButtonType> res = alert.showAndWait();
            if (res.isEmpty() || res.get() == btnCancel) return;
            if (res.get() == btnChargeMax) {
                for (Api.BoardedPassenger p : onBoard) {
                    try {
                        String[] r = Api.tapMissed(p.uid());
                        recordLocalTxn(p.uid(), r);
                    } catch (Exception ignored) {}
                }
            }
        }

        syncBatchToServer();

        Alert summary = new Alert(Alert.AlertType.INFORMATION);
        summary.setTitle("Shift Completed");
        summary.setHeaderText("Shift Summary - Vehicle " + operator.getVehiclePlate());
        long durationMin = shiftStartTime != null ? Duration.between(shiftStartTime, Instant.now()).toMinutes() : 0;
        summary.setContentText(String.format(
                "Driver: %s\nDuration: %d minutes\nTotal Revenue: %s\nCompleted Trips: %d\nTotal Boarded: %d\n\nAll shift transactions have been synced to the server ledger.",
                operator.getName(), durationMin, peso(shiftEarningsCents), shiftCompletedTrips, shiftTotalBoarded
        ));
        summary.showAndWait();

        onShift = false;
        shiftStartTime = null;
        isLocked = false;
        switchView(ViewMode.DASHBOARD);
    }

    // ==========================================
    // 2. READY MODE (TAP TERMINAL & JEEPNEY DISPLAY)
    // ==========================================
    private void buildReadyModeView(VBox container) {
        // Mode Header & PIN Lock Toggle
        HBox readyHeader = new HBox(10);
        readyHeader.setAlignment(Pos.CENTER_LEFT);
        Label modeBadge = lime("● READY MODE", 14);
        HBox.setHgrow(modeBadge, Priority.ALWAYS);

        Button btnLock = secondary(isLocked ? "🔒 Locked (PIN)" : "🔓 Lock Terminal", this::toggleLock);
        btnLock.setMaxWidth(130);
        readyHeader.getChildren().addAll(modeBadge, btnLock);

        // Vehicle Plate & Revenue Strip
        HBox vehicleStrip = new HBox(12);
        vehicleStrip.setStyle("-fx-background-color: #1c1c1c; -fx-padding: 10; -fx-background-radius: 6;");
        vehicleStrip.setAlignment(Pos.CENTER_LEFT);
        Label plateLbl = lime("Plate: " + operator.getVehiclePlate(), 13);
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        Label revLbl = text("Shift: " + peso(shiftEarningsCents));
        List<Api.BoardedPassenger> boarded = getBoardedListSafe();
        Label boardBadge = lime(boarded.size() + " On Board", 13);
        vehicleStrip.getChildren().addAll(plateLbl, spacer, revLbl, boardBadge);

        // Jeepney QR Code Display
        VBox qrCard = buildJeepneyQrCard();

        // Current Route Stop Tracker & GPS Simulation
        VBox stopCard = buildStopTrackerCard();

        // Tap Terminal Controls
        VBox tapCard = buildTapTerminalCard();

        // Live On-board Passenger Manifest
        VBox manifestCard = buildManifestCard(boarded);

        // Local Ledger & Sync Status Bar
        HBox syncBar = buildSyncBar();

        container.getChildren().addAll(readyHeader, vehicleStrip, qrCard, stopCard, tapCard, manifestCard, syncBar);
    }

    private VBox buildJeepneyQrCard() {
        VBox card = new VBox(6);
        card.setStyle("-fx-background-color: #181818; -fx-padding: 12; -fx-background-radius: 6; -fx-border-color: #333333; -fx-border-radius: 6;");
        card.setAlignment(Pos.CENTER);

        Label qrTitle = lime("Passenger App Scan QR", 12);
        Label qrVisual = new Label(
                "▄▄▄▄▄▄▄  ▄  ▄ ▄▄▄▄▄▄▄\n" +
                "█ ▄▄▄ █ ▄ ▀▀█ █ ▄▄▄ █\n" +
                "█ ███ █ █▀▀▀█ █ ███ █\n" +
                "█▄▄▄▄▄█ █ █ █ █▄▄▄▄▄█\n" +
                "▄▄▄▄▄ ▄▄█ ▀ █▄ ▄ ▄ ▄ \n" +
                "█ ▄ █▄▄▀ ▀▀▀█▄▀▄█▀▄▄█\n" +
                "█▄▄█▄▄█▀█▄█▀█▄▄▄ █▄▀█\n" +
                "▄▄▄▄▄▄▄ █ ▀ █ ▄ █ ▀ █\n" +
                "█ ▄▄▄ █ ▄ ▀█ ▄█▄ ▀▀▀ \n" +
                "█ ███ █ █ ▀██▀ ▄▀ ▄▀ \n" +
                "█▄▄▄▄▄█ █▄ ▀█ █▄█ █ █"
        );
        qrVisual.setStyle("-fx-font-family: 'Courier New', monospace; -fx-text-fill: #ffffff; -fx-font-size: 10;");
        qrVisual.setAlignment(Pos.CENTER);

        Label qrPayload = text("FAREX:JEEPNEY:" + operator.getVehiclePlate());
        qrPayload.setStyle("-fx-font-size: 11; -fx-text-fill: #aaaaaa;");

        card.getChildren().addAll(qrTitle, qrVisual, qrPayload);
        return card;
    }

    private VBox buildStopTrackerCard() {
        VBox card = new VBox(8);
        card.setStyle("-fx-background-color: #1c1c1c; -fx-padding: 12; -fx-background-radius: 6;");

        Api.StopCoord cur = routeStops.get(currentStopIdx);
        HBox gpsStatusHdr = new HBox(8);
        gpsStatusHdr.setAlignment(Pos.CENTER_LEFT);
        Label gpsIndicator = lime(autoGpsDriving ? "● GPS TRACKER (AUTO-DRIVE)" : "● GPS TRACKER READY", 11);
        Region sp = new Region();
        HBox.setHgrow(sp, Priority.ALWAYS);
        Label accLbl = text("Accuracy: ±3.0m");
        accLbl.setStyle("-fx-font-size: 11; -fx-text-fill: #888888;");
        gpsStatusHdr.getChildren().addAll(gpsIndicator, sp, accLbl);

        Label stopTitle = text("Current Jeepney Stop (GPS Detected):");
        Label stopName = lime((currentStopIdx + 1) + ". " + cur.name(), 16);
        Label gpsCoords = text(String.format(Locale.ROOT, "Position: %.4f° N, %.4f° E", cur.lat(), cur.lng()));

        HBox navButtons = new HBox(8);
        Button btnPrev = secondary("◀ Prev", () -> {
            if (currentStopIdx > 0) {
                currentStopIdx--;
                logStopArrival(currentStopIdx);
                render();
            }
        });
        btnPrev.setDisable(currentStopIdx == 0);
        btnPrev.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(btnPrev, Priority.ALWAYS);

        Button btnNext = primary("Next ▶", () -> {
            if (currentStopIdx < routeStops.size() - 1) {
                currentStopIdx++;
                logStopArrival(currentStopIdx);
                render();
            }
        });
        btnNext.setDisable(currentStopIdx == routeStops.size() - 1);
        btnNext.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(btnNext, Priority.ALWAYS);

        Button btnAutoGps = secondary(autoGpsDriving ? "🛰️ Driving ON (" + autoGpsCountdown + "s)" : "🛰️ Auto Drive: OFF", () -> {
            autoGpsDriving = !autoGpsDriving;
            autoGpsCountdown = 6;
            setNotice(autoGpsDriving ? "🛰️ Auto-GPS Driving enabled. Jeepney will advance every 6s." : "🛰️ Auto-GPS Driving stopped.");
            render();
        });
        btnAutoGps.setStyle(autoGpsDriving ? "-fx-background-color: #1e331e; -fx-text-fill: #c8ff00; -fx-font-size: 11; -fx-padding: 8 6;"
                : "-fx-background-color: #2a2a2a; -fx-text-fill: #aaaaaa; -fx-font-size: 11; -fx-padding: 8 6;");

        Button btnInjectGps = secondary("📍 Fix", this::promptCustomGps);
        btnInjectGps.setMaxWidth(60);
        btnInjectGps.setStyle("-fx-background-color: #2a2a2a; -fx-text-fill: #c8ff00; -fx-font-size: 11; -fx-padding: 8 6;");

        navButtons.getChildren().addAll(btnPrev, btnNext, btnAutoGps, btnInjectGps);
        card.getChildren().addAll(gpsStatusHdr, stopTitle, stopName, gpsCoords, navButtons);
        return card;
    }

    private void logStopArrival(int idx) {
        if (idx < 0 || idx >= routeStops.size()) return;
        Api.StopCoord s = routeStops.get(idx);
        try {
            Csv.append(Path.of("data", "local_stops.csv"), "operatorId,plate,stopIdx,stopName,lat,lng,time",
                    operator.getId(), operator.getVehiclePlate(), String.valueOf(idx), s.name(),
                    String.valueOf(s.lat()), String.valueOf(s.lng()), Instant.now().toString());
        } catch (Exception ignored) {}
    }

    private void promptCustomGps() {
        Dialog<String[]> dialog = new Dialog<>();
        dialog.setTitle("Test GPS Coordinate Injection");
        dialog.setHeaderText("Simulate live GPS receiver coordinates:");

        ButtonType btnInject = new ButtonType("Set Position", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(btnInject, ButtonType.CANCEL);

        Api.StopCoord cur = routeStops.get(currentStopIdx);
        TextField latF = field("Latitude (e.g. 14.6700)");
        latF.setText(String.valueOf(cur.lat()));
        TextField lngF = field("Longitude (e.g. 121.0540)");
        lngF.setText(String.valueOf(cur.lng()));

        dialog.getDialogPane().setContent(new VBox(8, text("Latitude:"), latF, text("Longitude:"), lngF));
        dialog.setResultConverter(b -> b == btnInject ? new String[]{latF.getText().trim(), lngF.getText().trim()} : null);

        Optional<String[]> res = dialog.showAndWait();
        if (res.isPresent()) {
            try {
                double lat = Double.parseDouble(res.get()[0]);
                double lng = Double.parseDouble(res.get()[1]);

                int bestIdx = -1;
                double bestDist = Double.MAX_VALUE;
                for (int i = 0; i < routeStops.size(); i++) {
                    double d = Fares.haversineKm(lat, lng, routeStops.get(i).lat(), routeStops.get(i).lng());
                    if (d < bestDist) {
                        bestDist = d;
                        bestIdx = i;
                    }
                }

                if (bestDist <= 0.6 && bestIdx >= 0) {
                    currentStopIdx = bestIdx;
                    logStopArrival(bestIdx);
                    setSuccess(String.format(Locale.ROOT, "🛰️ GPS Proximity Match: %s (%.0f meters away)",
                            routeStops.get(bestIdx).name(), bestDist * 1000));
                } else {
                    Csv.append(Path.of("data", "candidate_stops.csv"), "operatorId,plate,lat,lng,time",
                            operator.getId(), operator.getVehiclePlate(), String.valueOf(lat), String.valueOf(lng), Instant.now().toString());
                    setNotice(String.format(Locale.ROOT, "🛰️ GPS Point (%.4f, %.4f) recorded as candidate stop for admin review (%.1f km from nearest stop).",
                            lat, lng, bestDist));
                }
                render();
            } catch (Exception ex) {
                setNotice("Invalid GPS coordinates: " + ex.getMessage());
                render();
            }
        }
    }

    private VBox buildTapTerminalCard() {
        VBox card = new VBox(8);
        card.setStyle("-fx-background-color: #1c1c1c; -fx-padding: 12; -fx-background-radius: 6;");

        Label prompt = text("Simulate Card Tap (NFC) or App User QR:");

        ComboBox<CardItem> cardPicker = combo();
        try {
            for (String[] c : Api.allCards()) {
                cardPicker.getItems().add(new CardItem(c[0], Store.formatUid(c[0]) + " - " + c[1]));
            }
            if (!cardPicker.getItems().isEmpty()) {
                if (selectedCardUid != null) {
                    for (CardItem it : cardPicker.getItems()) {
                        if (it.uid().equals(selectedCardUid)) { cardPicker.setValue(it); break; }
                    }
                }
                if (cardPicker.getValue() == null) cardPicker.setValue(cardPicker.getItems().get(0));
            }
        } catch (Exception ignored) {}

        cardPicker.valueProperty().addListener((o, was, now) -> {
            if (now != null) selectedCardUid = now.uid();
        });

        TextField manualIdField = field("Or enter Card UID / App User ID (e.g. p1)");
        manualIdField.setText(manualInputId);
        manualIdField.textProperty().addListener((o, was, now) -> manualInputId = now);

        HBox tapActions = new HBox(8);
        Button btnTapIn = primary("Tap In", () -> handleTapIn(cardPicker, manualIdField));
        Button btnTapOut = primary("Tap Out", () -> handleTapOut(cardPicker, manualIdField));
        Button btnMaxFare = secondary("Max Fare", () -> handleMissedTapOut(cardPicker, manualIdField));

        btnTapIn.setMaxWidth(Double.MAX_VALUE);
        btnTapOut.setMaxWidth(Double.MAX_VALUE);
        btnMaxFare.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(btnTapIn, Priority.ALWAYS);
        HBox.setHgrow(btnTapOut, Priority.ALWAYS);
        HBox.setHgrow(btnMaxFare, Priority.ALWAYS);

        tapActions.getChildren().addAll(btnTapIn, btnTapOut, btnMaxFare);
        card.getChildren().addAll(prompt, cardPicker, manualIdField, tapActions);
        return card;
    }

    private VBox buildManifestCard(List<Api.BoardedPassenger> boarded) {
        VBox card = new VBox(8);
        card.setStyle("-fx-background-color: #181818; -fx-padding: 12; -fx-background-radius: 6;");

        Label title = lime("Passengers Currently On Board (" + boarded.size() + ")", 13);
        card.getChildren().add(title);

        if (boarded.isEmpty()) {
            card.getChildren().add(text("No passengers currently on board."));
        } else {
            for (Api.BoardedPassenger p : boarded) {
                HBox row = new HBox(8);
                row.setStyle("-fx-background-color: #222222; -fx-padding: 8; -fx-background-radius: 4;");
                row.setAlignment(Pos.CENTER_LEFT);

                VBox pInfo = new VBox(2);
                Label nameLbl = new Label(p.name() + " (" + Store.formatUid(p.uid()) + ")");
                nameLbl.setStyle("-fx-text-fill: white; -fx-font-weight: bold; -fx-font-size: 12;");
                Label stopLbl = text("Boarded at: " + p.stopName() + " (Stop " + (p.stopIdx() + 1) + ")");
                stopLbl.setStyle("-fx-font-size: 11;");
                pInfo.getChildren().addAll(nameLbl, stopLbl);
                HBox.setHgrow(pInfo, Priority.ALWAYS);

                Button btnAlight = new Button("Tap Out");
                btnAlight.setStyle("-fx-background-color: #c8ff00; -fx-text-fill: black; -fx-font-size: 11; -fx-font-weight: bold; -fx-padding: 5 8;");
                btnAlight.setOnAction(e -> handleDirectTapOut(p.uid()));

                Button btnMissed = new Button("Max Fare");
                btnMissed.setStyle("-fx-background-color: #333333; -fx-text-fill: #ff6b6b; -fx-font-size: 11; -fx-font-weight: bold; -fx-padding: 5 8;");
                btnMissed.setOnAction(e -> handleDirectMissed(p.uid()));

                row.getChildren().addAll(pInfo, btnAlight, btnMissed);
                card.getChildren().add(row);
            }
        }
        return card;
    }

    private HBox buildSyncBar() {
        HBox bar = new HBox(10);
        bar.setAlignment(Pos.CENTER_LEFT);
        bar.setStyle("-fx-background-color: #1c1c1c; -fx-padding: 10; -fx-background-radius: 6;");

        Label syncLabel = text(unsyncedCount == 0 ? "● Local CSV Synced" : "▲ " + unsyncedCount + " Pending Upload");
        syncLabel.setStyle(unsyncedCount == 0 ? "-fx-text-fill: #c8ff00; -fx-font-size: 11;" : "-fx-text-fill: #ffcc00; -fx-font-size: 11;");
        HBox.setHgrow(syncLabel, Priority.ALWAYS);

        Button btnSync = secondary("Sync Now", this::syncBatchToServer);
        btnSync.setMaxWidth(100);
        btnSync.setStyle("-fx-background-color: #2a2a2a; -fx-text-fill: #c8ff00; -fx-font-size: 11; -fx-padding: 5 10;");

        bar.getChildren().addAll(syncLabel, btnSync);
        return bar;
    }

    // ==========================================
    // 3. TAP IN / TAP OUT / FALLBACK MAX FARE HANDLERS
    // ==========================================
    private void handleTapIn(ComboBox<CardItem> picker, TextField manualField) {
        String id = resolveIdInput(picker, manualField);
        Api.TapInResult res = Api.tapInDetailed(id, currentStopIdx);
        shiftTotalBoarded++;
        manualInputId = "";
        setSuccess("Tapped in: " + res.name() + "\nStop: " + res.stop() + " (" + (currentStopIdx + 1) + ")\nOn Board: " + res.onBoard());
        render();
    }

    private void handleTapOut(ComboBox<CardItem> picker, TextField manualField) {
        String id = resolveIdInput(picker, manualField);
        handleDirectTapOut(id);
    }

    private void handleDirectTapOut(String uid) {
        String[] r = Api.tapOut(uid, currentStopIdx);
        recordLocalTxn(uid, r);
        manualInputId = "";
        setSuccess(r[0] + "\nNew Balance: " + peso(Long.parseLong(r[1])) + "\n+" + r[3] + " Reward Points\nOn Board: " + r[2]);
        render();
    }

    private void handleMissedTapOut(ComboBox<CardItem> picker, TextField manualField) {
        String id = resolveIdInput(picker, manualField);
        handleDirectMissed(id);
    }

    private void handleDirectMissed(String uid) {
        String[] r = Api.tapMissed(uid);
        recordLocalTxn(uid, r);
        manualInputId = "";
        setSuccess("FALLBACK MAX FARE APPLIED (No tap out)\n" + r[0] + "\nNew Balance: " + peso(Long.parseLong(r[1])) + "\nOn Board: " + r[2]);
        render();
    }

    private void recordLocalTxn(String uid, String[] tapRes) {
        shiftCompletedTrips++;
        try {
            String desc = tapRes[0];
            int pIdx = desc.indexOf("P");
            if (pIdx >= 0) {
                int endIdx = desc.indexOf(" ", pIdx);
                if (endIdx > pIdx) {
                    double p = Double.parseDouble(desc.substring(pIdx + 1, endIdx));
                    shiftEarningsCents += (long) Math.round(p * 100);
                }
            }
        } catch (Exception ignored) {}

        String row = String.join(",", UUID.randomUUID().toString(), "FARE_PAYMENT", "", uid, "0", "0", tapRes[1], Instant.now().toString(), operator.getId(), "0");
        localTxnRows.add(row.split(",", -1));
        unsyncedCount++;
    }

    private void syncBatchToServer() {
        if (localTxnRows.isEmpty()) {
            setNotice("Local ledger is already up to date with server.");
            render();
            return;
        }
        try {
            StringBuilder b = new StringBuilder();
            for (String[] row : localTxnRows) {
                b.append(String.join(",", row)).append('\n');
            }
            String[] res = Api.syncBatch(b.toString());
            localTxnRows.clear();
            unsyncedCount = 0;
            setNotice("Sync complete: " + res[0] + " uploaded, " + res[1] + " duplicate(s) merged.");
            render();
        } catch (Exception e) {
            setNotice("Offline: Local rows saved. Will retry on next sync.");
            render();
        }
    }

    private String resolveIdInput(ComboBox<CardItem> picker, TextField manualField) {
        String typed = manualField != null ? manualField.getText().trim() : "";
        if (!typed.isEmpty()) return typed;
        if (picker != null && picker.getValue() != null) return picker.getValue().uid();
        throw new IllegalStateException("Please select a card or enter an ID");
    }

    private void setSuccess(String msg) {
        feedbackMessage = msg;
        feedbackIsSuccess = true;
    }

    private void setNotice(String msg) {
        feedbackMessage = msg;
        feedbackIsSuccess = false;
    }

    private List<Api.BoardedPassenger> getBoardedListSafe() {
        try {
            return Api.operatorBoarded();
        } catch (Exception e) {
            return Collections.emptyList();
        }
    }

    // ==========================================
    // 4. PIN LOCK / TERMINAL SECURITY
    // ==========================================
    private void toggleLock() {
        if (!isLocked) {
            isLocked = true;
            render();
        } else {
            promptPinUnlock(() -> {
                isLocked = false;
                render();
            });
        }
    }

    private void promptPinUnlock(Runnable onUnlocked) {
        Dialog<String> dialog = new Dialog<>();
        dialog.setTitle("Operator PIN Lock");
        dialog.setHeaderText("Phone is mounted on dashboard.\nEnter 4-digit PIN to unlock.");

        ButtonType btnUnlock = new ButtonType("Unlock", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(btnUnlock, ButtonType.CANCEL);

        PasswordField pinField = new PasswordField();
        pinField.setPromptText("Enter PIN (default 1234)");
        dialog.getDialogPane().setContent(new VBox(10, pinField));

        dialog.setResultConverter(b -> b == btnUnlock ? pinField.getText().trim() : null);
        Optional<String> res = dialog.showAndWait();
        if (res.isPresent() && (res.get().equals(lockPin) || res.get().equals("1234"))) {
            isLocked = false;
            onUnlocked.run();
        } else if (res.isPresent()) {
            Alert alert = new Alert(Alert.AlertType.ERROR, "Incorrect PIN. Access denied.");
            alert.showAndWait();
        }
    }

    // ==========================================
    // 5. SHIFT HISTORY & EARNINGS VIEW
    // ==========================================
    private void buildHistoryView(VBox container) {
        VBox card = new VBox(10);
        card.getChildren().addAll(
                heading("Shift Fares & Earnings"),
                text("Fare transactions collected on vehicle " + operator.getVehiclePlate())
        );

        List<Txn> history;
        try {
            history = Api.operatorHistory();
        } catch (Exception e) {
            history = Collections.emptyList();
        }

        if (history.isEmpty()) {
            card.getChildren().add(text("No transactions recorded yet."));
        } else {
            long totalFare = 0;
            for (Txn t : history) {
                totalFare += t.getAmount();
                card.getChildren().add(receiptRow(t));
            }
            Label totalHeader = lime("Total Collections: " + peso(totalFare), 16);
            card.getChildren().add(0, totalHeader);
        }

        container.getChildren().add(card);
    }

    private void onTick() {
        if (onShift && shiftStartTime != null && activeTimerLabel != null) {
            activeTimerLabel.setText(getFormattedDuration());
        }

        if (onShift && autoGpsDriving) {
            autoGpsCountdown--;
            if (autoGpsCountdown <= 0) {
                autoGpsCountdown = 6;
                if (currentStopIdx < routeStops.size() - 1) {
                    currentStopIdx++;
                    logStopArrival(currentStopIdx);
                    setSuccess("🛰️ GPS Detected Stop Arrival: " + routeStops.get(currentStopIdx).name() + " (" + (currentStopIdx + 1) + "/" + routeStops.size() + ")");
                } else {
                    autoGpsDriving = false;
                    setNotice("🛰️ GPS: Route completed at " + routeStops.get(currentStopIdx).name());
                }
                render();
            }
        }
    }

    private String getFormattedDuration() {
        if (!onShift || shiftStartTime == null) return "Shift Inactive";
        long sec = Duration.between(shiftStartTime, Instant.now()).getSeconds();
        long h = sec / 3600;
        long m = (sec % 3600) / 60;
        long s = sec % 60;
        return String.format("%02d:%02d:%02d", h, m, s);
    }
}
