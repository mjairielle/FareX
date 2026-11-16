package com.farex.ui;

import javafx.scene.effect.DropShadow;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Arc;
import javafx.scene.shape.ArcType;
import javafx.scene.shape.Circle;
import javafx.scene.layout.Pane;

// Round profile picture with a lime ring and glow.
// Shows the account's photo when one was chosen, otherwise a plain gray person.
public class Avatar extends StackPane {

    public Avatar(DemoAccount account, double size) {
        double r = size / 2;

        Circle ring = new Circle(r);
        ring.setFill(Color.web("#2B2B2B"));
        ring.setStroke(Theme.LIME);
        ring.setStrokeWidth(2.5);
        ring.setEffect(new DropShadow(18, Color.rgb(202, 254, 6, 0.35)));

        String photo = (account == null) ? null : account.getPhotoUrl();
        if (photo != null) {
            getChildren().addAll(ring, photo(photo, size - 8));
        } else {
            getChildren().addAll(ring, person(size - 8));
        }
        setMinSize(size, size);
        setPrefSize(size, size);
        setMaxSize(size, size);
    }

    // The chosen picture, cut to a circle
    private ImageView photo(String url, double size) {
        ImageView view = new ImageView(new Image(url, size * 2, size * 2, true, true));
        view.setFitWidth(size);
        view.setFitHeight(size);
        view.setPreserveRatio(false);
        view.setClip(new Circle(size / 2, size / 2, size / 2));
        return view;
    }

    // Plain avatar: head and shoulders in gray, cut to a circle
    private Pane person(double size) {
        double c = size / 2;
        Circle head = new Circle(c, size * 0.38, size * 0.18, Color.web("#8A8A8A"));
        Arc body = new Arc(c, size * 0.95, size * 0.32, size * 0.30, 0, 180);
        body.setType(ArcType.ROUND);
        body.setFill(Color.web("#8A8A8A"));

        Pane person = new Pane(head, body);
        person.setPrefSize(size, size);
        person.setMaxSize(size, size);
        person.setClip(new Circle(c, c, c));
        return person;
    }
}
