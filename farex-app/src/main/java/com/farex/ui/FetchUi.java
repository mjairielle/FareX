package com.farex.ui;

import java.util.Locale;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;

// Shared pieces for the Fetch Load screens: titles, gray panels, label/value rows, buttons
final class FetchUi {

    static final Color PANEL = Color.web("#1E1E1E");
    static final Color SOFT_TEXT = Color.web("#E6E6E6");

    private FetchUi() {
    }

    // ----- Text -----

    static Label title(String text) {
        Label label = new Label(text);
        label.setTextFill(Theme.WHITE);
        label.setFont(Theme.bold(18));
        label.setWrapText(true);
        return label;
    }

    static Label subtitle(String text) {
        Label label = small(text, SOFT_TEXT);
        label.setWrapText(true);
        return label;
    }

    static Label small(String text, Color color) {
        Label label = new Label(text);
        label.setTextFill(color);
        label.setFont(Font.font("System", 11));
        return label;
    }

    static Label smallBold(String text, Color color) {
        Label label = new Label(text);
        label.setTextFill(color);
        label.setFont(Theme.bold(11));
        return label;
    }

    // ----- Panels -----

    static VBox panel(Node... children) {
        VBox panel = new VBox(10, children);
        panel.setPadding(new Insets(14));
        panel.setBackground(Theme.rounded(PANEL, 8));
        return panel;
    }

    // Gray box with one wrapped sentence, e.g. "Keep your physical card nearby..."
    static VBox note(String text) {
        Label label = small(text, Theme.WHITE);
        label.setWrapText(true);
        return panel(label);
    }

    // "Reference ............ FX-LD-1024"
    static HBox row(String label, String value) {
        return row(label, smallBold(value, Theme.WHITE));
    }

    static HBox row(String label, Node value) {
        Region fill = new Region();
        HBox.setHgrow(fill, Priority.ALWAYS);
        HBox row = new HBox(8, small(label, SOFT_TEXT), fill, value);
        row.setAlignment(Pos.CENTER_LEFT);
        return row;
    }

    // ----- Bottom of the screen -----

    static Region spacer() {
        Region spacer = new Region();
        VBox.setVgrow(spacer, Priority.ALWAYS);
        return spacer;
    }

    static Button limeButton(String text, Runnable onClick) {
        Button button = new Button(text);
        Theme.limeButton(button);
        button.setFont(Theme.bold(14));
        button.setMaxWidth(Double.MAX_VALUE);
        button.setOnAction(e -> onClick.run());
        return button;
    }

    // Centered white text under the lime button, e.g. "Cancel"
    static Label link(String text, Runnable onClick) {
        Label label = Theme.linkLabel(text);
        label.setMaxWidth(Double.MAX_VALUE);
        label.setAlignment(Pos.CENTER);
        label.setPadding(new Insets(4, 0, 0, 0));
        label.setOnMouseClicked(e -> onClick.run());
        return label;
    }

    // ----- Formatting -----

    // 200 -> "₱200.00", 1000 -> "₱1,000.00"
    static String peso(double value) {
        return String.format(Locale.US, "₱%,.2f", value);
    }

    // "0067676767" -> "•••• 6767"
    static String masked(DemoCard card) {
        return "•••• " + card.getLastFour();
    }

    // "GCash" -> "Paid via GCash"; "Others" -> "Paid via card/e-wallet"
    static String paidVia(String method) {
        return "Paid via " + ("Others".equals(method) ? "card/e-wallet" : method);
    }
}
