package com.farex;

import javafx.geometry.Pos;

public class WelcomeScreen extends Screen {
    public WelcomeScreen() {
        setAlignment(Pos.CENTER);
        getChildren().addAll(heading("Your Fare,"), lime("Your Way", 28),
                primary("Get Started", () -> Nav.show(new LoginScreen())));
    }
}
