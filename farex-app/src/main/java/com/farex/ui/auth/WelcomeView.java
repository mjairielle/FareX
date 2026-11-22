package com.farex.ui.auth;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.ContentDisplay;
import javafx.scene.control.Label;
import javafx.scene.shape.Polyline;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

import com.farex.ui.common.Assets;
import com.farex.ui.common.Navigator;
import com.farex.ui.common.Theme;

public class WelcomeView extends BorderPane {

    public WelcomeView() {
        setBackground(Theme.solid(Theme.DARK));

        Label line1 = Theme.headline("Your Fare,", Theme.WHITE);
        Label line2 = Theme.headline("Your Way", Theme.LIME);
        VBox headline = new VBox(0, line1, line2);
        headline.setAlignment(Pos.CENTER);

        // drawn: the phone's font has no arrow sign (it showed as a box)
        Polyline arrow = new Polyline(0, 6, 14, 6, 9, 1, 14, 6, 9, 11);
        arrow.setStroke(Theme.BLACK);
        arrow.setStrokeWidth(2);

        Button getStarted = new Button("Get Started", arrow);
        Theme.limeButton(getStarted);
        getStarted.setContentDisplay(ContentDisplay.RIGHT);
        getStarted.setGraphicTextGap(16);
        getStarted.setMaxWidth(Double.MAX_VALUE);
        getStarted.setOnAction(e -> Navigator.go("login"));

        VBox center = new VBox(28, headline, getStarted);
        center.setAlignment(Pos.CENTER);
        center.setPadding(new Insets(0, 28, 0, 28));
        setCenter(center);

        Label brand = new Label("FareX");
        brand.setTextFill(Theme.WHITE);
        brand.setFont(Theme.bold(18));
        HBox footer = new HBox(10, Assets.logo(28, 7), brand);
        footer.setAlignment(Pos.CENTER);
        footer.setPadding(new Insets(0, 0, 40, 0));
        setBottom(footer);
    }
}