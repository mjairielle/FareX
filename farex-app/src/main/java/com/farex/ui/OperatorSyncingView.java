package com.farex.ui;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.scene.control.Label;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.util.Duration;

// Operator / O8a "Uploading 12 records…": a lime bar fills one record at a time (0.25 s each).
// Ends on All records synced. With the jeep's connection set to Offline (Ready Mode) it stops
// after the first record and shows "No connection".
// Demo: Esc = the upload fails (O8d).
public class OperatorSyncingView extends SubScreen {

    private static final double SECONDS_PER_RECORD = 0.25;

    private final int total = OperatorState.pendingRecords();
    private final Region fill = new Region();
    private final Label progress = FetchUi.small("", Theme.WHITE);
    private final Timeline timeline = new Timeline();
    private int uploaded = 0;

    public OperatorSyncingView() {
        super("Operator Mode", (String) null);

        // ----- Progress bar: gray track, lime fill -----
        fill.setBackground(Theme.rounded(Theme.LIME, 4));
        fill.setMaxWidth(0);
        StackPane track = new StackPane(fill);
        StackPane.setAlignment(fill, javafx.geometry.Pos.CENTER_LEFT);
        track.setMinHeight(7);
        track.setMaxHeight(7);
        track.setBackground(Theme.rounded(Color.web("#333333"), 4));

        body.setSpacing(12);
        body.getChildren().addAll(
                TripUi.eyebrow("DATA SYNC"),
                TripUi.bold("Sync your records", 22),
                OperatorSyncView.countPanel(false),
                TripUi.bold("Uploading " + total + (total == 1 ? " record…" : " records…"), 15),
                track,
                progress,
                FetchUi.spacer(),
                FetchUi.link("Cancel upload", () -> {
                    timeline.stop();
                    Navigator.go("operatorSync");
                }));
        VBox.setMargin(track, new Insets(2, 0, 0, 0));

        // The track's width is only known after layout, so the fill is sized from it each step
        track.widthProperty().addListener((obs, was, now) -> showProgress(track));
        for (int i = 1; i <= Math.max(total, 1); i++) {
            int step = i;
            timeline.getKeyFrames().add(new KeyFrame(Duration.seconds(step * SECONDS_PER_RECORD), e -> {
                if (!OperatorState.isOnline() && step >= 2) {
                    timeline.stop();
                    finish();
                    return;
                }
                uploaded = Math.min(step, total);
                showProgress(track);
                if (step >= total) {
                    finish();
                }
            }));
        }
        showProgress(track);
        timeline.play();
        listenForEsc();
    }

    // "6 of 12 uploaded · keep this screen open"
    private void showProgress(StackPane track) {
        double share = (total == 0) ? 1 : (double) uploaded / total;
        fill.setMinWidth(track.getWidth() * share);
        fill.setMaxWidth(track.getWidth() * share);
        progress.setText(uploaded + " of " + total + " uploaded · keep this screen open");
    }

    private void finish() {
        if (getScene() == null) {
            return;     // left with Cancel
        }
        if (!OperatorState.isOnline()) {
            Navigator.go("operatorSyncOffline");
        } else {
            OperatorState.syncAll();
            Navigator.go("operatorSyncDone");
        }
    }

    private void listenForEsc() {
        setFocusTraversable(true);
        setOnKeyPressed(e -> {
            if (e.getCode() == KeyCode.ESCAPE) {
                timeline.stop();
                Navigator.go("operatorSyncFailed");
            }
        });
        Platform.runLater(this::requestFocus);
    }
}
