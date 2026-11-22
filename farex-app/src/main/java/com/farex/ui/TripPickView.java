package com.farex.ui;

import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

// Trip Planner / 03 (starting point) and 04 (destination): cards floating over the map.
// One class, two modes: new TripPickView(true) picks the start, (false) the destination.
public class TripPickView extends SubScreen {

    public TripPickView(boolean pickingStart) {
        super("Trip Planner", pickingStart ? "tripPlanner" : "tripStart");

        TripPlace place = pickingStart ? TripState.QUIAPO : TripState.RIZAL_PARK;

        // ----- Top card -----
        Label title = TripUi.bold(pickingStart ? "Starting point" : "Destination", 22);
        TripPlace start = TripState.getStart();
        String hint = pickingStart
                ? "Select a suggested stop to plan this preview trip."
                : "Starting at " + (start == null ? "your starting point" : start.getFullName()) + ".";
        VBox topCard = TripUi.floatingCard(
                TripUi.eyebrow(pickingStart ? "STEP 1 OF 2" : "STEP 2 OF 2"),
                title,
                FetchUi.small(hint, Theme.WHITE));

        // ----- Bottom cards and buttons -----
        VBox placeCard = TripUi.floatingCard(
                TripUi.eyebrow("SEARCH LOCATION"),
                TripUi.bold(place.getName(), 15),
                FetchUi.small(place.getAddress(), Theme.WHITE));

        VBox tipCard = pickingStart
                ? tip("Prefer your current location?",
                        "You can enable location when you start tracking. Planning a trip does not require GPS.")
                : tip("Stop-based route planning",
                        "Routes are matched to boarding and alighting stops. A short walk may be needed.");

        Node choose = FetchUi.limeButton(
                (pickingStart ? "Use " : "Choose ") + place.getName(),
                () -> {
                    if (pickingStart) {
                        TripState.setStart(place);
                        Navigator.go("tripDestination");
                    } else {
                        TripState.setDestination(place);
                        Navigator.go("tripPlanner");   // 05: both filled in
                    }
                });

        Node second = pickingStart
                ? TripUi.darkButton("Cancel", () -> Navigator.go("tripPlanner"))
                : TripUi.darkButton("Back to starting point", () -> Navigator.go("tripStart"));

        Region push = new Region();
        VBox.setVgrow(push, Priority.ALWAYS);

        VBox overlay = new VBox(10, topCard, push, placeCard, tipCard, choose, second);
        overlay.setPadding(new Insets(10, 12, 14, 12));
        overlay.setPickOnBounds(false);

        // The map fills the whole area under the header
        StackPane area = new StackPane(new MapView(), overlay);
        VBox.setVgrow(area, Priority.ALWAYS);

        body.setPadding(Insets.EMPTY);
        body.getChildren().add(area);
    }

    // Small dark card: bold line + short text
    private VBox tip(String heading, String text) {
        Label small = FetchUi.small(text, Theme.WHITE);
        small.setWrapText(true);
        VBox card = TripUi.floatingCard(TripUi.bold(heading, 11), small);
        card.setPadding(new Insets(8, 12, 8, 12));
        return card;
    }
}
