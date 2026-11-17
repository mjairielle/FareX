package com.farex.ui;

import javafx.geometry.Insets;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;

// Operator / O8 sync screens that share one layout:
// READY (O8) "Sync your records", SUCCESS (O8b) "All records synced",
// OFFLINE (O8c) no connection, FAILED (O8d) sync failed. The uploading one is OperatorSyncingView.
public class OperatorSyncView extends SubScreen {

    public enum Mode { READY, SUCCESS, OFFLINE, FAILED }

    public OperatorSyncView(Mode mode) {
        super("Operator Mode", "operatorShell");

        // Nothing to send: go straight to the "All records synced" look
        if (mode == Mode.READY && OperatorState.pendingRecords() == 0) {
            mode = Mode.SUCCESS;
        }
        boolean success = (mode == Mode.SUCCESS);

        body.setSpacing(12);
        body.getChildren().addAll(
                TripUi.eyebrow("DATA SYNC"),
                TripUi.bold(success ? "All records synced" : "Sync your records", 22));

        if (success) {
            body.getChildren().add(small(FetchUi.doubleCheck(), 0.8));
        } else if (mode != Mode.READY) {
            body.getChildren().add(small(WarningDialog.warningIcon(), 0.6));
        }

        body.getChildren().add(countPanel(success));
        body.getChildren().addAll(message(mode));
        body.getChildren().add(FetchUi.spacer());

        switch (mode) {
            case READY:
                body.getChildren().addAll(
                        FetchUi.limeButton("Sync now", () -> Navigator.go("operatorSyncing")),
                        FetchUi.link("Back", () -> Navigator.go("operatorShell")));
                break;
            case SUCCESS:
                body.getChildren().add(FetchUi.limeButton("Done", () -> Navigator.go("operatorShell")));
                break;
            default:
                body.getChildren().addAll(
                        FetchUi.limeButton("Try again", () -> Navigator.go("operatorSyncing")),
                        FetchUi.link("Back", () -> Navigator.go("operatorShell")));
        }
    }

    // "12 / records waiting to sync / Last synced: Today, 6:10 PM"
    static VBox countPanel(boolean justSynced) {
        int pending = OperatorState.pendingRecords();
        Label count = TripUi.bold(String.valueOf(pending), 34);
        count.setTextFill(Theme.LIME);
        String words = justSynced ? "records waiting"
                : (pending == 1 ? "record" : "records") + " waiting to sync";
        VBox panel = FetchUi.panel(count, TripUi.bold(words, 13),
                FetchUi.small("Last synced: " + (justSynced ? "Just now" : OperatorState.getLastSynced()),
                        Theme.WHITE));
        panel.setSpacing(4);
        panel.setPadding(new Insets(16));
        return panel;
    }

    // The bold sentence and the small one under the panel
    private Node[] message(Mode mode) {
        String bold;
        String small;
        switch (mode) {
            case SUCCESS:
                bold = "Your shift records are up to date.";
                small = "App payments have been sent for server reconciliation. "
                        + "Check history for payment results.";
                break;
            case OFFLINE:
                bold = "No connection. Records are safely saved on this device.";
                small = "App balances are confirmed by the server, never while offline.";
                break;
            case FAILED:
                bold = "Sync failed. Your records are still saved.";
                small = "App balances are confirmed by the server, never while offline.";
                break;
            default:
                bold = null;
                small = "App balances are confirmed by the server, never while offline.";
        }
        if (bold == null) {
            Label intro = FetchUi.subtitle("Send locally saved transactions when you have a connection.");
            intro.setFont(javafx.scene.text.Font.font("System", 13));
            return new Node[] {intro, wrapped(small)};
        }
        return new Node[] {TripUi.bold(bold, 14), wrapped(small)};
    }

    private Label wrapped(String text) {
        Label label = FetchUi.small(text, Theme.WHITE);
        label.setWrapText(true);
        return label;
    }

    private Group small(Node icon, double scale) {
        icon.setScaleX(scale);
        icon.setScaleY(scale);
        return new Group(icon);
    }
}
