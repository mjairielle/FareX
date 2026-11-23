package com.farex.ui.common;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

public abstract class SubScreen extends VBox {

    protected final VBox body = new VBox(16);

    // backTarget = null -> no back arrow (result screens like "Load fetched successfully")
    protected SubScreen(String title, String backTarget) {
        this(title, (backTarget == null) ? null : () -> Navigator.go(backTarget));
    }

    // onBack runs when the arrow is clicked, e.g. StepScreen goes to the previous step
    protected SubScreen(String title, Runnable onBack) {
        setBackground(Theme.solid(Theme.DARK));

        body.setPadding(new Insets(28, 20, 24, 20));
        VBox.setVgrow(body, Priority.ALWAYS);

        getChildren().addAll(header(title, onBack), body);
    }

    // The lime bar with the title and back arrow. Static so StepScreen can rebuild it
    // with its own back action, and other screens can borrow the same header.
    public static StackPane header(String title, String backTarget) {
        return header(title, (backTarget == null) ? null : () -> Navigator.go(backTarget));
    }

    public static StackPane header(String title, Runnable onBack) {
        Label titleLabel = new Label(title);
        titleLabel.setTextFill(Theme.BLACK);
        titleLabel.setFont(Theme.bold(16));

        StackPane header = new StackPane(titleLabel);
        header.setBackground(Theme.solid(Theme.LIME));
        header.setMinHeight(76);
        header.setPrefHeight(76);
        header.setMaxHeight(76);

        if (onBack != null) {
            Node back = backButton(Theme.BLACK, onBack);
            StackPane.setAlignment(back, Pos.CENTER_LEFT);
            StackPane.setMargin(back, new Insets(0, 0, 0, 6));
            header.getChildren().add(back);
        }
        return header;
    }

    // The back arrow: a drawn chevron (the font's "‹" was too small to see and to hit) in a 48 x 48 touch area,
    // the size Android asks for
    public static Node backButton(javafx.scene.paint.Color color, Runnable onBack) {
        javafx.scene.shape.Polyline chevron = new javafx.scene.shape.Polyline(13, 0, 0, 12, 13, 24);
        chevron.setStroke(color);
        chevron.setStrokeWidth(3.2);
        chevron.setStrokeLineCap(javafx.scene.shape.StrokeLineCap.ROUND);
        chevron.setStrokeLineJoin(javafx.scene.shape.StrokeLineJoin.ROUND);
        chevron.setFill(null);
        StackPane button = new StackPane(chevron);
        button.setMinSize(48, 48);
        button.setMaxSize(48, 48);
        button.setPickOnBounds(true);                 // the whole square takes the tap, not just the thin line
        button.setCursor(Cursor.HAND);
        button.setOnMouseClicked(e -> onBack.run());
        Navigator.setBack(button, onBack);                // the phone's back does the same
        return button;
    }
}
