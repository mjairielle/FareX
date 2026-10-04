package com.farex.ui;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;

public class FarexApp extends Application {

    @Override
    public void start(Stage stage) {
        StackPane root = new StackPane();
        root.setBackground(Theme.solid(Theme.DARK));
        Navigator.init(root);

        Navigator.register("splash", SplashView::new);
        Navigator.register("welcome", WelcomeView::new);
        Navigator.register("login", LoginView::new);
        Navigator.register("register", RegisterView::new);
        Navigator.register("otp", OtpView::new);
        Navigator.register("accountType", AccountTypeView::new);
        Navigator.register("congrats", CongratsView::new);

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