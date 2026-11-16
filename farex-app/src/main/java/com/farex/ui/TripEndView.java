package com.farex.ui;

import javafx.scene.layout.VBox;

// Trip Planner / 17: confirm ending the trip before reaching the destination
public class TripEndView extends SubScreen {

    public TripEndView() {
        super("Trip Planner", "tripTracking");

        VBox heading = new VBox(6,
                FetchUi.title("End this trip?"),
                FetchUi.subtitle("Location tracking will stop. You can plan another journey anytime."));

        body.setSpacing(12);
        body.getChildren().addAll(
                heading,
                TripUi.centerMessage(WarningDialog.warningIcon(),
                        "Not at your destination yet?",
                        "Choose Keep tracking to stay with this trip.",
                        null),
                FetchUi.spacer(),
                TripUi.routeCard(),
                FetchUi.limeButton("End trip", () -> {
                    TripState.clear();
                    Navigator.go("routes");
                }),
                FetchUi.link("Keep tracking", () -> Navigator.go("tripTracking")));
    }
}
