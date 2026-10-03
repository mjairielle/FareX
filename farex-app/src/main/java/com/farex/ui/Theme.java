package com.farex.ui;

import javafx.geometry.Insets;
import javafx.scene.Cursor;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.effect.DropShadow;
import javafx.scene.layout.Background;
import javafx.scene.layout.BackgroundFill;
import javafx.scene.layout.CornerRadii;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;

/**
 * FareX look in plain Java (no stylesheet): colors from the Figma
 * plus small helpers so every screen styles things the same way.
 */
public final class Theme {

    public static final Color LIME = Color.web("#CAFE06");         // taken from the logo file
    public static final Color LIME_HOVER = Color.web("#D8FF3A");
    public static final Color LIME_PRESSED = Color.web("#A9D400");
    public static final Color DARK = Color.web("#141414");
    public static final Color BLACK = Color.BLACK;
    public static final Color WHITE = Color.WHITE;

    private Theme() {
    }

    public static Background solid(Color color) {
        return new Background(new BackgroundFill(color, CornerRadii.EMPTY, Insets.EMPTY));
    }

    public static Background rounded(Color color, double radius) {
        return new Background(new BackgroundFill(color, new CornerRadii(radius), Insets.EMPTY));
    }

    public static Font bold(double size) {
        return Font.font("System", FontWeight.BOLD, size);
    }

    /** Big title text, e.g. Theme.headline("Your Way", Theme.LIME) */
    public static Label headline(String text, Color color) {
        Label label = new Label(text);
        label.setTextFill(color);
        label.setFont(bold(34));
        return label;
    }

    /** Normal small text. */
    public static Label bodyText(String text, Color color) {
        Label label = new Label(text);
        label.setTextFill(color);
        label.setFont(Font.font("System", 14));
        return label;
    }

    /** Lime rounded button with a glow (Get Started, Login, Sign up, ...). */
    public static void limeButton(Button button) {
        button.setBackground(rounded(LIME, 12));
        button.setTextFill(BLACK);
        button.setFont(bold(16));
        button.setPadding(new Insets(14, 20, 14, 20));
        button.setCursor(Cursor.HAND);

        DropShadow glow = new DropShadow(18, Color.rgb(202, 254, 6, 0.55));
        glow.setSpread(0.1);
        button.setEffect(glow);

        button.setOnMouseEntered(e -> button.setBackground(rounded(LIME_HOVER, 12)));
        button.setOnMouseExited(e -> button.setBackground(rounded(LIME, 12)));
        button.setOnMousePressed(e -> button.setBackground(rounded(LIME_PRESSED, 12)));
        button.setOnMouseReleased(e -> button.setBackground(rounded(LIME_HOVER, 12)));
    }
}
