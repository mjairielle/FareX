package com.farex.ui;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.layout.Border;
import javafx.scene.layout.BorderStroke;
import javafx.scene.layout.BorderStrokeStyle;
import javafx.scene.layout.BorderWidths;
import javafx.scene.layout.CornerRadii;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

// Trip Planner / 07: the three routes; tapping one opens its details
public class TripRoutesView extends SubScreen {

    public TripRoutesView() {
        super("Trip Planner", "tripPlanner");

        VBox heading = new VBox(6,
                FetchUi.title("Choose your route"),
                FetchUi.subtitle(TripState.summary()
                        + "\nExample routes · fares and times are estimates"));
        VBox.setMargin(heading, new Insets(0, 0, 6, 0));

        body.setSpacing(12);
        body.getChildren().add(heading);
        for (int i = 0; i < TripRoute.SAMPLES.size(); i++) {
            body.getChildren().add(routeCard(i));
        }
        body.getChildren().addAll(
                FetchUi.spacer(),
                FetchUi.limeButton("Edit locations", () -> Navigator.go("tripPlanner")),
                FetchUi.link("Back to Routes", () -> Navigator.go("routes")));
    }

    // "01  Shortest distance ›" / "4.2 km · 25 min · ₱26" / "1 transfer · Walk 300 m"
    private Node routeCard(int index) {
        TripRoute route = TripRoute.SAMPLES.get(index);
        boolean selected = (index == TripState.getRouteIndex());

        Label name = new Label(TripRoute.number(index) + "  " + route.getTitle() + " ›");
        name.setTextFill(selected ? Theme.LIME : Theme.WHITE);
        name.setFont(Theme.bold(12));

        VBox text = new VBox(3, name, TripUi.bold(route.getStats(), 14),
                FetchUi.small(route.getSummary(), Theme.WHITE));
        HBox.setHgrow(text, Priority.ALWAYS);

        Label chevron = new Label("›");
        chevron.setTextFill(Theme.WHITE);
        chevron.setFont(Theme.bold(22));

        HBox card = new HBox(8, text, chevron);
        card.setAlignment(Pos.CENTER_LEFT);
        card.setPadding(new Insets(12, 16, 12, 16));
        card.setBackground(Theme.rounded(FetchUi.PANEL, 8));
        if (selected) {
            card.setBorder(new Border(new BorderStroke(Theme.LIME,
                    BorderStrokeStyle.SOLID, new CornerRadii(8), new BorderWidths(1.2))));
        }
        card.setCursor(Cursor.HAND);
        card.setOnMouseClicked(e -> {
            TripState.setRouteIndex(index);
            Navigator.go("tripRoute");
        });
        return card;
    }
}
