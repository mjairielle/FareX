package com.farex.ui.common;

import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.layout.Border;
import javafx.scene.layout.BorderStroke;
import javafx.scene.layout.BorderStrokeStyle;
import javafx.scene.layout.BorderWidths;
import javafx.scene.layout.CornerRadii;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;

// On/off switch: lime track with a dark knob on the right when on,
// dark track with a white outline and a white knob on the left when off
public class ToggleSwitch extends StackPane {

    private static final double W = 36;
    private static final double H = 20;

    private final Circle knob = new Circle(7);
    private boolean on;

    public ToggleSwitch(boolean startOn) {
        setMinSize(W, H);
        setPrefSize(W, H);
        setMaxSize(W, H);
        setCursor(Cursor.HAND);
        getChildren().add(knob);
        setOnMouseClicked(e -> setOn(!on));
        setOn(startOn);
    }

    public boolean isOn() {
        return on;
    }

    public void setOn(boolean value) {
        on = value;
        if (on) {
            setBackground(Theme.rounded(Theme.LIME, H / 2));
            setBorder(null);
            knob.setFill(Color.web("#141414"));
            setAlignment(Pos.CENTER_RIGHT);
        } else {
            setBackground(Theme.rounded(Color.web("#141414"), H / 2));
            setBorder(new Border(new BorderStroke(Theme.WHITE,
                    BorderStrokeStyle.SOLID, new CornerRadii(H / 2), new BorderWidths(1.2))));
            knob.setFill(Theme.WHITE);
            setAlignment(Pos.CENTER_LEFT);
        }
        setStyle("-fx-padding: 0 3 0 3;");
    }
}
