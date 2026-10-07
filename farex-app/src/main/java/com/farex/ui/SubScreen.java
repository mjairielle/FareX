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

    protected SubScreen(String title, String backTarget) {
        setBackground(Theme.solid(Theme.DARK));

        Label titleLabel = new Label(title);
        titleLabel.setTextFill(Theme.BLACK);
        titleLabel.setFont(Theme.bold(16));

        Label back = new Label("\u2039");
        back.setTextFill(Theme.BLACK);
        back.setFont(Theme.bold(36));
        back.setCursor(Cursor.HAND);
        back.setOnMouseClicked(e -> Navigator.go(backTarget));
        StackPane.setAlignment(back, Pos.CENTER_LEFT);
        StackPane.setMargin(back, new Insets(0, 0, 4, 20));

        StackPane header = new StackPane(titleLabel, back);
        header.setBackground(Theme.solid(Theme.LIME));
        header.setMinHeight(76);
        header.setPrefHeight(76);
        header.setMaxHeight(76);

        body.setPadding(new Insets(28, 20, 24, 20));
        VBox.setVgrow(body, Priority.ALWAYS);

        getChildren().addAll(header, body);
    }
}
