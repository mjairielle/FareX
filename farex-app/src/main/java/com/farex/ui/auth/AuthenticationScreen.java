package com.farex.ui.auth;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.effect.DropShadow;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;

import com.farex.ui.common.Assets;
import com.farex.ui.common.Theme;

public abstract class AuthenticationScreen extends VBox {

    private final VBox card = new VBox(16);

    // Red message under the fields. Hidden until showError(...) is called.
    protected final Label errorLabel = new Label();

    protected AuthenticationScreen() {
        // logo + name, takes all the space the card doesn't use
        Label name = new Label("FareX");
        name.setTextFill(Theme.BLACK);
        name.setFont(Theme.bold(40));

        VBox top = new VBox(8, Assets.logo(120, 0), name);
        top.setAlignment(Pos.CENTER);
        top.setBackground(Theme.solid(Theme.LIME));
        VBox.setVgrow(top, Priority.ALWAYS);

        // Error label starts hidden (and takes no space)
        errorLabel.setTextFill(Theme.ERROR);
        errorLabel.setWrapText(true);
        errorLabel.setVisible(false);
        errorLabel.managedProperty().bind(errorLabel.visibleProperty());

        // Dark card with rounded top corners and a soft shadow above it
        card.setPadding(new Insets(32, 28, 32, 28));
        card.setBackground(Theme.roundedTop(Theme.DARK, 28));
        DropShadow shadow = new DropShadow(30, Color.rgb(0, 0, 0, 0.6));
        shadow.setOffsetY(-8);
        card.setEffect(shadow);

        getChildren().addAll(top, card);
    }

    protected void addToCard(Node... nodes) {
        card.getChildren().addAll(nodes);
    }

    protected void showError(String message) {
        errorLabel.setText(message);
        errorLabel.setVisible(true);
    }
}
