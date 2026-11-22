package com.farex.ui;

import javafx.geometry.Insets;
import javafx.scene.Cursor;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;

// Trip Planner / 01 (nothing chosen) and 05 (both chosen): Point A, Point B, Search routes
public class TripPlannerView extends SubScreen {

    public TripPlannerView() {
        super("Trip Planner", "routes");

        TripPlace start = TripState.getStart();
        TripPlace destination = TripState.getDestination();

        Label title = new Label("Trip planner");
        title.setTextFill(Theme.WHITE);
        title.setFont(Theme.bold(22));

        VBox heading = new VBox(6,
                TripUi.eyebrow("PLAN YOUR JOURNEY"),
                title,
                FetchUi.subtitle("Choose where to start and where you want to go."));
        VBox.setMargin(heading, new Insets(-12, 0, 4, 0));

        Node pointA = pointPanel("POINT A · FROM",
                (start == null) ? "Choose starting point" : start.getFullName(),
                () -> Navigator.go("tripStart"));
        // Point B needs Point A first ("Starting at Quiapo Church" is shown there)
        Node pointB = pointPanel("POINT B · TO",
                (destination == null) ? "Choose destination" : destination.getFullName(),
                () -> Navigator.go(start == null ? "tripStart" : "tripDestination"));

        VBox info = FetchUi.panel(
                TripUi.bold("Find your way, your way", 13),
                FetchUi.subtitle("Compare distance, transfers, and estimated fare. "
                        + "You can track your trip after choosing a route."));
        info.setSpacing(6);

        body.setSpacing(12);
        body.getChildren().addAll(
                heading,
                pointA,
                pointB,
                info,
                FetchUi.small("Preview journey · sample stops and estimates", Theme.WHITE),
                FetchUi.spacer(),
                FetchUi.limeButton("Search routes", this::search),
                FetchUi.link("Cancel", () -> {
                    TripState.clear();
                    Navigator.go("routes");
                }));
    }

    private Node pointPanel(String caption, String value, Runnable onClick) {
        VBox panel = FetchUi.panel(TripUi.eyebrow(caption), TripUi.bold(value, 14));
        panel.setSpacing(4);
        panel.setCursor(Cursor.HAND);
        panel.setOnMouseClicked(e -> onClick.run());
        return panel;
    }

    // Both chosen -> search; otherwise the Warning pop-up
    private void search() {
        if (TripState.isReady()) {
            Navigator.go("tripSearching");
        } else {
            Navigator.showOverlay(new TripWarningDialog());
        }
    }
}
