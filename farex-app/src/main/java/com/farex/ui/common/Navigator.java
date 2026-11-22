package com.farex.ui.common;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

public final class Navigator {

    private static StackPane container;
    private static final Map<String, Supplier<Parent>> screens = new HashMap<>();
    private static Node overlay;

    private Navigator() {
    }

    public static void init(StackPane root) {
        container = root;
    }

    public static void register(String name, Supplier<Parent> factory) {
        screens.put(name, factory);
    }

    public static void go(String name) {
        Supplier<Parent> factory = screens.get(name);
        Parent screen = (factory != null) ? factory.get() : comingSoon(name);
        container.getChildren().setAll(screen);
        overlay = null;
    }

    // Puts a dialog on top of the current screen; the dialog's own dim layer covers the screen behind it.
    // No blur: blurring the whole screen made every pop-up slow to open on the phone.
    public static void showOverlay(Node dialog) {
        closeOverlay();
        overlay = dialog;
        container.getChildren().add(dialog);
    }

    public static void closeOverlay() {
        if (overlay != null) {
            container.getChildren().remove(overlay);
            overlay = null;
        }
    }

    private static Parent comingSoon(String name) {
        Label message = Theme.bodyText("The \"" + name + "\" screen isn't built yet.", Theme.WHITE);

        Button back = new Button("Back to welcome");
        Theme.limeButton(back);
        back.setOnAction(e -> go("welcome"));

        VBox box = new VBox(20, message, back);
        box.setAlignment(Pos.CENTER);
        box.setBackground(Theme.solid(Theme.DARK));
        return box;
    }
}