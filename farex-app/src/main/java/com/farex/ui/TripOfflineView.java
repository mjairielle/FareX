package com.farex.ui;

import javafx.scene.layout.VBox;

// Trip Planner / 12: the search lost its connection; the chosen stops are kept
public class TripOfflineView extends SubScreen {

    public TripOfflineView() {
        super("Trip Planner", "tripPlanner");

        VBox problem = FetchUi.panel(
                TripUi.bold("Unable to load routes", 15),
                FetchUi.subtitle("Check your Wi-Fi or mobile data. Route results may be unavailable "
                        + "while you’re offline."),
                FetchUi.smallBold(TripState.summary(), Theme.LIME));
        problem.setSpacing(8);

        body.setSpacing(12);
        body.getChildren().addAll(
                FetchUi.resultHeader(WarningDialog.warningIcon(),
                        "Connection lost",
                        "Your locations are saved. Reconnect and try searching again."),
                problem,
                FetchUi.spacer(),
                FetchUi.limeButton("Try again", () -> Navigator.go("tripSearching")),
                FetchUi.link("Edit locations", () -> Navigator.go("tripPlanner")));
    }
}
