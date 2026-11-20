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
import com.farex.ui.common.RouteMap;
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

    // "My location": a ring with a dot and four ticks, like a map app's locate button
    public static Node locateIcon() {
        Circle ring = new Circle(7, Color.TRANSPARENT);
        ring.setStroke(Color.WHITE);
        ring.setStrokeWidth(2);
        Group icon = new Group(ring, new Circle(3, Color.WHITE));
        for (double[] t : new double[][] {{0, -7, 0, -10}, {0, 7, 0, 10}, {-7, 0, -10, 0}, {7, 0, 10, 0}}) {
            Line tick = new Line(t[0], t[1], t[2], t[3]);
            tick.setStroke(Color.WHITE);
            tick.setStrokeWidth(2);
            icon.getChildren().add(tick);
        }
        return icon;
    }

    // ----- Tracking screens -----

    // "Route 1 · Shortest distance" / "4.2 km · 25 min · ₱26" / "Quiapo–Kalaw jeepney"
    public static VBox routeCard() {
        TripRoute route = TripState.getRoute();
        Label name = CommonUi.smallBold("Route " + (TripState.getRouteIndex() + 1)
                + " · " + route.getTitle(), Theme.LIME);
        name.setFont(Theme.bold(12));
        VBox card = CommonUi.panel(name, bold(route.getStats(), 15), wrapped(route.getJeepney()));
        card.setSpacing(6);
        card.setPadding(new Insets(14, 14, 18, 14));
        return card;
    }

    // Small white text that wraps: real route names are long ("Commonwealth–SM Fairview via Regalado ...")
    public static Label wrapped(String text) {
        Label label = CommonUi.small(text, Theme.WHITE);
        label.setWrapText(true);
        return label;
    }

    // The pinned ends of the trip (not stops), by name, so walks to and from them can be placed
    public static java.util.Map<String, farex.shared.data.RouteStop> pinnedPoints() {
        java.util.Map<String, farex.shared.data.RouteStop> pins = new java.util.HashMap<>();
        for (TripPlace p : new TripPlace[]{TripState.getStart(), TripState.getDestination()}) {
            if (p != null && p.isPin()) pins.put(p.getName(), new farex.shared.data.RouteStop(p.getName(), p.getLat(), p.getLng()));
        }
        return pins;
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
