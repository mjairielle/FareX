package com.farex;

import farex.shared.user.User;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.layout.Pane;
import javafx.stage.Stage;

public class Nav {
    private static Stage stage;

    public static void init(Stage s) {
        stage = s;
        stage.setTitle("FareX");
        stage.setScene(new Scene(new Pane(), 360, 640));
    }

    public static void show(Parent screen) {
        stage.getScene().setRoot(screen);
    }

    /** Parent type User, child decides the screen. No if on role here. */
    public static Parent home(User u) {
        return switch (u.homeScreen()) {
            case "PassengerHome" -> new PassengerHome();
            case "OperatorShift" -> new OperatorScreen();
            default -> new AdminScreen();
        };
    }
}
