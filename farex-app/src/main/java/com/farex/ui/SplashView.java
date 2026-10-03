package com.farex.ui;

import javafx.animation.PauseTransition;
import javafx.scene.layout.StackPane;
import javafx.util.Duration;

/** Figma "Enter" screen: lime background, logo in the middle, then moves on to Welcome. */
public class SplashView extends StackPane {

    public SplashView() {
        setBackground(Theme.solid(Theme.LIME));
        getChildren().add(Assets.logo(96, 0));

        PauseTransition wait = new PauseTransition(Duration.seconds(1.5));
        wait.setOnFinished(e -> Navigator.go("welcome"));
        wait.play();
    }
}