package com.farex.ui.common;

import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Polyline;
import javafx.scene.shape.StrokeLineCap;
import javafx.scene.shape.StrokeLineJoin;

// Lime ring with a double check mark (Congrats screen and Load Receipt)
public class CheckRing extends Pane {

    // size 90 is the Congrats ring; every point is drawn on a 90 x 90 grid and scaled
    public CheckRing(double size, Color fill) {
        double k = size / 90;

        Circle ring = new Circle(45 * k, 45 * k, 42 * k);
        ring.setFill(fill);
        ring.setStroke(Theme.LIME);
        ring.setStrokeWidth(4 * k);

        getChildren().addAll(ring, tick(0, k), tick(14, k));
        setMinSize(size, size);
        setPrefSize(size, size);
        setMaxSize(size, size);
    }

    // One check mark; the second is shifted right by 14 so the two overlap
    private Polyline tick(double dx, double k) {
        Polyline tick = new Polyline((24 + dx) * k, 44 * k, (34 + dx) * k, 56 * k, (52 + dx) * k, 34 * k);
        tick.setFill(Color.TRANSPARENT);
        tick.setStroke(Theme.LIME);
        tick.setStrokeWidth(5 * k);
        tick.setStrokeLineCap(StrokeLineCap.ROUND);
        tick.setStrokeLineJoin(StrokeLineJoin.ROUND);
        return tick;
    }
}
