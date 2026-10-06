package com.farex.ui;

import javafx.geometry.Pos;
import javafx.scene.control.Label;

/** Stands in for any screen that has no Figma frame yet. */
public class PlaceholderView extends SubScreen {

    public PlaceholderView(String title, String backTarget) {
        super(title, backTarget);
        Label message = Theme.bodyText("This screen isn't designed yet.", Theme.GRAY);
        body.setAlignment(Pos.CENTER);
        body.getChildren().add(message);
    }
}
