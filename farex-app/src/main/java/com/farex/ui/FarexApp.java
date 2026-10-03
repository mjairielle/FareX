package com.farex.ui;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;

/**
 * Entry point of the FareX app. Builds the window (phone-sized),
 * registers every screen with the Navigator, then shows the splash.
 */
public class FarexApp extends Application {

    @Override
    public void start(Stage stage) {
        StackPane root = new StackPane();
        root.setBackground(Theme.solid(Theme.DARK));
        Navigator.init(root);

        // Each screen is registered by name. Later parts add more lines here.
        Navigator.register("splash", SplashView::new);
        Navigator.register("welcome", WelcomeView::new);
        // Part 2 will add: "login", "register"

        Scene scene = new Scene(root, 360, 720);

        stage.setTitle("FareX");
        stage.setScene(scene);
        stage.setResizable(false);
        stage.show();

        Navigator.go("splash");
    }

    public static void main(String[] args) {
        launch(args);
    }
}