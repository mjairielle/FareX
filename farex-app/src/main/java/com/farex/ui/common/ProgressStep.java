package com.farex.ui.common;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.util.Duration;

// Base for the "…ing" steps (Fetching your load, Redeeming your points, ...) shown inside a
// StepScreen. It lights up numbered steps one by one, then calls onFinished().
// Demo: Esc acts like the connection dropping and calls onFailed().
// Each child decides what finishing and failing mean: FetchProgressStep, RedeemProgressStep,
// LostFreezeStep, LostTransferStep, TripSearchStep.
public abstract class ProgressStep extends VBox {

    private static final Color WAITING = Color.web("#BDBDBD");
    private static final double SECONDS_PER_STEP = 1.6;

    private Label[] stepLabels = new Label[0];
    private Timeline timeline;

    protected ProgressStep() {
        super(12);
        VBox.setVgrow(this, Priority.ALWAYS);
    }

    protected abstract void onFinished();

    protected abstract void onFailed();

    // ----- Building blocks -----

    // "1   Payment verified", "2   Writing load…" ... numbered automatically
    protected Label[] steps(String... texts) {
        stepLabels = new Label[texts.length];
        for (int i = 0; i < texts.length; i++) {
            stepLabels[i] = new Label((i + 1) + "   " + texts[i]);
        }
        return stepLabels;
    }

    // Black bar at the bottom, e.g. "Fetching…" (a label, not a button: nothing to press)
    protected Label busyBar(String text) {
        Label bar = new Label(text);
        bar.setTextFill(Theme.WHITE);
        bar.setFont(Theme.bold(14));
        bar.setMaxWidth(Double.MAX_VALUE);
        bar.setAlignment(Pos.CENTER);
        bar.setPadding(new Insets(14));
        bar.setBackground(Theme.rounded(Color.web("#050505"), 12));
        return bar;
    }

    // ----- Running -----

    // firstActive = 1 -> step 1 done, step 2 running; then 1.6 s per step until all are done.
    // Example with 3 steps: step 3 at 1.6 s, onFinished() at 3.2 s.
    protected void start(int firstActive) {
        showStep(firstActive);

        timeline = new Timeline();
        int seconds = 0;
        for (int step = firstActive + 1; step <= stepLabels.length; step++) {
            seconds++;
            int show = step;
            boolean last = (step == stepLabels.length);
            timeline.getKeyFrames().add(new KeyFrame(Duration.seconds(seconds * SECONDS_PER_STEP), e -> {
                if (last) {
                    finish();
                } else {
                    showStep(show);
                }
            }));
        }
        timeline.play();
        listenForEsc();
    }

    // active = 1 -> step 1 lime (done), step 2 white bold (now), step 3 gray (waiting)
    private void showStep(int active) {
        for (int i = 0; i < stepLabels.length; i++) {
            Label label = stepLabels[i];
            if (i < active) {
                label.setTextFill(Theme.LIME);
                label.setFont(Font.font(Theme.FONT, 12));
            } else if (i == active) {
                label.setTextFill(Theme.WHITE);
                label.setFont(Theme.bold(12));
            } else {
                label.setTextFill(WAITING);
                label.setFont(Font.font(Theme.FONT, 12));
            }
        }
    }

    private void finish() {
        // Left with the back arrow while it ran: this step is no longer on screen
        if (getScene() == null) {
            return;
        }
        onFinished();
    }

    private void listenForEsc() {
        setFocusTraversable(true);
        setOnKeyPressed(e -> {
            if (e.getCode() == KeyCode.ESCAPE) {
                timeline.stop();
                onFailed();
            } else {
                onOtherKey(e.getCode());
            }
        });
        Platform.runLater(this::requestFocus);
    }

    // A child can react to more demo keys (Trip search: N = no route found)
    protected void onOtherKey(KeyCode code) {
    }

    // Stop the steps without finishing, e.g. "Cancel search"
    protected void stopSteps() {
        if (timeline != null) {
            timeline.stop();
        }
    }
}
