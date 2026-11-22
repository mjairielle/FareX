package com.farex.ui.auth;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.CubicCurve;
import javafx.scene.text.TextAlignment;

import com.farex.ui.common.CheckRing;
import com.farex.ui.common.Navigator;
import com.farex.ui.common.Theme;

public class CongratsView extends VBox {

    public CongratsView() {
        setBackground(Theme.solid(Theme.DARK));
        setAlignment(Pos.CENTER);
        setSpacing(16);
        setPadding(new Insets(28));

        Region topSpacer = new Region();
        VBox.setVgrow(topSpacer, Priority.ALWAYS);
        Region bottomSpacer = new Region();
        VBox.setVgrow(bottomSpacer, Priority.ALWAYS);

        Label title = new Label("Congratulations!");
        title.setTextFill(Theme.WHITE);
        title.setFont(Theme.bold(30));
        VBox.setMargin(title, new Insets(16, 0, 0, 0));

        // A new operator cannot log in until an admin approves; say so instead of a surprise error at login
        Label message = Theme.bodyText("Operator".equals(SignUpState.getRole())
                ? "Your operator account is created. An admin must approve it before your first login."
                : "You have successfully created your account.", Theme.WHITE);
        message.setWrapText(true);
        message.setTextAlignment(TextAlignment.CENTER);
        message.setAlignment(Pos.CENTER);
        message.setMaxWidth(240);

        Button startButton = new Button("Let's start the new journey!");
        Theme.limeButton(startButton);
        startButton.setMaxWidth(Double.MAX_VALUE);
        startButton.setOnAction(e -> Navigator.go("login"));

        getChildren().addAll(topSpacer, buildArt(), title, message, bottomSpacer, startButton);
    }

    // Ring with a double check mark on top, two dashed curves behind its lower half
    private StackPane buildArt() {
        Pane roads = new Pane();
        roads.setPrefSize(300, 90);
        roads.setMinSize(300, 90);
        roads.setMaxSize(300, 90);
        roads.getChildren().addAll(
                road(10, 30, 90, 0, 130, 80, 290, 35),
                road(30, 60, 120, 30, 170, 95, 270, 70));

        CheckRing icon = new CheckRing(90, Color.TRANSPARENT);

        StackPane art = new StackPane(roads, icon);
        StackPane.setAlignment(icon, Pos.TOP_CENTER);
        StackPane.setAlignment(roads, Pos.BOTTOM_CENTER);
        art.setMinHeight(150);
        art.setPrefHeight(150);
        art.setMaxWidth(300);
        return art;
    }

    // Dashed curve from (sx, sy) to (ex, ey), bent by two control points
    private CubicCurve road(double sx, double sy, double c1x, double c1y,
                            double c2x, double c2y, double ex, double ey) {
        CubicCurve curve = new CubicCurve(sx, sy, c1x, c1y, c2x, c2y, ex, ey);
        curve.setFill(Color.TRANSPARENT);
        curve.setStroke(Theme.LIME);
        curve.setStrokeWidth(2);
        curve.getStrokeDashArray().addAll(8.0, 8.0);
        curve.setOpacity(0.7);
        return curve;
    }
}