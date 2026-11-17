package com.farex.ui.trip;

import javafx.geometry.Insets;
import javafx.scene.layout.VBox;

import com.farex.ui.common.CommonUi;
import com.farex.ui.common.MapView;
import com.farex.ui.common.Navigator;
import com.farex.ui.common.SubScreen;

// Trip Planner / 20 Explore map: a map card and "Plan a trip"
public class TripMapView extends SubScreen {

    public TripMapView() {
        super("Trip Planner", "routes");

        VBox heading = new VBox(8,
                CommonUi.title("Find your next stop"),
                CommonUi.subtitle("Use the trip planner to compare jeepney connections, fares, and stops."));
        VBox.setMargin(heading, new Insets(0, 0, 6, 0));

        body.setSpacing(12);
        body.getChildren().addAll(
                heading,
                new MapView(320, 250, 12),
                CommonUi.spacer(),
                CommonUi.limeButton("Plan a trip", () -> Navigator.go("tripNew")));
    }
}
