package com.farex.ui;

import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;

// Small pieces only the Trip Planner screens use (the rest comes from FetchUi)
final class TripUi {

    static final Color CARD = Color.web("#1C1C1C");

    private TripUi() {
    }

    // Small lime caps text above a title, e.g. "PLAN YOUR JOURNEY", "POINT A · FROM"
    static Label eyebrow(String text) {
        Label label = new Label(text);
        label.setTextFill(Theme.LIME);
        label.setFont(Theme.bold(9));
        return label;
    }

    static Label bold(String text, double size) {
        Label label = new Label(text);
        label.setTextFill(Theme.WHITE);
        label.setFont(Theme.bold(size));
        label.setWrapText(true);
        return label;
    }

    // Dark card that floats over the map
    static VBox floatingCard(Node... children) {
        VBox card = new VBox(4, children);
        card.setPadding(new Insets(14, 16, 14, 16));
        card.setBackground(Theme.rounded(CARD, 10));
        card.setEffect(new javafx.scene.effect.DropShadow(12, Color.rgb(0, 0, 0, 0.45)));
        return card;
    }

    // Black button with white text, e.g. "Cancel" under the lime one on the map screens
    static Button darkButton(String text, Runnable onClick) {
        Button button = new Button(text);
        button.setFont(Theme.bold(13));
        button.setPadding(new Insets(10));
        button.setMaxWidth(Double.MAX_VALUE);
        Theme.paintButton(button, "#141414", "#222222", "#0A0A0A", 8, "white");
        button.setOnAction(e -> onClick.run());
        return button;
    }
}
