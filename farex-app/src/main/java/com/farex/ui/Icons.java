package com.farex.ui;

import java.net.URL;

import javafx.scene.Node;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.Region;

/**
 * Loads icon images from the images folder.
 * Icons.view("icon_user", 20) shows images/icon_user.png at 20 x 20 pixels.
 */
public final class Icons {

    private Icons() {
    }

    public static boolean exists(String name) {
        return Icons.class.getResource("images/" + name + ".png") != null;
    }

    public static Node view(String name, double size) {
        URL url = Icons.class.getResource("images/" + name + ".png");
        if (url == null) {
            // Missing file: keep the space so the layout does not jump
            Region blank = new Region();
            blank.setPrefSize(size, size);
            blank.setMinSize(size, size);
            blank.setMaxSize(size, size);
            return blank;
        }

        // Loaded at double size so it stays sharp on high-resolution screens
        Image image = new Image(url.toExternalForm(), size * 2, size * 2, true, true);
        ImageView view = new ImageView(image);
        view.setFitWidth(size);
        view.setFitHeight(size);
        view.setPreserveRatio(true);
        view.setSmooth(true);
        return view;
    }
}
