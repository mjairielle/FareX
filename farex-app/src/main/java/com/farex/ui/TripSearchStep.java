package com.farex.ui;

import javafx.geometry.Insets;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.VBox;

// Trip Planner / 06 "Searching routes…": fake route search.
// Demo keys: N = no route found, Esc = connection lost.
public class TripSearchStep extends ProgressStep {

    private final Runnable onFound;
    private final Runnable onConnectionLost;
    private final Runnable onNoRoute;

    public TripSearchStep(Runnable onFound, Runnable onConnectionLost, Runnable onNoRoute, Runnable onCancel) {
        this.onFound = onFound;
        this.onConnectionLost = onConnectionLost;
        this.onNoRoute = onNoRoute;

        VBox heading = new VBox(8,
                FetchUi.title("Searching routes…"),
                FetchUi.subtitle(TripState.summary()));
        VBox.setMargin(heading, new Insets(0, 0, 6, 0));

        VBox panel = FetchUi.panel(steps(
                "Match nearby stops",
                "Search available connections",
                "Rank up to three routes"));
        panel.getChildren().add(FetchUi.small(
                "Shortest distance · Fewest transfers · Cheapest fare", Theme.WHITE));
        panel.setPadding(new Insets(16, 14, 18, 14));

        VBox tip = FetchUi.panel(
                TripUi.bold("A little planning goes a long way", 12),
                FetchUi.subtitle("Fare and travel time are estimates, not live transport information."));
        tip.setSpacing(4);

        getChildren().addAll(
                heading,
                panel,
                tip,
                FetchUi.small("Having connection trouble?", Theme.WHITE),
                FetchUi.spacer(),
                FetchUi.link("Cancel search", () -> {
                    stopSteps();
                    onCancel.run();
                }));

        start(1);
    }

    @Override
    protected void onFinished() {
        TripState.setRouteIndex(0);   // the first route starts highlighted
        onFound.run();
    }

    @Override
    protected void onFailed() {
        onConnectionLost.run();
    }

    @Override
    protected void onOtherKey(KeyCode code) {
        if (code == KeyCode.N) {
            stopSteps();
            onNoRoute.run();
        }
    }
}
