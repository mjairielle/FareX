package com.farex.ui;

import java.util.ArrayDeque;
import java.util.Deque;

// One screen for a whole flow (Fetch Load, Redeem, Buy Load…). Instead of opening a new page
// for every step, the body is rebuilt in place, and the back arrow returns to the previous step.
// Example (Fetch Load): select -> hold card -> fetching. Back on "hold card" shows "select" again;
// back on "select" leaves to exitRoute ("dashboard").
public abstract class StepScreen extends SubScreen {

    private final String exitRoute;
    private final Deque<Runnable> previous = new ArrayDeque<>();
    private Runnable current;

    protected StepScreen(String title, String exitRoute) {
        super(title, (Runnable) null);
        this.exitRoute = exitRoute;
        // The header was built with no arrow; put one in that calls back()
        getChildren().set(0, SubScreen.header(title, this::back));
    }

    // ----- Moving between steps -----

    // Go forward: the step builder fills body; the current step is remembered for back()
    protected void showStep(Runnable step) {
        if (current != null) {
            previous.push(current);
        }
        draw(step);
    }

    // Start over at this step and forget the history, e.g. after picking another card
    protected void restartAt(Runnable step) {
        previous.clear();
        draw(step);
    }

    // Draw the current step again, e.g. after a new profile photo was picked
    protected void redraw() {
        draw(current);
    }

    // Back arrow: previous step, or leave the flow from the first step
    protected void back() {
        if (previous.isEmpty()) {
            onExit();
            Navigator.go(exitRoute);
        } else {
            draw(previous.pop());
        }
    }

    // Leaving the flow with the back arrow; a flow can clear its state here
    protected void onExit() {
    }

    private void draw(Runnable step) {
        current = step;
        body.getChildren().clear();
        body.setSpacing(12);
        step.run();
    }
}
