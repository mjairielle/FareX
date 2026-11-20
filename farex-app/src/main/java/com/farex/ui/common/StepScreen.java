package com.farex.ui.common;

import java.util.ArrayDeque;
import java.util.Deque;

// One screen for a whole flow (Fetch Load, Redeem, Buy Load…). Instead of opening a new page
// for every step, the body is rebuilt in place, and the back arrow returns to the previous step.
// Example (Fetch Load): select -> hold card -> fetching. Back on "hold card" shows "select" again;
// back on "select" leaves to exitRoute ("dashboard").
public abstract class StepScreen extends SubScreen {

    private final String exitRoute;
    private final String defaultTitle;
    private String title;
    private boolean headerShown = true;
    private final javafx.geometry.Insets defaultPadding;
    private final Deque<Runnable> previous = new ArrayDeque<>();
    private Runnable current;

    protected StepScreen(String title, String exitRoute) {
        super(title, (Runnable) null);
        this.exitRoute = exitRoute;
        this.defaultTitle = title;
        this.defaultPadding = body.getPadding();
        this.title = title;
        // The header was built with no arrow; put one in that calls back()
        rebuildHeader();
    }

    // ----- Header -----

    // A step with its own Figma title, e.g. Buy Load's "Payment" step. Each new step starts
    // with the screen's own title again.
    protected void setTitle(String newTitle) {
        title = newTitle;
        rebuildHeader();
    }

    // A step drawn without the lime bar (Buy Load's OTP has its own back arrow)
    protected void setHeaderShown(boolean shown) {
        headerShown = shown;
        rebuildHeader();
    }

    private void rebuildHeader() {
        javafx.scene.layout.StackPane header = SubScreen.header(title, this::back);
        header.setVisible(headerShown);
        header.setManaged(headerShown);
        getChildren().set(0, header);
    }

    // ----- Moving between steps -----

    // Go forward: the step builder fills body; the current step is remembered for back()
    protected void showStep(Runnable step) {
        if (current != null) {
            previous.push(current);
        }
        draw(step);
    }

    // Swap the current step for another without remembering it, e.g. "Searching…" -> the results,
    // so back from the results skips the search
    protected void replaceStep(Runnable step) {
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
        if (!title.equals(defaultTitle) || !headerShown) {
            title = defaultTitle;
            headerShown = true;
            rebuildHeader();
        }
        body.getChildren().clear();
        body.setSpacing(12);
        body.setPadding(defaultPadding);   // a full-screen step (a map) can remove it
        step.run();
    }
}
