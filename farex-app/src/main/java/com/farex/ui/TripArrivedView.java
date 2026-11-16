package com.farex.ui;

import javafx.geometry.Insets;
import javafx.scene.layout.VBox;

// Trip Planner / 18: trip complete
public class TripArrivedView extends SubScreen {

    public TripArrivedView() {
        super("Trip Planner", "routes");

        VBox heading = new VBox(6,
                FetchUi.title("You’ve arrived!"),
                FetchUi.subtitle("Location tracking is now off."));

        VBox thanks = TripUi.centerMessage(new CheckRing(84, Theme.DARK),
                "Thanks for riding with FareX", TripState.summary(), null);
        VBox.setMargin(thanks, new Insets(44, 0, 0, 0));

        body.setSpacing(12);
        body.getChildren().addAll(
                heading,
                thanks,
                FetchUi.spacer(),
                FetchUi.subtitle("Route estimates are not an actual payment or recorded travel time. "
                        + "This journey is a preview."),
                TripUi.routeCard(),
                FetchUi.limeButton("Plan another trip", () -> Navigator.go("tripNew")),
                FetchUi.link("Back to Routes", () -> {
                    TripState.clear();
                    Navigator.go("routes");
                }));
    }
}
