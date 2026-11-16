package com.farex.ui;

import javafx.scene.layout.VBox;

// Trip Planner / 16: off-route warning while tracking
public class TripOffRouteView extends SubScreen {

    public TripOffRouteView() {
        super("Trip Planner", "tripTracking");

        VBox heading = new VBox(6,
                FetchUi.title("Check your route"),
                FetchUi.subtitle("Your location appears away from the selected route."));

        body.setSpacing(12);
        body.getChildren().addAll(
                heading,
                TripUi.centerMessage(WarningDialog.warningIcon(),
                        "You may be off route",
                        "GPS can be inaccurate. Check your surroundings and confirm the route with the driver. "
                                + "Do not use your phone while crossing.",
                        "Stay safe. Replan once you’re in a safe place."),
                FetchUi.spacer(),
                TripUi.routeCard(),
                FetchUi.limeButton("Continue tracking", () -> Navigator.go("tripTracking")),
                // Ends tracking and goes back to the planner with the same stops
                FetchUi.link("End trip & replan", () -> Navigator.go("tripPlanner")));
    }
}
