package com.farex.ui.auth;

import javafx.animation.PauseTransition;
import javafx.scene.layout.StackPane;
import javafx.util.Duration;

import com.farex.ui.common.Assets;
import com.farex.ui.common.Navigator;
import com.farex.ui.common.Theme;

public class SplashView extends StackPane {

    public SplashView() {
        setBackground(Theme.solid(Theme.LIME));
        getChildren().add(Assets.logo(96, 0));

        PauseTransition wait = new PauseTransition(Duration.seconds(1.5));
        wait.setOnFinished(e -> Navigator.go("welcome"));
        wait.play();
    }
}