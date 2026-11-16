package com.farex.ui;

import javafx.scene.layout.VBox;

// Trip Planner / 14: location was not allowed; the route can still be used without tracking
public class TripNoLocationView extends SubScreen {

    public TripNoLocationView() {
        super("Trip Planner", "tripLocation");

        VBox heading = new VBox(6,
                FetchUi.title("Tracking unavailable"),
                FetchUi.subtitle("You can continue planning without sharing your location."));

        body.setSpacing(12);
        body.getChildren().addAll(
                heading,
                TripUi.centerMessage(WarningDialog.warningIcon(),
                        "Location access is needed",
                        "To track a trip, enable device location and allow location access while using FareX.",
                        "Your selected route is still available."),
                FetchUi.spacer(),
                FetchUi.subtitle("You can continue planning without sharing your location."),
                TripUi.routeCard(),
                FetchUi.limeButton("Enable location", LocationPermissionDialog::ask),
                FetchUi.link("Return to route options", () -> Navigator.go("tripRoutes")));
    }
}
