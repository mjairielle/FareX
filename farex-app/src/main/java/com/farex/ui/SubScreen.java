package com.farex.ui;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.control.Label;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

public abstract class SubScreen extends VBox {

    protected final VBox body = new VBox(16);

    // backTarget = null -> no back arrow (result screens like "Load fetched successfully")
    protected SubScreen(String title, String backTarget) {
        setBackground(Theme.solid(Theme.DARK));

        body.setPadding(new Insets(28, 20, 24, 20));
        VBox.setVgrow(body, Priority.ALWAYS);

        getChildren().addAll(header(title, backTarget), body);
    }

    // The lime bar with the title and back arrow. Static so screens that can't extend
    // SubScreen (LostCardOtpView extends OtpView) can use the same header.
    public static StackPane header(String title, String backTarget) {
        Label titleLabel = new Label(title);
        titleLabel.setTextFill(Theme.BLACK);
        titleLabel.setFont(Theme.bold(16));

        StackPane header = new StackPane(titleLabel);
        header.setBackground(Theme.solid(Theme.LIME));
        header.setMinHeight(76);
        header.setPrefHeight(76);
        header.setMaxHeight(76);

        if (backTarget != null) {
            Label back = new Label("‹");
            back.setTextFill(Theme.BLACK);
            back.setFont(Theme.bold(36));
            back.setCursor(Cursor.HAND);
            back.setOnMouseClicked(e -> Navigator.go(backTarget));
            StackPane.setAlignment(back, Pos.CENTER_LEFT);
            StackPane.setMargin(back, new Insets(0, 0, 4, 20));
            header.getChildren().add(back);
        }
        return header;
    }
}
