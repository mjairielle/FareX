package com.farex.ui;

import javafx.geometry.Insets;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;

// Trip Planner / 08, 09, 10: one screen for whichever route was picked (TripState.getRoute())
public class TripRouteView extends SubScreen {

    public TripRouteView() {
        super("Trip Planner", "tripRoutes");

        TripRoute route = TripState.getRoute();

        VBox heading = new VBox(6,
                FetchUi.title(route.getTitle()),
                FetchUi.subtitle(TripState.summary()));
        VBox.setMargin(heading, new Insets(0, 0, 6, 0));

        // "4.2 km  ·  25 min  ·  ₱26"
        Label stats = new Label(route.getStats().replace(" · ", "  ·  "));
        stats.setTextFill(Theme.LIME);
        stats.setFont(Theme.bold(16));
        VBox total = FetchUi.panel(stats, FetchUi.small("Estimated total · standard fare", Theme.WHITE));
        total.setSpacing(4);

        // "01  Board at Quiapo Church" / "Quiapo–Kalaw jeepney · estimated ₱13"
        VBox legs = FetchUi.panel();
        legs.setSpacing(8);
        for (int i = 0; i < route.getLegs().size(); i++) {
            TripRoute.Leg leg = route.getLegs().get(i);
            VBox step = new VBox(2,
                    TripUi.bold(TripRoute.number(i) + "  " + leg.getTitle(), 12),
                    FetchUi.small(leg.getDetail(), Theme.WHITE));
            legs.getChildren().add(step);
        }

        Label disclaimer = FetchUi.subtitle("Estimates only. Confirm the jeepney signboard, fare, "
                + "and alighting stop with the driver.");

        body.setSpacing(12);
        body.getChildren().addAll(
                heading,
                total,
                legs,
                disclaimer,
                FetchUi.spacer(),
                FetchUi.limeButton("Start trip tracking", () -> Navigator.go("tripLocation")),
                FetchUi.link("View route map", () -> Navigator.go("tripRouteMap")));
    }
}
