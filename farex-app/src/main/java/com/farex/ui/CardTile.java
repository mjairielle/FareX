package com.farex.ui;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.control.Label;
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

public class CardTile extends StackPane {

    private static final double W = 320;
    private static final double H = 190;
    private static final double ARC = 36;

    public CardTile(DemoCard card) {
        setMinSize(W, H);
        setPrefSize(W, H);
        setMaxSize(W, H);

        // Keep everything (glows, lines) inside the rounded corners
        Rectangle clip = new Rectangle(W, H);
        clip.setArcWidth(ARC);
        clip.setArcHeight(ARC);
        setClip(clip);

        getChildren().addAll(backgroundLayer(), textLayer(card));
    }

    // Background

    private Pane backgroundLayer() {
        Rectangle base = new Rectangle(W, H);
        base.setFill(new LinearGradient(0, 0, 1, 1, true, CycleMethod.NO_CYCLE,
                new Stop(0, Color.web("#1F1F1F")), new Stop(1, Color.web("#161616"))));

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

    private BorderPane textLayer(DemoCard card) {
        Label brand = new Label("xPress");
        brand.setTextFill(Theme.WHITE);
        brand.setFont(Theme.bold(14));

        Label small = new Label("AVAILABLE TRANSIT BALANCE");
        small.setTextFill(Color.web("#B8C7A0"));
        small.setFont(Font.font("System", 9));

        Label peso = new Label("\u20B1");
        peso.setTextFill(Theme.LIME);
        peso.setFont(Theme.bold(24));
        Label amount = new Label(card.getBalanceText());
        amount.setTextFill(Theme.LIME);
        amount.setFont(Theme.bold(34));
        HBox balanceRow = new HBox(6, peso, amount);
        balanceRow.setAlignment(Pos.CENTER);

        VBox center = new VBox(2, small, balanceRow);
        center.setAlignment(Pos.CENTER);

        Label holderCaption = caption("CARDHOLDER");
        Label holder = new Label(card.getHolderName());
        holder.setTextFill(Theme.WHITE);
        holder.setFont(Font.font("System", FontWeight.MEDIUM, 17));

        Label number = new Label(card.getFormattedNumber());
        number.setTextFill(Color.web("#D6D6D6"));
        number.setFont(Font.font("Consolas", 12));
        HBox numberRow = new HBox(6, number, Icons.view("icon_copy", 12));
        numberRow.setAlignment(Pos.CENTER_LEFT);
        numberRow.setCursor(Cursor.HAND);

        VBox left = new VBox(2, holderCaption, holder, numberRow);

        Label expiresCaption = caption("EXPIRES");
        Label expires = new Label(card.getExpiry());
        expires.setTextFill(Theme.WHITE);
        expires.setFont(Theme.bold(13));
        VBox right = new VBox(2, expiresCaption, expires);
        right.setAlignment(Pos.BOTTOM_RIGHT);

        Region gap = new Region();
        HBox.setHgrow(gap, Priority.ALWAYS);
        HBox bottom = new HBox(left, gap, right);
        bottom.setAlignment(Pos.BOTTOM_LEFT);

        BorderPane layout = new BorderPane();
        layout.setPadding(new Insets(16, 18, 16, 18));
        layout.setTop(brand);
        layout.setCenter(center);
        layout.setBottom(bottom);
        return layout;
    }

    private Label caption(String text) {
        Label label = new Label(text);
        label.setTextFill(Color.web("#8FA08A"));
        label.setFont(Font.font("System", 8));
        return label;
    }
}
