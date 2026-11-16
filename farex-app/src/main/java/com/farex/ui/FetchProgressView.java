package com.farex.ui;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.util.Duration;

// Fetch Load / 03: a fake card read. The steps light up one by one, then the load is added.
// Demo: pressing Esc acts like the card being pulled away -> "Couldn't fetch your load"
public class FetchProgressView extends SubScreen {

    private static final String[] STEPS = {
        "1   Payment verified",
        "2   Writing load to your card…",
        "3   Confirm updated balance"};
    private static final Color WAITING = Color.web("#BDBDBD");

    private final Label[] stepLabels = new Label[STEPS.length];
    private final PendingLoad load;
    private Timeline timeline;

    public FetchProgressView() {
        super("Fetch Load", "fetchHold");

        DemoCard card = FetchLoadState.getCard();
        load = card.getPendingLoad();
        FetchLoadState.setLastLoad(load);   // the failure screen shows this one

        VBox heading = new VBox(8,
                FetchUi.title("Fetching your load…"),
                FetchUi.subtitle("Keep your card in place. Do not close the app until the update is confirmed."));
        VBox.setMargin(heading, new Insets(0, 0, 6, 0));

        // ----- Amount + steps -----
        Label amount = new Label(FetchUi.peso(load.getAmount()));
        amount.setTextFill(Theme.LIME);
        amount.setFont(Theme.bold(30));

        VBox panel = FetchUi.panel(amount, FetchUi.row("Card", FetchUi.masked(card)));
        for (int i = 0; i < STEPS.length; i++) {
            stepLabels[i] = new Label(STEPS[i]);
            panel.getChildren().add(stepLabels[i]);
        }
        panel.setPadding(new Insets(16, 14, 18, 14));
        showStep(1);   // Figma start: step 1 done, step 2 in progress

        // ----- Black "Fetching..." bar (not a button: nothing to press while it runs) -----
        Label fetching = new Label("Fetching…");
        fetching.setTextFill(Theme.WHITE);
        fetching.setFont(Theme.bold(14));
        fetching.setMaxWidth(Double.MAX_VALUE);
        fetching.setAlignment(Pos.CENTER);
        fetching.setPadding(new Insets(14));
        fetching.setBackground(Theme.rounded(Color.web("#050505"), 12));

        body.setSpacing(12);
        body.getChildren().addAll(
                heading,
                panel,
                FetchUi.note("If the connection drops, check the fetch status before trying again. "
                        + "The same top-up can only be collected once."),
                FetchUi.spacer(),
                fetching);

        listenForEsc();
        startTimer();
    }

    // ----- Steps -----

    // active = 1 -> step 1 lime (done), step 2 white bold (now), step 3 gray (waiting)
    private void showStep(int active) {
        for (int i = 0; i < stepLabels.length; i++) {
            Label label = stepLabels[i];
            if (i < active) {
                label.setTextFill(Theme.LIME);
                label.setFont(Font.font("System", 12));
            } else if (i == active) {
                label.setTextFill(Theme.WHITE);
                label.setFont(Theme.bold(12));
            } else {
                label.setTextFill(WAITING);
                label.setFont(Font.font("System", 12));
            }
        }
    }

    // 0 s: writing, 1.6 s: confirming, 3.2 s: done
    private void startTimer() {
        timeline = new Timeline(
                new KeyFrame(Duration.seconds(1.6), e -> showStep(2)),
                new KeyFrame(Duration.seconds(3.2), e -> finish()));
        timeline.play();
    }

    private void finish() {
        // Left with the back arrow mid-read: nothing is added, the load stays pending
        if (getScene() == null) {
            return;
        }
        FetchLoadState.collect(load);
        Navigator.go("fetchSuccess");
    }

    // ----- Esc = card pulled away (demo of the failure screen) -----

    private void listenForEsc() {
        setFocusTraversable(true);
        setOnKeyPressed(e -> {
            if (e.getCode() == KeyCode.ESCAPE) {
                timeline.stop();
                Navigator.go("fetchFailed");
            }
        });
        Platform.runLater(this::requestFocus);
    }
}
