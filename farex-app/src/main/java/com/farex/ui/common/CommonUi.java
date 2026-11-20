package com.farex.ui.common;

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
import javafx.scene.Group;
import javafx.scene.shape.Polyline;
import javafx.scene.shape.StrokeLineCap;
import javafx.scene.shape.StrokeLineJoin;
import javafx.scene.text.Font;
import javafx.scene.text.TextAlignment;

import com.farex.ui.data.DemoCard;

// Shared pieces for the Fetch Load, Redeem and Lost Card screens:
// titles, gray panels, label/value rows, buttons, result icons
public final class CommonUi {

    public static final Color PANEL = Color.web("#1E1E1E");
    public static final Color SOFT_TEXT = Color.web("#E6E6E6");

    private CommonUi() {
    }

    // ----- Text -----

    public static Label title(String text) {
        Label label = new Label(text);
        label.setTextFill(Theme.WHITE);
        label.setFont(Theme.bold(18));
        label.setWrapText(true);
        return label;
    }

    public static Label subtitle(String text) {
        Label label = small(text, SOFT_TEXT);
        label.setWrapText(true);
        return label;
    }

    public static Label small(String text, Color color) {
        Label label = new Label(text);
        label.setTextFill(color);
        label.setFont(Font.font("System", 11));
        return label;
    }

    public static Label smallBold(String text, Color color) {
        Label label = new Label(text);
        label.setTextFill(color);
        label.setFont(Theme.bold(11));
        return label;
    }

    // Icon, big title and a centered sentence, e.g. the warning sign + "Couldn't fetch your load"
    public static VBox resultHeader(Node icon, String titleText, String text) {
        Label title = title(titleText);
        title.setFont(Theme.bold(20));
        Label sentence = subtitle(text);
        sentence.setTextAlignment(TextAlignment.CENTER);
        sentence.setMaxWidth(260);

        VBox header = new VBox(12, icon, title, sentence);
        header.setAlignment(Pos.TOP_CENTER);
        VBox.setMargin(header, new Insets(46, 0, 0, 0));
        return header;
    }

    // Two overlapping lime check marks (same shape as CheckRing, without the ring)
    public static Group doubleCheck() {
        return new Group(tick(0), tick(14));
    }

    private static Polyline tick(double dx) {
        double k = 1.1;
        Polyline tick = new Polyline((4 + dx) * k, 14 * k, (14 + dx) * k, 26 * k, (32 + dx) * k, 4 * k);
        tick.setFill(Color.TRANSPARENT);
        tick.setStroke(Theme.LIME);
        tick.setStrokeWidth(5);
        tick.setStrokeLineCap(StrokeLineCap.ROUND);
        tick.setStrokeLineJoin(StrokeLineJoin.ROUND);
        return tick;
    }

    // ----- Panels -----

    public static VBox panel(Node... children) {
        VBox panel = new VBox(10, children);
        panel.setPadding(new Insets(14));
        panel.setBackground(Theme.rounded(PANEL, 8));
        return panel;
    }

    // Gray box with one wrapped sentence, e.g. "Keep your physical card nearby..."
    public static VBox note(String text) {
        Label label = small(text, Theme.WHITE);
        label.setWrapText(true);
        return panel(label);
    }

    // "Reference ............ FX-LD-1024"
    public static HBox row(String label, String value) {
        return row(label, smallBold(value, Theme.WHITE));
    }

    public static HBox row(String label, Node value) {
        Region fill = new Region();
        HBox.setHgrow(fill, Priority.ALWAYS);
        HBox row = new HBox(8, small(label, SOFT_TEXT), fill, value);
        row.setAlignment(Pos.CENTER_LEFT);
        return row;
    }

    // ----- Bottom of the screen -----

    public static Region spacer() {
        Region spacer = new Region();
        VBox.setVgrow(spacer, Priority.ALWAYS);
        return spacer;
    }

    public static Button limeButton(String text, Runnable onClick) {
        Button button = new Button(text);
        Theme.limeButton(button);
        button.setFont(Theme.bold(14));
        button.setMaxWidth(Double.MAX_VALUE);
        button.setOnAction(e -> onClick.run());
        return button;
    }

    // Centered white text under the lime button, e.g. "Cancel"
    public static Label link(String text, Runnable onClick) {
        Label label = Theme.linkLabel(text);
        label.setMaxWidth(Double.MAX_VALUE);
        label.setAlignment(Pos.CENTER);
        label.setPadding(new Insets(4, 0, 0, 0));
        label.setOnMouseClicked(e -> onClick.run());
        return label;
    }

    // ----- Formatting -----

    // 200 -> "₱200.00", 1000 -> "₱1,000.00"
    public static String peso(double value) {
        return String.format(Locale.US, "₱%,.2f", value);
    }

    // "0067676767" -> "•••• 6767"
    public static String masked(DemoCard card) {
        return "•••• " + card.getLastFour();
    }

    // "GCash" -> "Paid via GCash"; "Others" -> "Paid via card/e-wallet"
    // "Points" (a redeemed reward) -> "From xPress Points"
    public static String paidVia(String method) {
        if ("Points".equals(method)) {
            return "From xPress Points";
        }
        return "Paid via " + ("Others".equals(method) ? "card/e-wallet" : method);
    }

    // 100 -> "100 pts"
    public static String pts(int points) {
        return points + " pts";
    }
}
