package com.farex.ui;

import javafx.geometry.Insets;
import javafx.scene.layout.VBox;

// Trip Planner / 20 Explore map: a map card and "Plan a trip"
public class TripMapView extends SubScreen {

    public TripMapView() {
        super("Trip Planner", "routes");

        VBox heading = new VBox(8,
                FetchUi.title("Find your next stop"),
                FetchUi.subtitle("Use the trip planner to compare jeepney connections, fares, and stops."));
        VBox.setMargin(heading, new Insets(0, 0, 6, 0));

        body.setSpacing(12);
        body.getChildren().addAll(
                heading,
                new MapView(320, 250, 12),
                FetchUi.spacer(),
                FetchUi.limeButton("Plan a trip", () -> Navigator.go("tripNew")));
    }
}
