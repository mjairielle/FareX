package com.farex.ui;

import javafx.geometry.Insets;
import javafx.scene.Cursor;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;

import farex.shared.fare.DiscountedFare;
import farex.shared.fare.Fares;
import farex.shared.fare.RegularFare;

// Operator / Preview tap states: "Choose a tap result". Shows any result pop-up with sample
// values for the presentation. Nothing is charged or saved.
public class TapPreviewDialog extends StackPane {

    public TapPreviewDialog() {
        setBackground(Theme.solid(Color.rgb(0, 0, 0, 0.45)));

        String stop = OperatorState.getGps().getCurrentStop().name();

        VBox box = new VBox(8,
                TripUi.eyebrow("PROTOTYPE PREVIEW"),
                TripUi.bold("Choose a tap result", 18),
                FetchUi.small("Simulated taps · no real charges", Theme.WHITE),
                option("Card tap in", () -> TapResult.tapIn(sampleCard(), stop, "Logged: 7:42 AM · GPS saved")),
                option("App tap in · online", () -> TapResult.tapIn(sampleApp(), stop, "Server confirmed · GPS saved")),
                option("Tap out · fare charged", TapPreviewDialog::sampleTapOut),
                option("Insufficient balance", TapResult::insufficient),
                option("Card unreadable", TapResult::unreadable),
                option("Invalid QR", TapResult::invalidQr),
                option("No tap out · max fare", TapPreviewDialog::sampleMaxFare),
                option("App tap · offline pending", () -> TapResult.appOffline(sampleApp(), stop)),
                backRow());
        box.setPadding(new Insets(18));
        box.setMaxSize(290, VBox.USE_PREF_SIZE);
        box.setBackground(Theme.rounded(Color.web("#1E1E1E"), 14));
        getChildren().add(box);
    }

    // ----- Rows -----

    private interface Sample {
        TapResult make();
    }

    // Opens the sample pop-up; its Try again / close comes back to this menu
    private Node option(String text, Sample sample) {
        Label row = darkRow(text);
        row.setOnMouseClicked(e -> Navigator.showOverlay(
                new TapResultDialog(sample.make(), () -> Navigator.showOverlay(new TapPreviewDialog()))));
        return row;
    }

    private Node backRow() {
        Label row = darkRow("Back to shift");
        row.setOnMouseClicked(e -> Navigator.closeOverlay());
        return row;
    }

    private Label darkRow(String text) {
        Label row = FetchUi.smallBold(text, Theme.WHITE);
        row.setFont(Theme.bold(11));
        row.setMaxWidth(Double.MAX_VALUE);
        row.setPadding(new Insets(9, 12, 9, 12));
        row.setBackground(Theme.rounded(Color.web("#141414"), 6));
        row.setCursor(Cursor.HAND);
        return row;
    }

    // ----- Sample values (not in the demo data, so nothing real changes) -----

    private static TapRider sampleCard() {
        return new TapRider(new DemoCard("0000006767", "Sample", 676.50, "--"), "REGULAR");
    }

    private static TapRider sampleApp() {
        return new TapRider("APP-0821", "App user", "REGULAR", 240.00);
    }

    // Student, Philcoa -> Ever Gotesco (3.29 km): ₱19.57 regular, ₱15.66 after 20% off
    private static TapResult sampleTapOut() {
        double km = 3.29;
        long fare = Fares.fare(km, new DiscountedFare());
        long base = Fares.fare(km, new RegularFare());
        return TapResult.tapOut(new ShiftRecord(false, "**** 6767", "Philcoa", "Ever Gotesco", km,
                fare, base, 676.50 - fare / 100.0, false, false));
    }

    // Whole route Philcoa -> Fairview Terminal (13.53 km) at the regular fare: ₱40.05
    private static TapResult sampleMaxFare() {
        double km = 13.53;
        long fare = Fares.fare(km, new RegularFare());
        return TapResult.maxFare(new ShiftRecord(false, "**** 9124", "Philcoa", "Route end", km,
                fare, fare, 120.00, true, false), 0);
    }
}
