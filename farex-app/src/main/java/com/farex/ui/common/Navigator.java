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
    private static String current = "";                     // the screen on show, by its registered name
    private static final Map<String, Runnable> backs = new HashMap<>();   // phone back for screens with no arrow

    private static final String BACK = "farex.back";        // node property: what phone back does there
    private static final String NO_BACK = "farex.noBack";   // overlay property: phone back cannot close it

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
        current = name;
        container.getChildren().setAll(screen);
        overlay = null;
    }

    // ----- The phone's back (button or swipe; Gluon delivers it as the Esc key) -----

    /** What phone back does on a screen that has no back arrow, e.g. login -> welcome. */
    public static void onBack(String screenName, Runnable action) {
        backs.put(screenName, action);
    }

    /** Phone back runs action while node is on screen: a back arrow, or a screen with its own rule (the main tabs). */
    public static void setBack(Node node, Runnable action) {
        node.getProperties().put(BACK, action);
    }

    /** An overlay phone back must not close (a loading screen, a pop-up whose button is the only way on). */
    public static void keepOnBack(Node overlay) {
        overlay.getProperties().put(NO_BACK, true);
    }

    // A pop-up closes (then runs its own back, if it set one); otherwise the back arrow on show is pressed
    // (the innermost one), else the screen's own rule
    public static void back() {
        if (overlay != null) {
            if (!overlay.getProperties().containsKey(NO_BACK)) {
                Runnable after = overlay.getProperties().get(BACK) instanceof Runnable r ? r : null;
                closeOverlay();
                if (after != null) {
                    after.run();
                }
            }
            return;
        }
        Runnable action = backOf(container.getChildren().get(0));
        if (action == null) {
            action = backs.get(current);
        }
        if (action != null) {
            action.run();
        }
    }

    private static Runnable backOf(Node node) {
        if (!node.isVisible()) {
            return null;                                     // a hidden step's arrow does not count
        }
        if (node instanceof Parent parent) {
            for (Node child : parent.getChildrenUnmodifiable()) {
                Runnable inner = backOf(child);
                if (inner != null) {
                    return inner;
                }
            }
        }
        return node.getProperties().get(BACK) instanceof Runnable r ? r : null;
    }

    // On a home screen: the first back says how to leave, a second one within 2 s closes the app
    private static long lastBack;

    public static void backToExit() {
        long now = System.currentTimeMillis();
        if (now - lastBack < 2000) {
            com.gluonhq.attach.lifecycle.LifecycleService.create()
                    .ifPresentOrElse(com.gluonhq.attach.lifecycle.LifecycleService::shutdown, javafx.application.Platform::exit);
            return;
        }
        lastBack = now;
        toast("Press back again to exit");
    }

    // A short note at the bottom that fades away by itself; it takes no taps and is not an overlay
    public static void toast(String text) {
        Label note = CommonUi.small(text, Theme.WHITE);
        note.setFont(Theme.bold(12));
        note.setPadding(new javafx.geometry.Insets(10, 16, 10, 16));
        note.setBackground(Theme.rounded(javafx.scene.paint.Color.web("#2E2E2E"), 20));
        note.setMouseTransparent(true);
        note.setMaxSize(Label.USE_PREF_SIZE, Label.USE_PREF_SIZE);
        StackPane.setAlignment(note, Pos.BOTTOM_CENTER);
        StackPane.setMargin(note, new javafx.geometry.Insets(0, 0, 110, 0));
        container.getChildren().add(note);
        javafx.animation.FadeTransition fade = new javafx.animation.FadeTransition(javafx.util.Duration.millis(400), note);
        fade.setDelay(javafx.util.Duration.millis(1600));
        fade.setToValue(0);
        fade.setOnFinished(e -> container.getChildren().remove(note));
        fade.play();
    }

    // Puts a dialog on top of the current screen; the dialog's own dim layer covers the screen behind it.
    // No blur: blurring the whole screen made every pop-up slow to open on the phone.
    public static void showOverlay(Node dialog) {
        closeOverlay();
        overlay = dialog;
        container.getChildren().add(dialog);
    }

    // Closes this dialog only if it is still the one showing (a screen change may have removed it already)
    public static void closeOverlay(Node dialog) {
        if (overlay == dialog) {
            closeOverlay();
        }
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