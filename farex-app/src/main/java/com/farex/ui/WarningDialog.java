package com.farex.ui;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Group;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.Border;
import javafx.scene.layout.BorderStroke;
import javafx.scene.layout.BorderStrokeStyle;
import javafx.scene.layout.BorderWidths;
import javafx.scene.layout.CornerRadii;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Line;
import javafx.scene.shape.Polygon;
import javafx.scene.shape.StrokeLineCap;
import javafx.scene.shape.StrokeLineJoin;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;
import javafx.scene.text.TextAlignment;
import javafx.scene.text.TextFlow;

// Shown over Home when a card feature is tapped on an account with no card
public class WarningDialog extends StackPane {

    public WarningDialog() {
        // Dim layer over the blurred screen; it also stops clicks reaching the screen below
        setBackground(Theme.solid(Color.rgb(0, 0, 0, 0.45)));

        Label title = new Label("Warning");
        title.setTextFill(Theme.WHITE);
        title.setFont(Theme.bold(22));

        Button addCard = new Button("Add Card");
        addCard.setFont(Theme.bold(13));
        addCard.setPadding(new Insets(8));
        addCard.setMaxWidth(Double.MAX_VALUE);
        Theme.paintButton(addCard, "#CAFE06", "#D8FF3A", "#A9D400", 6, "black");
        addCard.setOnAction(e -> Navigator.go("addCard"));

        Button cancel = new Button("Cancel");
        cancel.setFont(Theme.bold(13));
        cancel.setPadding(new Insets(8));
        cancel.setMaxWidth(Double.MAX_VALUE);
        Theme.paintButton(cancel, "#141414", "#1E1E1E", "#0E0E0E", 6, "white");
        cancel.setBorder(new Border(new BorderStroke(Color.web("#4A4A4A"),
                BorderStrokeStyle.SOLID, new CornerRadii(6), new BorderWidths(1))));
        cancel.setOnAction(e -> Navigator.closeOverlay());

        VBox box = new VBox(10, warningIcon(), title, message(), addCard, cancel);
        box.setAlignment(Pos.CENTER);
        box.setPadding(new Insets(22, 20, 18, 20));
        box.setMaxSize(270, VBox.USE_PREF_SIZE);
        box.setBackground(Theme.rounded(Color.web("#1C1C1C"), 10));
        box.setBorder(new Border(new BorderStroke(Theme.LIME,
                BorderStrokeStyle.SOLID, new CornerRadii(10), new BorderWidths(1.5))));
        VBox.setMargin(title, new Insets(-4, 0, 0, 0));
        VBox.setMargin(addCard, new Insets(6, 0, 0, 0));

        getChildren().add(box);
    }

    // "No xPress Card yet, Click Add Card to continue" with "Add Card" in bold
    private TextFlow message() {
        Text start = new Text("No xPress Card yet,\nClick ");
        Text bold = new Text("Add Card");
        Text end = new Text(" to continue");
        start.setFont(Font.font("System", 12));
        bold.setFont(Font.font("System", FontWeight.BOLD, 12));
        end.setFont(Font.font("System", 12));
        for (Text part : new Text[] {start, bold, end}) {
            part.setFill(Theme.WHITE);
        }
        TextFlow flow = new TextFlow(start, bold, end);
        flow.setTextAlignment(TextAlignment.CENTER);
        return flow;
    }

    // Rounded lime triangle with an exclamation mark (also used on the Fetch Load screens)
    public static Group warningIcon() {
        Polygon triangle = new Polygon(32, 4, 60, 52, 4, 52);
        triangle.setFill(Color.TRANSPARENT);
        triangle.setStroke(Theme.LIME);
        triangle.setStrokeWidth(5);
        triangle.setStrokeLineJoin(StrokeLineJoin.ROUND);

        Line bar = new Line(32, 21, 32, 35);
        bar.setStroke(Theme.LIME);
        bar.setStrokeWidth(5);
        bar.setStrokeLineCap(StrokeLineCap.ROUND);

        Circle dot = new Circle(32, 44, 3, Theme.LIME);
        return new Group(triangle, bar, dot);
    }
}
