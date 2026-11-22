package com.farex.ui.common;

import java.net.URL;

import javafx.scene.Node;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.Region;

public final class Icons {

    private Icons() {
    }

    // Decoded pictures, kept: every screen is rebuilt on each visit, and decoding the same PNGs again
    // each time was a big part of a slow page change on the phone. shortcut: never emptied, fine for a few dozen icons.
    private static final java.util.Map<String, Image> CACHE = new java.util.HashMap<>();

    /** The picture at url, decoded at w x h once per run (0 x 0 = its own size). FX thread only. */
    public static Image image(String url, double w, double h) {
        return CACHE.computeIfAbsent(url + "@" + w + "x" + h, k -> new Image(url, w, h, true, true));
    }

    public static boolean exists(String name) {
        return Icons.class.getResource("/com/farex/ui/images/" + name + ".png") != null;
    }

    public static Node view(String name, double size) {
        URL url = Icons.class.getResource("/com/farex/ui/images/" + name + ".png");
        if (url == null) {
            // Missing file: keep the space so the layout does not jump
            Region blank = new Region();
            blank.setPrefSize(size, size);
            blank.setMinSize(size, size);
            blank.setMaxSize(size, size);
            return blank;
        }

        // Loaded at double size so it stays sharp on high-resolution screens
        Image image = image(url.toExternalForm(), size * 2, size * 2);
        ImageView view = new ImageView(image);
        view.setFitWidth(size);
        view.setFitHeight(size);
        view.setPreserveRatio(true);
        view.setSmooth(true);
        return view;
    }
}
