package com.farex.ui;

import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;

// Profile / 06: password saved
public class PasswordChangedView extends SubScreen {

    public PasswordChangedView() {
        super("Change Password", "privacy");

        VBox heading = new VBox(6,
                FetchUi.title("Password changed"),
                FetchUi.subtitle("Your account security is up to date."));

        Label footer = FetchUi.small("You’re still signed in to FareX.", Theme.WHITE);
        footer.setMaxWidth(Double.MAX_VALUE);
        footer.setAlignment(Pos.CENTER);

        body.setSpacing(12);
        body.getChildren().addAll(
                heading,
                TripUi.centerMessage(FetchUi.doubleCheck(), "Password updated!",
                        "Your password has been changed successfully. Use your new password "
                                + "the next time you log in.", null),
                FetchUi.spacer(),
                FetchUi.limeButton("Back to Privacy & Security", () -> Navigator.go("privacy")),
                footer);
    }
}
