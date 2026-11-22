package com.farex.ui.common;

import java.util.function.Consumer;
import java.util.function.Supplier;

import javafx.animation.FadeTransition;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.util.Duration;

import com.farex.ui.trip.TripUi;

// "Logging in…" with a spinner over the screen while work talks to the server, off the UI thread: done on the
// UI thread, the app froze until the reply came. Clicks are blocked from the first moment (no double taps), but the
// spinner only fades in after a short wait, so quick work (mock mode, a fast reply) shows no flash.
public final class Loading extends StackPane {

    private Loading(String message) {
        setBackground(Theme.solid(Color.rgb(0, 0, 0, 0.55)));
        ProgressIndicator spin = new ProgressIndicator();
        spin.setMaxSize(44, 44);
        spin.setStyle("-fx-progress-color: #CAFE06;");
        VBox box = new VBox(14, spin, TripUi.bold(message, 15));
        box.setAlignment(Pos.CENTER);
        box.setPadding(new Insets(24, 28, 22, 28));
        box.setMaxSize(VBox.USE_PREF_SIZE, VBox.USE_PREF_SIZE);
        box.setBackground(Theme.rounded(Color.web("#1E1E1E"), 14));
        getChildren().add(box);

        setOpacity(0);
        FadeTransition in = new FadeTransition(Duration.millis(150), this);
        in.setDelay(Duration.millis(250));
        in.setToValue(1);
        in.play();
    }

    /** Runs work in the background behind a loading screen, then done(result) on the UI thread. */
    public static <T> void run(String message, Supplier<T> work, Consumer<T> done) {
        Loading screen = new Loading(message);
        Navigator.showOverlay(screen);
        Thread t = new Thread(() -> {
            T result = null;
            RuntimeException failed = null;
            try {
                result = work.get();
            } catch (RuntimeException e) {
                failed = e;
            }
            T r = result;
            RuntimeException f = failed;
            Platform.runLater(() -> {
                Navigator.closeOverlay(screen);
                if (f != null) {
                    new PopupDialog(WarningDialog.warningIcon(), "Something went wrong",
                            f.getMessage() == null ? "Please try again." : f.getMessage()).button("OK", () -> { }).show();
                } else {
                    done.accept(r);
                }
            });
        }, "farex-loading");
        t.setDaemon(true);
        t.start();
    }

    /** Same, for work with nothing to hand back. */
    public static void run(String message, Runnable work, Runnable done) {
        run(message, () -> {
            work.run();
            return Boolean.TRUE;
        }, ignored -> done.run());
    }
}
