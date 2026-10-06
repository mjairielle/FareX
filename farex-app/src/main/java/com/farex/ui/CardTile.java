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

/**
 * The xPress card from the Figma: dark gradient, lime and teal glows, faint dashed lines,
 * balance in the middle, cardholder + number bottom-left, expiry bottom-right.
 * Fixed size 320 x 190 so it lines up with the 20px screen margins.
 */
public class CardTile extends StackPane {

    private static final double W = 320;
    private static final double H = 190;
    private static final double ARC = 36;      // corner rounding (arc width = 2 x radius 18)

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

    // ---------- Background: base color + two glows + dashed circuit lines ----------

    private Pane backgroundLayer() {
        Rectangle base = new Rectangle(W, H);
        base.setFill(new LinearGradient(0, 0, 1, 1, true, CycleMethod.NO_CYCLE,
                new Stop(0, Color.web("#1C2020")), new Stop(1, Color.web("#101313"))));

        Rectangle limeGlow = new Rectangle(W, H);       // top-right
        limeGlow.setFill(new RadialGradient(0, 0, 0.95, 0.05, 0.75, true, CycleMethod.NO_CYCLE,
                new Stop(0, Color.rgb(202, 254, 6, 0.32)), new Stop(1, Color.rgb(202, 254, 6, 0))));

        Rectangle tealGlow = new Rectangle(W, H);       // bottom-left
        tealGlow.setFill(new RadialGradient(0, 0, 0.02, 0.98, 0.75, true, CycleMethod.NO_CYCLE,
                new Stop(0, Color.rgb(0, 190, 210, 0.30)), new Stop(1, Color.rgb(0, 190, 210, 0))));

        Polyline lineTop = dashedLine(0, 78, 70, 78, 96, 100, 225, 100, 250, 124, W, 124);
        Polyline lineBottom = dashedLine(0, 150, 90, 150, 112, 128, 190, 128);
        Circle dot1 = lineDot(250, 124);
        Circle dot2 = lineDot(96, 100);

        return new Pane(base, limeGlow, tealGlow, lineTop, lineBottom, dot1, dot2);
    }

    private Polyline dashedLine(double... points) {
        Polyline line = new Polyline(points);
        line.setStroke(Color.rgb(202, 254, 6, 0.28));
        line.setStrokeWidth(1);
        line.getStrokeDashArray().addAll(3.0, 3.0);
        line.setFill(null);
        return line;
    }

    private Circle lineDot(double x, double y) {
        Circle dot = new Circle(x, y, 2.5);
        dot.setFill(Color.rgb(202, 254, 6, 0.45));
        return dot;
    }

    // ---------- Text ----------

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
