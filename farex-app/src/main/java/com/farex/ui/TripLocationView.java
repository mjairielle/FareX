package com.farex.ui;

import javafx.geometry.Insets;
import javafx.scene.layout.VBox;

// Trip Planner / 13: ask for location before tracking. "Not now" goes back to the chosen route.
public class TripLocationView extends SubScreen {

    public TripLocationView() {
        super("Trip Planner", "tripRoute");

        VBox heading = new VBox(6,
                FetchUi.title("Enable location"),
                FetchUi.subtitle("Follow your progress and see when to get off."));

        body.setSpacing(12);
        body.getChildren().addAll(
                heading,
                TripUi.centerMessage(TripUi.steeringWheel(),
                        "Use location while traveling",
                        "Your device location helps show trip progress and possible route deviations. "
                                + "You can still view route details without tracking.",
                        "Tracking stops when you end your trip."),
                FetchUi.spacer(),
                TripUi.routeCard(),
                FetchUi.limeButton("Allow location", LocationPermissionDialog::ask),
                FetchUi.link("Not now", () -> Navigator.go("tripRoute")));
        VBox.setMargin(heading, new Insets(0, 0, 0, 0));
    }
}
