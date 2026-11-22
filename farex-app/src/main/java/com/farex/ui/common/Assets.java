package com.farex.ui.common;

import java.net.URL;

import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.image.ImageView;
import javafx.scene.shape.Rectangle;

public final class Assets {

    private Assets() {
    }

    public static Node logo(double size, double cornerRadius) {
        URL url = Assets.class.getResource("/com/farex/ui/images/farex_logo.png");
        if (url == null) {
            Label fallback = new Label("FareX");
            fallback.setTextFill(Theme.BLACK);
            fallback.setFont(Theme.bold(36));
            return fallback;
        }

        ImageView view = new ImageView(Icons.image(url.toExternalForm(), 0, 0));
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