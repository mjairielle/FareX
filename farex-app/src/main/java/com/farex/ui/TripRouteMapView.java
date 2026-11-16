package com.farex.ui;

import javafx.scene.layout.VBox;

// Trip Planner / 19: the chosen route on the map. "Back to route details" returns to that route.
public class TripRouteMapView extends SubScreen {

    public TripRouteMapView() {
        super("Trip Planner", "tripRoute");

        VBox heading = new VBox(6,
                FetchUi.title("Your route at a glance"),
                FetchUi.subtitle(TripState.summary()));

        body.setSpacing(12);
        body.getChildren().addAll(
                heading,
                TripUi.mapCard(190),
                TripUi.routeCard(),
                FetchUi.subtitle("This map provides area context only. Follow the stop-by-stop directions "
                        + "in route details and confirm boarding with the driver."),
                FetchUi.spacer(),
                FetchUi.limeButton("Back to route details", () -> Navigator.go("tripRoute")),
                FetchUi.link("Choose another route", () -> Navigator.go("tripRoutes")));
    }
}
