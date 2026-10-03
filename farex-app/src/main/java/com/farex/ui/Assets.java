package com.farex.ui;

import java.net.URL;

import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.shape.Rectangle;

/** Loads shared images. If the logo file is missing, shows the text "FareX" instead. */
public final class Assets {

    private Assets() {
    }

    /**
     * @param size         width and height in pixels, e.g. 96
     * @param cornerRadius rounded corners in pixels; 0 keeps square corners
     */
    public static Node logo(double size, double cornerRadius) {
        URL url = Assets.class.getResource("images/farex_logo.png");
        if (url == null) {
            Label fallback = new Label("FareX");
            fallback.setTextFill(Theme.BLACK);
            fallback.setFont(Theme.bold(36));
            return fallback;
        }

        ImageView view = new ImageView(new Image(url.toExternalForm()));
        view.setFitWidth(size);
        view.setFitHeight(size);
        view.setPreserveRatio(true);
        view.setSmooth(true);

        if (cornerRadius > 0) {
            Rectangle clip = new Rectangle(size, size);
            clip.setArcWidth(cornerRadius * 2);
            clip.setArcHeight(cornerRadius * 2);
            view.setClip(clip);
        }
        return view;
    }
}