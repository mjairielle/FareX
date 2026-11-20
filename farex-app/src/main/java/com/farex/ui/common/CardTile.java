package com.farex.ui.common;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.effect.DropShadow;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.paint.CycleMethod;
import javafx.scene.paint.LinearGradient;
import javafx.scene.paint.RadialGradient;
import javafx.scene.paint.Stop;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Polyline;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;

import com.farex.ui.data.DemoCard;

public class CardTile extends StackPane {

    private static final double W = 320;
    private static final double H = 190;
    private static final double ARC = 36;

    // Text nudges in pixels. A bigger number moves that text further down.
    private static final double XPRESS_TOP = 26;
    private static final double BALANCE_TOP = 24;
    private static final double EXPIRES_DROP = 3;

    // Logo in the middle of a plain card (View All Cards, "Other cards")
    private static final double LOGO_SIZE = 54;
    private static final double LOGO_RING = 68;

    // Full card: glow, lines, balance and copy icon (Home, My Card)
    public CardTile(DemoCard card) {
        this(card, true);
    }

    // showBalance = false -> plain card with the FareX logo instead of the balance
    public CardTile(DemoCard card, boolean showBalance) {
        setMinSize(W, H);
        setPrefSize(W, H);
        setMaxSize(W, H);

        // Keep everything (glows, lines) inside the rounded corners
        Rectangle clip = new Rectangle(W, H);
        clip.setArcWidth(ARC);
        clip.setArcHeight(ARC);
        setClip(clip);

        getChildren().addAll(backgroundLayer(showBalance), textLayer(card, showBalance));
    }

    // Background

    private Pane backgroundLayer(boolean full) {
        Rectangle base = new Rectangle(W, H);
        base.setFill(new LinearGradient(0, 0, 1, 1, true, CycleMethod.NO_CYCLE,
                new Stop(0, Color.web("#1F1F1F")), new Stop(1, Color.web("#161616"))));

        if (!full) {
            return new Pane(base);   // plain card: no glow, no lines
        }

        Rectangle oliveGlow = new Rectangle(W, H);       // top-right
        oliveGlow.setFill(new RadialGradient(0, 0, 0.84, 0.12, 0.42, true, CycleMethod.NO_CYCLE,
                new Stop(0, Color.rgb(202, 254, 6, 0.24)), new Stop(1, Color.rgb(202, 254, 6, 0))));

        Rectangle tealGlow = new Rectangle(W, H);        // bottom-left
        tealGlow.setFill(new RadialGradient(0, 0, 0.09, 0.88, 0.34, true, CycleMethod.NO_CYCLE,
                new Stop(0, Color.rgb(0, 200, 220, 0.22)), new Stop(1, Color.rgb(0, 200, 220, 0))));

        // Points are fractions of the card (x across, y down), measured from the Figma
        Polyline dashed = line(true, 0.34, 0.0, 0.158, 0.304, 0.158, 0.397, 0.316,
                0.743, 0.316, 0.845, 0.471, 1.0, 0.471);
        Polyline solid = line(false, 0.22, 0.0, 0.747, 0.224, 0.747, 0.326, 0.592,
                0.673, 0.592, 0.774, 0.747, 1.0, 0.747);

        Pane layer = new Pane(base, oliveGlow, tealGlow, dashed, solid);
        layer.getChildren().addAll(dot(0.397, 0.316), dot(0.845, 0.471), dot(0.326, 0.592));
        return layer;
    }

    // fractions = x1, y1, x2, y2, ... (each between 0 and 1); opacity = how visible the line is
    private Polyline line(boolean dashed, double opacity, double... fractions) {
        double[] points = new double[fractions.length];
        for (int i = 0; i < fractions.length; i++) {
            points[i] = fractions[i] * ((i % 2 == 0) ? W : H);   // even index = x, odd index = y
        }
        Polyline line = new Polyline(points);
        line.setStroke(Color.rgb(202, 254, 6, opacity));
        line.setStrokeWidth(1.2);
        line.setFill(null);
        if (dashed) {
            line.getStrokeDashArray().addAll(5.0, 4.0);
        }
        return line;
    }

    private Circle dot(double fx, double fy) {
        Circle dot = new Circle(fx * W, fy * H, 3);
        dot.setFill(Color.rgb(202, 254, 6, 0.45));
        return dot;
    }

    // Text

    private BorderPane textLayer(DemoCard card, boolean showBalance) {
        Label brand = new Label("xPress");
        brand.setTextFill(Theme.WHITE);
        brand.setFont(Theme.bold(14));

        Label holderCaption = caption("CARDHOLDER");
        Label holder = new Label(card.getHolderName());
        holder.setTextFill(Theme.WHITE);
        holder.setFont(Font.font("System", FontWeight.MEDIUM, 17));

        Label number = new Label(card.getFormattedNumber());
        number.setTextFill(Color.web("#D6D6D6"));
        number.setFont(Font.font("Consolas", 12));
        HBox numberRow = new HBox(6, number);
        numberRow.setAlignment(Pos.CENTER_LEFT);
        if (showBalance) {
            numberRow.getChildren().add(Icons.view("icon_copy", 12));
            numberRow.setCursor(Cursor.HAND);
        }

        VBox left = new VBox(2, holderCaption, holder, numberRow);

        Label expiresCaption = caption("EXPIRES");
        Label expires = new Label(card.getExpiry());
        expires.setTextFill(Theme.WHITE);
        expires.setFont(Theme.bold(13));
        expiresCaption.setTranslateY(EXPIRES_DROP);
        VBox right = new VBox(0, expiresCaption, expires);
        right.setAlignment(Pos.BOTTOM_RIGHT);
        right.setVisible(!card.getExpiry().startsWith("--"));   // server cards have no expiry date

        Region gap = new Region();
        HBox.setHgrow(gap, Priority.ALWAYS);
        HBox bottom = new HBox(left, gap, right);
        bottom.setAlignment(Pos.BOTTOM_LEFT);

        BorderPane layout = new BorderPane();
        layout.setPadding(new Insets(XPRESS_TOP, 18, 16, 18));
        layout.setTop(brand);
        layout.setCenter(showBalance ? balanceBox(card) : logoBadge());
        layout.setBottom(bottom);
        return layout;
    }

    private Node balanceBox(DemoCard card) {
        Label small = new Label("AVAILABLE TRANSIT BALANCE");
        small.setTextFill(Color.web("#B8C7A0"));
        small.setFont(Font.font("System", 9));

        Label peso = new Label("₱");
        peso.setTextFill(Theme.LIME);
        peso.setFont(Theme.bold(24));
        Label amount = new Label(card.getBalanceText());
        amount.setTextFill(Theme.LIME);
        amount.setFont(Theme.bold(34));
        HBox balanceRow = new HBox(6, peso, amount);
        balanceRow.setAlignment(Pos.CENTER);
        balanceRow.setTranslateY(-3);

        // Starts just below the dashed line
        VBox center = new VBox(0, small, balanceRow);
        center.setAlignment(Pos.TOP_CENTER);
        BorderPane.setMargin(center, new Insets(BALANCE_TOP, 0, 0, 0));
        return center;
    }

    // Round lime logo inside a dark ring with a soft glow
    private Node logoBadge() {
        Circle ring = new Circle(LOGO_RING / 2, Color.web("#111111"));
        ring.setEffect(new DropShadow(18, Color.rgb(202, 254, 6, 0.25)));

        StackPane badge = new StackPane(ring, Assets.logo(LOGO_SIZE, LOGO_SIZE / 2));
        badge.setMaxSize(LOGO_RING, LOGO_RING);
        BorderPane.setAlignment(badge, Pos.CENTER);
        return badge;
    }

    private Label caption(String text) {
        Label label = new Label(text);
        label.setTextFill(Color.web("#8FA08A"));
        label.setFont(Font.font("System", 8));
        return label;
    }
}
