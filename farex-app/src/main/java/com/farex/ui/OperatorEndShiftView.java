package com.farex.ui;

import javafx.geometry.Insets;
import javafx.scene.Group;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;

// Operator / O9: "End this shift?" (from END SHIFT, or Exit Operator Mode during a shift)
public class OperatorEndShiftView extends SubScreen {

    public OperatorEndShiftView() {
        super("Operator Mode", "operatorShell");

        int pending = OperatorState.pendingRecords();

        VBox heading = new VBox(8,
                TripUi.eyebrow("CONFIRM YOUR CHOICE"),
                TripUi.bold("End this shift?", 22),
                FetchUi.subtitle("NFC and QR will stop. You can start a new shift anytime."));

        Group warning = WarningDialog.warningIcon();
        VBox.setMargin(warning, new Insets(6, 0, 0, 0));

        body.getChildren().addAll(heading, warning, syncPanel(pending), FetchUi.spacer(),
                FetchUi.limeButton("End shift", () -> {
                    OperatorState.endShift();
                    Navigator.go("operatorSummary");
                }),
                FetchUi.link("Keep driving", () -> Navigator.go("operatorShell")));
        if (pending > 0) {
            body.getChildren().add(FetchUi.link("Sync first", () -> Navigator.go("operatorSync")));
        }
    }

    // "12 records not yet synced / Sync before ending? / Records stay safely saved..."
    private VBox syncPanel(int pending) {
        if (pending == 0) {
            return FetchUi.panel(TripUi.bold("All records synced", 15),
                    FetchUi.small("Nothing is waiting to be sent.", Theme.WHITE));
        }
        Label count = TripUi.bold(pending + (pending == 1 ? " record" : " records") + " not yet synced", 15);
        count.setTextFill(Theme.LIME);
        Label saved = FetchUi.small("Records stay safely saved on this device if you end now.", Theme.WHITE);
        saved.setWrapText(true);

        VBox panel = FetchUi.panel(count, TripUi.bold("Sync before ending?", 13), saved);
        panel.setSpacing(6);
        panel.setPadding(new Insets(16, 14, 16, 14));
        return panel;
    }
}
