package com.farex.ui.trip;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.effect.DropShadow;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Line;
import javafx.scene.shape.Polyline;
import javafx.scene.shape.StrokeLineCap;
import javafx.scene.shape.StrokeLineJoin;
import javafx.scene.text.TextAlignment;

import com.farex.ui.common.CommonUi;
import com.farex.ui.common.LeafletMap;
import com.farex.ui.common.Theme;

// Small pieces only the Trip Planner screens use (the rest comes from CommonUi)
public final class TripUi {

    public static final Color CARD = Color.web("#1C1C1C");

    private TripUi() {
    }

    // Small lime caps text above a title, e.g. "PLAN YOUR JOURNEY", "POINT A · FROM"
    public static Label eyebrow(String text) {
        Label label = new Label(text);
        label.setTextFill(Theme.LIME);
        label.setFont(Theme.bold(9));
        return label;
    }

    public static Label bold(String text, double size) {
        Label label = new Label(text);
        label.setTextFill(Theme.WHITE);
        label.setFont(Theme.bold(size));
        label.setWrapText(true);
        return label;
    }

    // Dark card that floats over the map
    public static VBox floatingCard(Node... children) {
        VBox card = new VBox(4, children);
        card.setPadding(new Insets(14, 16, 14, 16));
        card.setBackground(Theme.rounded(CARD, 10));
        card.setEffect(new DropShadow(12, Color.rgb(0, 0, 0, 0.45)));
        return card;
    }

    // Black button with white text, e.g. "Cancel" under the lime one on the map screens
    public static Button darkButton(String text, Runnable onClick) {
        Button button = new Button(text);
        button.setFont(Theme.bold(13));
        button.setPadding(new Insets(10));
        button.setMaxWidth(Double.MAX_VALUE);
        Theme.paintButton(button, "#141414", "#222222", "#0A0A0A", 8, "white");
        button.setOnAction(e -> onClick.run());
        return button;
    }

    // ----- Tracking screens -----

    // "Route 1 · Shortest distance" / "4.2 km · 25 min · ₱26" / "Quiapo–Kalaw jeepney"
    public static VBox routeCard() {
        TripRoute route = TripState.getRoute();
        Label name = CommonUi.smallBold("Route " + (TripState.getRouteIndex() + 1)
                + " · " + route.getTitle(), Theme.LIME);
        name.setFont(Theme.bold(12));
        VBox card = CommonUi.panel(name, bold(route.getStats(), 15),
                CommonUi.small(route.getJeepney(), Theme.WHITE));
        card.setSpacing(6);
        card.setPadding(new Insets(14, 14, 18, 14));
        return card;
    }

    // Centered icon, bold line, text, and an optional lime line, e.g. "You may be off route"
    public static VBox centerMessage(Node icon, String title, String text, String limeLine) {
        Label sentence = CommonUi.small(text, Theme.WHITE);
        sentence.setWrapText(true);
        sentence.setTextAlignment(TextAlignment.CENTER);
        sentence.setMaxWidth(270);

        VBox box = new VBox(12, icon, bold(title, 15), sentence);
        if (limeLine != null) {
            Label lime = CommonUi.smallBold(limeLine, Theme.LIME);
            VBox.setMargin(lime, new Insets(6, 0, 0, 0));
            box.getChildren().add(lime);
        }
        box.setAlignment(Pos.TOP_CENTER);
        VBox.setMargin(box, new Insets(36, 0, 0, 0));
        return box;
    }

    // Map card with a black tag at the bottom. A real plan is drawn on it (lime line, start and
    // end labels); the offline sample routes have no map data, so that map is area context only.
    public static StackPane mapCard(double height) {
        java.util.List<com.farex.Api.PlanRide> rides = TripState.getRoute().getRides();
        Label tag = new Label(rides.isEmpty() ? "Manila · map context, not live GPS"
                                              : "Your route in lime · not live GPS");
        tag.setTextFill(Theme.WHITE);
        tag.setFont(Theme.bold(9));
        tag.setPadding(new Insets(5, 10, 5, 10));
        tag.setMaxWidth(Double.MAX_VALUE);
        tag.setBackground(Theme.rounded(Color.web("#0E0E0E"), 6));
        StackPane.setAlignment(tag, Pos.BOTTOM_CENTER);
        StackPane.setMargin(tag, new Insets(0, 12, 12, 12));

        LeafletMap map = new LeafletMap(320, height, 12);
        if (!rides.isEmpty()) map.withFarexTrip(rides);
        StackPane card = new StackPane(map, tag);
        card.setMaxSize(320, height);
        return card;
    }

    // Lime steering wheel (Enable location)
    public static Group steeringWheel() {
        Circle rim = new Circle(36, 36, 30);
        rim.setFill(null);
        Polyline bar = new Polyline(9, 33, 24, 30, 36, 38, 48, 30, 63, 33);
        Line stem = new Line(36, 40, 36, 64);
        for (javafx.scene.shape.Shape shape : new javafx.scene.shape.Shape[] {rim, bar, stem}) {
            shape.setStroke(Theme.LIME);
            shape.setStrokeWidth(7);
            shape.setStrokeLineCap(StrokeLineCap.ROUND);
            shape.setStrokeLineJoin(StrokeLineJoin.ROUND);
        }
        bar.setFill(null);
        Circle hub = new Circle(36, 39, 6, Theme.LIME);
        return new Group(rim, bar, stem, hub);
    }
}
