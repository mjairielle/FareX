package com.farex.ui;

import javafx.geometry.Insets;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

import farex.shared.user.Operator;

// Operator / O10: "Shift complete" with this shift's totals (shown after End shift)
public class OperatorSummaryView extends SubScreen {

    public OperatorSummaryView() {
        super("Operator Mode", "operatorDashboard");

        Operator operator = OperatorState.getOperator();
        int pending = OperatorState.pendingRecords();

        VBox heading = new VBox(6,
                TripUi.eyebrow("SHIFT SUMMARY"),
                TripUi.bold("Shift complete", 22),
                FetchUi.small(operator.getName() + " · " + operator.getVehiclePlate(), Theme.WHITE));

        body.setSpacing(10);
        body.getChildren().addAll(heading, totalPanel(), cardVsAppPanel(), flagsPanel(pending),
                FetchUi.spacer(),
                FetchUi.limeButton("Back to dashboard", () -> Navigator.go("operatorDashboard")),
                FetchUi.link("Transaction history", () -> Navigator.go("operatorHistory")),
                FetchUi.link("Back to Passenger mode", () -> Navigator.go("profile")));
    }

    // TOTAL COLLECTED ₱1,240.00 / 96 passengers · 8 trips
    private VBox totalPanel() {
        int people = OperatorState.getPassengers();
        int trips = OperatorState.getTrips();
        String line = people + (people == 1 ? " passenger" : " passengers")
                + " · " + trips + (trips == 1 ? " trip" : " trips");
        return panel(TripUi.eyebrow("TOTAL COLLECTED"),
                OperatorUi.money(OperatorState.collectedCents(), 30),
                TripUi.bold(line, 14));
    }

    // CARD VS APP: Card ₱860.00   App ₱380.00
    private VBox cardVsAppPanel() {
        HBox split = new HBox(18,
                TripUi.bold("Card " + OperatorUi.peso(OperatorState.getCardCents()), 15),
                TripUi.bold("App " + OperatorUi.peso(OperatorState.getAppCents()), 15));
        Label note = FetchUi.small("Confirmed fares only. Pending app charges excluded.", Theme.WHITE);
        note.setWrapText(true);
        return panel(TripUi.eyebrow("CARD VS APP"), split, note);
    }

    // "2 max-fare charges / 12 records pending sync / Sync now"
    private VBox flagsPanel(int pending) {
        int maxFares = OperatorState.getMaxFareCount();
        VBox panel = panel(TripUi.bold(maxFares + (maxFares == 1 ? " max-fare charge" : " max-fare charges"), 13));
        if (pending > 0) {
            Label waiting = TripUi.bold(pending + (pending == 1 ? " record" : " records") + " pending sync", 13);
            waiting.setTextFill(Theme.LIME);
            panel.getChildren().addAll(waiting, FetchUi.link("Sync now", () -> Navigator.go("operatorSync")));
        } else {
            panel.getChildren().add(FetchUi.small("All records synced", Theme.LIME));
        }
        return panel;
    }

    private VBox panel(javafx.scene.Node... children) {
        VBox panel = FetchUi.panel(children);
        panel.setSpacing(6);
        panel.setPadding(new Insets(12, 14, 12, 14));
        return panel;
    }
}
