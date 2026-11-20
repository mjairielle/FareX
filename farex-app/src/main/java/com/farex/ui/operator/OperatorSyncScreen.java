package com.farex.ui.operator;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.util.Duration;

import com.farex.ui.common.CommonUi;
import com.farex.ui.common.Navigator;
import com.farex.ui.common.PopupDialog;
import com.farex.ui.common.StepScreen;
import com.farex.ui.common.Theme;
import com.farex.ui.common.WarningDialog;
import com.farex.ui.trip.TripUi;

// Operator / O8 sync, all in one screen:
//   "Sync your records" (O8) -> "Uploading 12 records…" (O8a, the bar fills 0.25 s per record)
// All synced (O8b), no connection (O8c) and sync failed (O8d) are pop-ups.
// Jeep connection Offline (Ready Mode) -> no connection after 0.25 s. Demo: Esc while uploading = sync failed.
public class OperatorSyncScreen extends StepScreen {

    private static final double SECONDS_PER_RECORD = 0.25;

    public OperatorSyncScreen() {
        super("Operator Mode", "operatorShell");
        restartAt(this::readyStep);
    }

    // ----- O8: how many records wait (or, with none, "All records synced") -----

    private void readyStep() {
        int pending = OperatorState.pendingRecords();
        if (pending == 0) {
            body.getChildren().addAll(
                    TripUi.eyebrow("DATA SYNC"),
                    TripUi.bold("All records synced", 22),
                    CommonUi.doubleCheck(),
                    countPanel(true),
                    TripUi.bold("Your shift records are up to date.", 14),
                    CommonUi.spacer(),
                    CommonUi.limeButton("Done", this::back));
            return;
        }
        Label intro = CommonUi.subtitle("Send locally saved transactions when you have a connection.");
        intro.setFont(javafx.scene.text.Font.font(Theme.FONT, 13));

        body.getChildren().addAll(
                TripUi.eyebrow("DATA SYNC"),
                TripUi.bold("Sync your records", 22),
                countPanel(false),
                intro,
                wrapped("App balances are confirmed by the server, never while offline."),
                CommonUi.spacer(),
                CommonUi.limeButton("Sync now", () -> showStep(this::uploadingStep)),
                CommonUi.link("Back", this::back));
    }

    // ----- O8a: the upload -----

    private void uploadingStep() {
        body.getChildren().add(new UploadStep());
    }

    // "Uploading 12 records…": the lime bar grows one record at a time, then the result pop-up
    private class UploadStep extends VBox {
        private final int total = OperatorState.pendingRecords();
        private final Region fill = new Region();
        private final StackPane track = new StackPane(fill);
        private final Label progress = CommonUi.small("", Theme.WHITE);
        private final Timeline timeline = new Timeline();
        private int uploaded = 0;

        UploadStep() {
            super(12);
            VBox.setVgrow(this, javafx.scene.layout.Priority.ALWAYS);

            // Progress bar: gray track, lime fill
            fill.setBackground(Theme.rounded(Theme.LIME, 4));
            fill.setMaxWidth(0);
            StackPane.setAlignment(fill, Pos.CENTER_LEFT);
            track.setMinHeight(7);
            track.setMaxHeight(7);
            track.setBackground(Theme.rounded(Color.web("#333333"), 4));
            VBox.setMargin(track, new Insets(2, 0, 0, 0));
            track.widthProperty().addListener((obs, was, now) -> showProgress());

            getChildren().addAll(
                    TripUi.eyebrow("DATA SYNC"),
                    TripUi.bold("Sync your records", 22),
                    countPanel(false),
                    TripUi.bold("Uploading " + total + (total == 1 ? " record…" : " records…"), 15),
                    track,
                    progress,
                    CommonUi.spacer(),
                    CommonUi.link("Cancel upload", () -> {
                        timeline.stop();
                        back();
                    }));

            for (int i = 1; i <= Math.max(total, 1); i++) {
                int record = i;
                timeline.getKeyFrames().add(new KeyFrame(Duration.seconds(record * SECONDS_PER_RECORD),
                        e -> onRecord(record)));
            }
            showProgress();
            timeline.play();

            // Demo: Esc = the upload fails
            setFocusTraversable(true);
            setOnKeyPressed(e -> {
                if (e.getCode() == KeyCode.ESCAPE) {
                    timeline.stop();
                    showProblem(true);
                }
            });
            Platform.runLater(this::requestFocus);
        }

        // Record 1, 2, 3 ... sent. Offline: nothing can be sent, so it stops at the first one
        private void onRecord(int record) {
            if (getScene() == null) {
                timeline.stop();       // left with the back arrow
                return;
            }
            if (!OperatorState.isOnline()) {
                timeline.stop();
                showProblem(false);
                return;
            }
            uploaded = Math.min(record, total);
            showProgress();
            if (record >= total) {
                OperatorState.syncAll();
                showSynced();
            }
        }

        // "6 of 12 uploaded · keep this screen open"; the fill is sized from the track's width
        private void showProgress() {
            double share = (total == 0) ? 1 : (double) uploaded / total;
            fill.setMinWidth(track.getWidth() * share);
            fill.setMaxWidth(track.getWidth() * share);
            progress.setText(uploaded + " of " + total + " uploaded · keep this screen open");
        }
    }

    // ----- Results (pop-ups) -----

    // O8b: 0 records waiting, last synced just now
    private void showSynced() {
        new PopupDialog(CommonUi.doubleCheck(), "All records synced", "Your shift records are up to date.")
                .add(countPanel(true), wrapped("App payments have been sent for server reconciliation. "
                        + "Check history for payment results."))
                .button("Done", () -> Navigator.go("operatorShell"))
                .show();
    }

    // O8c (failed = false): no connection. O8d (failed = true): the sync failed
    private void showProblem(boolean failed) {
        Group warning = WarningDialog.warningIcon();
        warning.setScaleX(0.7);
        warning.setScaleY(0.7);
        String text = failed ? "Sync failed. Your records are still saved."
                : "No connection. Records are safely saved on this device.";

        new PopupDialog(new Group(warning), failed ? "Sync failed" : "No connection", text)
                .add(countPanel(false), wrapped("App balances are confirmed by the server, never while offline."))
                .button("Try again", this::redraw)   // runs the upload step again
                .link("Back", () -> Navigator.go("operatorShell"))
                .show();
    }

    // ----- Helpers -----

    // "12 / records waiting to sync / Last synced: Today, 6:10 PM"
    private Node countPanel(boolean justSynced) {
        int pending = OperatorState.pendingRecords();
        Label count = TripUi.bold(String.valueOf(pending), 34);
        count.setTextFill(Theme.LIME);
        String words = justSynced ? "records waiting"
                : (pending == 1 ? "record" : "records") + " waiting to sync";
        VBox panel = CommonUi.panel(count, TripUi.bold(words, 13),
                CommonUi.small("Last synced: " + (justSynced ? "Just now" : OperatorState.getLastSynced()),
                        Theme.WHITE));
        panel.setSpacing(4);
        panel.setPadding(new Insets(16));
        return panel;
    }

    private Label wrapped(String text) {
        Label label = CommonUi.small(text, Theme.WHITE);
        label.setWrapText(true);
        return label;
    }
}
