package com.farex.ui.operator;

import java.util.Locale;
import java.util.function.IntConsumer;

import com.farex.Api;
import com.farex.operator.GpsSimulator;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Polygon;
import javafx.scene.text.Font;

import com.farex.ui.common.CommonUi;
import com.farex.ui.common.Icons;
import com.farex.ui.common.Navigator;
import com.farex.ui.common.Theme;
import com.farex.ui.trip.TripUi;

// Ready Mode Terminal (tab 1 of OperatorShell): boarding QR, onboard / collected counts,
// the GPS card (current stop, Prev / Next Stop / Fix GPS, Auto Drive), the simulated tap reader
// and END SHIFT.
// Before a shift starts it only shows a START SHIFT button.
public class ReadyModeView extends StackPane {

    private final IntConsumer showTab;
    private final VBox gpsCard = OperatorUi.card();
    private final HBox countCards = new HBox(10);
    private final VBox simulateCard = OperatorUi.card();

    public ReadyModeView(IntConsumer showTab) {
        this.showTab = showTab;
        if (OperatorState.isOnShift()) {
            buildTerminal();
        } else {
            buildNotStarted();
        }
    }

    // ----- No shift yet -----

    private void buildNotStarted() {
        VBox message = new VBox(8,
                TripUi.bold("Your shift hasn't started", 18),
                CommonUi.small("Start a shift to accept xPress Cards and app QR taps.", Theme.WHITE));
        message.setAlignment(Pos.CENTER);
        message.setPadding(new Insets(0, 0, 90, 0));

        Button start = OperatorUi.startShiftButton(() -> {
            OperatorState.startShift();
            showTab.accept(1);
        });
        StackPane.setAlignment(start, Pos.BOTTOM_CENTER);
        StackPane.setMargin(start, new Insets(0, 18, 10, 18));
        getChildren().addAll(message, start);
    }

    // ----- During a shift -----

    private void buildTerminal() {
        refreshLive();

        VBox content = new VBox(10, statusRow(), titleRow(), qrCard(), countCards, gpsCard, simulateCard);
        content.setPadding(new Insets(2, 18, 96, 18));   // bottom space so END SHIFT hides nothing

        ScrollPane scroll = new ScrollPane(content);
        scroll.setFitToWidth(true);
        scroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scroll.setVbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scroll.setStyle("-fx-background: transparent; -fx-background-color: transparent;");

        Button end = OperatorUi.endShiftButton(EndShiftDialog::show);
        StackPane.setAlignment(end, Pos.BOTTOM_CENTER);
        StackPane.setMargin(end, new Insets(0, 18, 10, 18));

        getChildren().addAll(scroll, end);

        // Auto Drive moves the jeep while this screen is open: redraw when it does
        OperatorState.setGpsListener(this::refreshLive);
        sceneProperty().addListener((obs, was, now) -> {
            if (now == null) {
                OperatorState.setGpsListener(null);
            }
        });
    }

    // The parts that change: counts, the GPS card, the tap reader
    private void refreshLive() {
        countCards.getChildren().setAll(onboardCard(), collectedCard());
        gpsCard.getChildren().setAll(gpsContent());
        simulateCard.getChildren().setAll(simulateContent());
    }

    // ----- "● SHIFT IN PROGRESS ............ ABC-123" -----

    private Node statusRow() {
        Label status = CommonUi.smallBold("SHIFT IN PROGRESS", Theme.LIME);
        status.setFont(Theme.bold(9));
        status.setGraphic(new Circle(3, Theme.LIME));
        status.setGraphicTextGap(6);

        Label plate = CommonUi.smallBold(OperatorState.PLATE, Theme.WHITE);
        plate.setPadding(new Insets(3, 9, 3, 9));
        plate.setBackground(Theme.rounded(Color.web("#2A2A2A"), 4));

        return spread(status, plate);
    }

    // ----- "Ready for passengers ....... Philcoa — Fairview" -----

    private Node titleRow() {
        Label title = TripUi.bold("Ready for passengers", 15);
        title.setWrapText(false);
        title.setMinWidth(Region.USE_PREF_SIZE);
        Label route = CommonUi.small("Philcoa — Fairview", Theme.WHITE);
        route.setFont(Font.font("System", 10));
        return spread(title, route);
    }

    // ----- QR card -----

    private Node qrCard() {
        Label nfc = CommonUi.smallBold("Tap card or scan QR to board.", Theme.WHITE);
        nfc.setGraphic(Icons.view("icon_op_nfc", 13));
        nfc.setGraphicTextGap(6);

        VBox card = OperatorUi.card(
                TripUi.bold("App users: scan to board", 15),
                new JeepneyQr(150),
                nfc,
                CommonUi.small("FAREX:JEEPNEY:" + OperatorState.PLATE, Theme.GRAY));
        card.setAlignment(Pos.TOP_CENTER);
        card.setSpacing(10);
        card.setPadding(new Insets(14, 14, 12, 14));
        return card;
    }

    // ----- ONBOARD 14 / 22 max  and  COLLECTED ₱182.00 -----

    private VBox onboardCard() {
        return countCard("ONBOARD", "icon_op_people",
                String.valueOf(TapTerminal.onBoardCount()), "/ " + OperatorState.CAPACITY + " max");
    }

    private VBox collectedCard() {
        // ₱182.00 -> big "₱182" + small ".00"
        String full = OperatorUi.peso(OperatorState.collectedCents());
        int dot = full.lastIndexOf('.');
        return countCard("COLLECTED", "icon_op_cash", full.substring(0, dot), full.substring(dot));
    }

    private VBox countCard(String label, String icon, String big, String small) {
        Label number = TripUi.bold(big, 28);
        number.setTextFill(Theme.LIME);
        number.setWrapText(false);
        Label rest = CommonUi.small(small, Theme.WHITE);
        rest.setFont(Font.font("System", 11));
        HBox value = new HBox(1, number, rest);
        value.setAlignment(Pos.BASELINE_LEFT);

        VBox card = OperatorUi.card(OperatorUi.cardTop(label, Theme.WHITE, Icons.view(icon, 16)), value);
        card.setSpacing(4);
        HBox.setHgrow(card, Priority.ALWAYS);
        card.setMaxWidth(Double.MAX_VALUE);
        card.setPrefWidth(0);   // both cards share the width equally
        return card;
    }

    // ----- GPS card -----

    private Node[] gpsContent() {
        GpsSimulator gps = OperatorState.getGps();
        Api.StopCoord stop = gps.getCurrentStop();
        Api.StopCoord next = gps.getNextStop();
        boolean auto = gps.isAutoDriving();

        Label tracker = CommonUi.smallBold(auto ? "AUTO DRIVING" : "GPS TRACKER READY", Theme.LIME);
        tracker.setFont(Theme.bold(9));
        tracker.setGraphic(new Circle(3, Theme.LIME));
        tracker.setGraphicTextGap(6);
        Label accuracy = CommonUi.smallBold("Accuracy: " + OperatorState.accuracyText(), Theme.WHITE);

        Label caption = CommonUi.small("CURRENT JEEPNEY STOP (GPS DETECTED):", Color.web("#BDBDBD"));
        caption.setFont(Theme.bold(9));

        Label stopName = TripUi.bold((stop.index() + 1) + ". " + stop.name(), 16);
        stopName.setTextFill(Theme.LIME);
        stopName.setWrapText(false);
        stopName.setMinWidth(Region.USE_PREF_SIZE);
        Label coords = CommonUi.small(String.format(Locale.US, "%.4f° N, %.4f° E", stop.lat(), stop.lng()),
                Theme.WHITE);
        coords.setFont(Font.font("System", 8));
        coords.setMinWidth(Region.USE_PREF_SIZE);

        // Auto Drive: tap to switch ACTIVE / OFF
        Label autoDrive = CommonUi.small("Auto Drive Route: " + (auto ? "ACTIVE" : "OFF"), Theme.WHITE);
        autoDrive.setGraphic(arrowIcon());
        autoDrive.setGraphicTextGap(6);
        autoDrive.setCursor(Cursor.HAND);
        autoDrive.setOnMouseClicked(e -> {
            OperatorState.toggleAutoDrive();
            refreshLive();
        });
        String nextText = (next == null) ? "End of route" : "Next: " + next.name();

        VBox.setMargin(caption, new Insets(4, 0, 0, 0));
        return new Node[] {
            spread(tracker, accuracy),
            OperatorUi.divider(),
            caption,
            spread(stopName, coords),
            stopButtons(gps),
            spread(autoDrive, CommonUi.smallBold(nextText, Theme.LIME)),
        };
    }

    // "‹ Prev"   "Next Stop ›"   "Fix GPS"  (at the terminal the middle one is "New trip")
    private Node stopButtons(GpsSimulator gps) {
        Button prev = OperatorUi.outlineButton("‹ Prev", () -> {
            OperatorState.previousStop();
            refreshLive();
        });
        prev.setDisable(!gps.hasPrevStop());

        boolean atEnd = OperatorState.isAtTerminal();
        Button next = CommonUi.limeButton(atEnd ? "New trip ↺" : "Next Stop ›", () -> {
            if (OperatorState.isAtTerminal()) {
                TapTerminal.chargeMaxFares();     // still on board when leaving the terminal
                OperatorState.startNewTrip();
            } else {
                OperatorState.nextStop();
            }
            refreshLive();
            showMaxFareIfAny();
        });
        next.setFont(Theme.bold(15));
        next.setPadding(new Insets(12, 8, 12, 8));
        HBox.setHgrow(next, Priority.ALWAYS);

        Button fix = OperatorUi.outlineButton("Fix GPS", () -> {
            OperatorState.fixGps();
            refreshLive();
        });
        fix.setFont(Theme.bold(10));

        for (Button side : new Button[] {prev, fix}) {
            side.setMaxWidth(Region.USE_PREF_SIZE);
            side.setMinWidth(72);
            side.setMaxHeight(Double.MAX_VALUE);
        }
        HBox row = new HBox(8, prev, next, fix);
        row.setAlignment(Pos.CENTER);
        VBox.setMargin(row, new Insets(4, 0, 2, 0));
        return row;
    }

    // ----- Simulated tap reader -----

    // "SIMULATE CARD TAP (NFC) / QR    Preview states", the drop-down, the connection switch
    private Node[] simulateContent() {
        Label preview = CommonUi.smallBold("Preview states", Theme.LIME);
        preview.setCursor(Cursor.HAND);
        preview.setOnMouseClicked(e -> Navigator.showOverlay(new TapPreviewDialog()));

        Label prompt = CommonUi.small("Choose a card or app user to tap", Theme.WHITE);
        prompt.setFont(Font.font("System", 12));
        Label chevron = CommonUi.smallBold("▾", Theme.WHITE);
        chevron.setFont(Theme.bold(14));
        HBox dropDown = spread(prompt, chevron);
        dropDown.setPadding(new Insets(10, 12, 10, 12));
        dropDown.setBackground(Theme.rounded(Color.web("#141414"), 8));
        dropDown.setBorder(new javafx.scene.layout.Border(new javafx.scene.layout.BorderStroke(
                Color.web("#3A3A3A"), javafx.scene.layout.BorderStrokeStyle.SOLID,
                new javafx.scene.layout.CornerRadii(8), new javafx.scene.layout.BorderWidths(1))));
        dropDown.setCursor(Cursor.HAND);
        dropDown.setOnMouseClicked(e -> openPicker());

        // Offline: cards still work (their balance is on the chip), app taps wait for sync
        boolean online = OperatorState.isOnline();
        Label connection = CommonUi.small("Jeep connection: " + (online ? "Online" : "Offline")
                + " · tap to switch", online ? Theme.GRAY : OperatorUi.AMBER);
        connection.setCursor(Cursor.HAND);
        connection.setOnMouseClicked(e -> {
            OperatorState.toggleOnline();
            refreshLive();
        });

        return new Node[] {
            OperatorUi.cardTop("SIMULATE CARD TAP (NFC) / QR", Theme.WHITE, preview),
            dropDown,
            connection,
        };
    }

    private void openPicker() {
        Navigator.showOverlay(new TapPickerDialog(this::tap));
    }

    // First tap = tap in, second = tap out; then the matching pop-up
    private void tap(TapRider rider) {
        TapResult result = TapTerminal.tap(rider);
        refreshLive();
        Navigator.showOverlay(new TapResultDialog(result, this::openPicker));
    }

    private void showMaxFareIfAny() {
        TapResult maxFare = TapTerminal.takeMaxFareResult();
        if (maxFare != null) {
            Navigator.showOverlay(new TapResultDialog(maxFare, null));
        }
    }

    // ----- Helpers -----

    // Left node ......... right node
    private HBox spread(Node left, Node right) {
        Region fill = new Region();
        HBox.setHgrow(fill, Priority.ALWAYS);
        HBox row = new HBox(8, left, fill, right);
        row.setAlignment(Pos.CENTER_LEFT);
        return row;
    }

    // Small navigation arrow in front of "Auto Drive Route"
    private Polygon arrowIcon() {
        Polygon arrow = new Polygon(0, 5, 11, 0, 6, 11, 5, 6);
        arrow.setFill(Theme.LIME);
        return arrow;
    }
}
