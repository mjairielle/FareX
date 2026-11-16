package com.farex.ui;

import javafx.scene.Cursor;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;

// Trip Planner / 15: the (simulated) trip in progress
public class TripTrackingView extends SubScreen {

    public TripTrackingView() {
        super("Trip Planner", "tripEndConfirm");   // leaving asks "End this trip?" first

        TripRoute route = TripState.getRoute();

        VBox heading = new VBox(6,
                FetchUi.title("Your trip is underway"),
                FetchUi.subtitle(TripState.summary() + "\nLocation and progress are simulated."));

        // Next step: the first leg, e.g. "Board at Quiapo Church"
        VBox nextStep = FetchUi.panel(
                TripUi.bold(route.getLegs().get(0).getTitle(), 14),
                FetchUi.subtitle("Check the signboard and confirm your destination with the driver before boarding."));
        nextStep.setSpacing(6);

        Label offRoute = FetchUi.small("Preview an off-route warning ›", Theme.LIME);
        offRoute.setCursor(Cursor.HAND);
        offRoute.setOnMouseClicked(e -> Navigator.go("tripOffRoute"));

        body.setSpacing(12);
        body.getChildren().addAll(
                heading,
                TripUi.mapCard(120),
                TripUi.routeCard(),
                nextStep,
                offRoute,
                FetchUi.spacer(),
                FetchUi.limeButton("I’ve arrived", () -> Navigator.go("tripArrived")),
                FetchUi.link("End trip", () -> Navigator.go("tripEndConfirm")));
    }
}
