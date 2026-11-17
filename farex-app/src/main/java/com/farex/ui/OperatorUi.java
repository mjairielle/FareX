package com.farex.ui;

import javafx.scene.effect.DropShadow;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.Border;
import javafx.scene.layout.BorderStroke;
import javafx.scene.layout.BorderStrokeStyle;
import javafx.scene.layout.BorderWidths;
import javafx.scene.layout.CornerRadii;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Arc;
import javafx.scene.shape.ArcType;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Line;
import javafx.scene.shape.Polygon;
import javafx.scene.shape.StrokeLineCap;
import javafx.scene.text.Font;

// Shared pieces for the Operator Mode screens: cards, chips, money, the big shift buttons
final class OperatorUi {

    static final Color CARD = Color.web("#1E1E1E");
    static final Color AMBER = Color.web("#F5A623");

    private OperatorUi() {
    }

    // ----- Cards -----

    static VBox card(Node... children) {
        VBox card = new VBox(8, children);
        card.setPadding(new Insets(14));
        card.setBackground(Theme.rounded(CARD, 10));
        return card;
    }

    // "SHIFT REVENUE ............ [cash icon]"
    static HBox cardTop(String label, Color labelColor, Node right) {
        Label text = new Label(label);
        text.setTextFill(labelColor);
        text.setFont(Theme.bold(10));
        Region fill = new Region();
        HBox.setHgrow(fill, Priority.ALWAYS);
        HBox top = new HBox(6, text, fill);
        if (right != null) {
            top.getChildren().add(right);
        }
        top.setAlignment(Pos.CENTER_LEFT);
        return top;
    }

    // "0 trips": big number, small word after it
    static HBox count(int number, String unit) {
        Label big = TripUi.bold(String.valueOf(number), 30);
        big.setTextFill(Theme.LIME);
        Label small = FetchUi.small(unit, Theme.WHITE);
        small.setFont(Font.font("System", 12));
        HBox line = new HBox(6, big, small);
        line.setAlignment(Pos.BASELINE_LEFT);
        return line;
    }

    // Thin gray divider line
    static Region divider() {
        Region line = new Region();
        line.setMinHeight(1);
        line.setPrefHeight(1);
        line.setMaxHeight(1);
        line.setBackground(Theme.solid(Color.web("#333333")));
        return line;
    }

    // ----- Chips -----

    // "● Shift inactive", "● Pending"
    static Label chip(String text, Color textColor, Color fill) {
        Label chip = new Label(text);
        chip.setGraphic(new Circle(3, textColor));
        chip.setGraphicTextGap(6);
        chip.setTextFill(textColor);
        chip.setFont(Theme.bold(9));
        chip.setPadding(new Insets(3, 10, 3, 9));
        chip.setBackground(Theme.rounded(fill, 50));
        return chip;
    }

    // "ABC-123" on dark olive
    static Label plateChip(String plate) {
        Label chip = new Label(plate);
        chip.setTextFill(Theme.LIME);
        chip.setFont(Theme.bold(11));
        chip.setPadding(new Insets(3, 10, 3, 10));
        chip.setBackground(Theme.rounded(Color.web("#2C3308"), 4));
        return chip;
    }

    // ----- Money -----

    // 124000 -> "₱1,240.00"
    static String peso(long cents) {
        return FetchUi.peso(cents / 100.0);
    }

    static Label money(long cents, double size) {
        Label label = TripUi.bold(peso(cents), size);
        label.setTextFill(Theme.LIME);
        label.setWrapText(false);
        return label;
    }

    // ----- Buttons -----

    // Black button with a thin gray border, e.g. "‹ Prev", "Fix GPS"
    static Button outlineButton(String text, Runnable onClick) {
        Button button = TripUi.darkButton(text, onClick);
        button.setFont(Theme.bold(12));
        button.setBorder(new Border(new BorderStroke(Color.web("#3A3A3A"), BorderStrokeStyle.SOLID,
                new CornerRadii(8), new BorderWidths(1))));
        return button;
    }

    // ----- Big floating shift buttons -----

    // Lime "▶ START SHIFT"
    static Button startShiftButton(Runnable onClick) {
        Polygon play = new Polygon(0, 0, 12, 7, 0, 14);
        play.setFill(Theme.BLACK);
        Button button = bigButton("START SHIFT", play, onClick);
        Theme.paintButton(button, "#CAFE06", "#D8FF3A", "#A9D400", 14, "black");
        button.setEffect(new DropShadow(26, Color.rgb(202, 254, 6, 0.5)));
        return button;
    }

    // Black "⏻ END SHIFT" with a lime border
    static Button endShiftButton(Runnable onClick) {
        Button button = bigButton("END SHIFT", powerIcon(), onClick);
        Theme.paintButton(button, "#141414", "#1C1C1C", "#0A0A0A", 14, "white");
        button.setBorder(new Border(new BorderStroke(Theme.LIME, BorderStrokeStyle.SOLID,
                new CornerRadii(14), new BorderWidths(2))));
        button.setEffect(new DropShadow(22, Color.rgb(202, 254, 6, 0.35)));
        return button;
    }

    private static Button bigButton(String text, Node icon, Runnable onClick) {
        Button button = new Button(text, icon);
        button.setGraphicTextGap(14);
        button.setFont(Theme.bold(20));
        button.setMaxWidth(Double.MAX_VALUE);
        button.setMinHeight(62);
        button.setOnAction(e -> onClick.run());
        return button;
    }

    // Power symbol: open circle with a line through the top
    private static Group powerIcon() {
        Arc ring = new Arc(9, 10, 8, 8, 120, 300);
        ring.setType(ArcType.OPEN);
        ring.setFill(null);
        Line stem = new Line(9, 1, 9, 9);
        for (javafx.scene.shape.Shape shape : new javafx.scene.shape.Shape[] {ring, stem}) {
            shape.setStroke(Theme.WHITE);
            shape.setStrokeWidth(2.2);
            shape.setStrokeLineCap(StrokeLineCap.ROUND);
        }
        return new Group(ring, stem);
    }
}
