package com.farex.ui;

import javafx.geometry.Insets;
import javafx.scene.Cursor;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.effect.DropShadow;
import javafx.scene.layout.Background;
import javafx.scene.layout.BackgroundFill;
import javafx.scene.layout.Border;
import javafx.scene.layout.BorderStroke;
import javafx.scene.layout.BorderStrokeStyle;
import javafx.scene.layout.BorderWidths;
import javafx.scene.layout.CornerRadii;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;

public final class Theme {

    public static final Color LIME = Color.web("#CAFE06");   
    public static final Color LIME_HOVER = Color.web("#D8FF3A");
    public static final Color LIME_PRESSED = Color.web("#A9D400");
    public static final Color DARK = Color.web("#141414");
    public static final Color BLACK = Color.BLACK;
    public static final Color WHITE = Color.WHITE;
    public static final Color GRAY = Color.web("#9A9A9A");
    public static final Color ERROR = Color.web("#FF5C5C");
    public static final Color FIELD = Color.web("#1F1F1F");    

    private Theme() {
    }

    public static Background solid(Color color) {
        return new Background(new BackgroundFill(color, CornerRadii.EMPTY, Insets.EMPTY));
    }

    public static Background rounded(Color color, double radius) {
        return new Background(new BackgroundFill(color, new CornerRadii(radius), Insets.EMPTY));
    }

    public static Background roundedTop(Color color, double radius) {
        CornerRadii radii = new CornerRadii(radius, radius, 0, 0, false);
        return new Background(new BackgroundFill(color, radii, Insets.EMPTY));
    }

    public static Font bold(double size) {
        return Font.font("System", FontWeight.BOLD, size);
    }

    public static Label headline(String text, Color color) {
        Label label = new Label(text);
        label.setTextFill(color);
        label.setFont(bold(34));
        return label;
    }

    public static Label bodyText(String text, Color color) {
        Label label = new Label(text);
        label.setTextFill(color);
        label.setFont(Font.font("System", 14));
        return label;
    }

    public static Label linkLabel(String text) {
        Label label = new Label(text);
        label.setTextFill(WHITE);
        label.setFont(bold(14));
        label.setCursor(Cursor.HAND);
        return label;
    }

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

    public static void inputField(TextField field, String prompt) {
        field.setPromptText(prompt);
        field.setBackground(rounded(FIELD, 10));
        field.setPadding(new Insets(14));
        field.setFont(Font.font("System", 14));
        field.setMaxWidth(Double.MAX_VALUE);
        field.setStyle("-fx-text-fill: white; -fx-prompt-text-fill: #9A9A9A;");

        Border idleBorder = new Border(new BorderStroke(
                Color.web("#333333"), BorderStrokeStyle.SOLID, new CornerRadii(10), new BorderWidths(1)));
        Border focusBorder = new Border(new BorderStroke(
                LIME, BorderStrokeStyle.SOLID, new CornerRadii(10), new BorderWidths(2)));

        field.setBorder(idleBorder);
        field.focusedProperty().addListener((obs, wasFocused, isFocused) -> field.setBorder(isFocused ? focusBorder : idleBorder));
    }
}