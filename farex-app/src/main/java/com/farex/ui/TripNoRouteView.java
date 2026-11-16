package com.farex.ui;

import javafx.geometry.Insets;
import javafx.scene.Cursor;
import javafx.scene.Node;
import javafx.scene.layout.VBox;

// Trip Planner / 11: nothing matched. Tapping a suggested stop makes it the destination
// and searches again.
public class TripNoRouteView extends SubScreen {

    public TripNoRouteView() {
        super("Trip Planner", "tripPlanner");

        VBox header = FetchUi.resultHeader(WarningDialog.warningIcon(),
                "No route found",
                "There is no matching route to this destination in the available service area.");
        VBox.setMargin(header, new Insets(10, 0, 8, 0));

        VBox different = FetchUi.panel(
                TripUi.bold("Try a different destination", 14),
                FetchUi.subtitle("Move your destination closer to an available stop, or check your starting point."));
        different.setSpacing(6);

        VBox.setMargin(body, Insets.EMPTY);
        body.setPadding(new Insets(16, 20, 24, 20));
        body.setSpacing(12);
        body.getChildren().addAll(
                TripUi.eyebrow("LET’S TRY A NEARBY STOP"),
                header,
                different,
                TripUi.eyebrow("SUGGESTED NEARBY STOPS"),
                suggestion(TripState.KALAW),
                suggestion(TripState.CITY_HALL),
                FetchUi.spacer(),
                FetchUi.limeButton("Edit locations", () -> Navigator.go("tripPlanner")),
                FetchUi.link("Back to Routes", () -> Navigator.go("routes")));
    }

    // "Kalaw Avenue ›" / "Near Rizal Park · walk to your destination"
    private Node suggestion(TripPlace place) {
        VBox panel = FetchUi.panel(
                TripUi.bold(place.getName() + " ›", 14),
                FetchUi.small(place.getAddress(), Theme.WHITE));
        panel.setSpacing(4);
        panel.setCursor(Cursor.HAND);
        panel.setOnMouseClicked(e -> {
            TripState.setDestination(place);
            Navigator.go("tripSearching");
        });
        return panel;
    }
}
