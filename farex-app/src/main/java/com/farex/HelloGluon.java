package com.farex;

import javafx.application.Application;
import javafx.stage.Stage;

public class HelloGluon extends Application {
    @Override
    public void start(Stage stage) {
        Nav.init(stage);
        Nav.show(new WelcomeScreen());
        stage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
