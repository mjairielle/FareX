package com.farex.ui;

import javafx.geometry.Insets;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.VBox;

// Trip Planner / 06: fake route search.
// Success -> 07 Choose your route. Demo keys: N -> 11 No route found, Esc -> 12 Connection lost
public class TripSearchView extends ProgressScreen {

    public TripSearchView() {
        super("Trip Planner", "tripPlanner");

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

        body.setSpacing(12);
        body.getChildren().addAll(
                heading,
                panel,
                tip,
                FetchUi.small("Having connection trouble?", Theme.WHITE),
                FetchUi.spacer(),
                FetchUi.link("Cancel search", () -> {
                    stopSteps();
                    Navigator.go("tripPlanner");
                }));

        start(1);
    }

    @Override
    protected void onFinished() {
        TripState.setRouteIndex(0);   // the first route starts highlighted
        Navigator.go("tripRoutes");
    }

    @Override
    protected void onFailed() {
        Navigator.go("tripOffline");
    }

    @Override
    protected void onOtherKey(KeyCode code) {
        if (code == KeyCode.N) {
            stopSteps();
            Navigator.go("tripNoRoute");
        }
    }
}
