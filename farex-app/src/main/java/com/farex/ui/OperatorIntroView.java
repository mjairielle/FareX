package com.farex.ui;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Group;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import javafx.scene.text.TextAlignment;

// Operator / E1: "Switch to Operator Mode?" (opened by OPERATOR'S MODE on the Profile tab)
public class OperatorIntroView extends SubScreen {

    public OperatorIntroView() {
        super("Operator Mode", "profile");

        Group wheel = TripUi.steeringWheel();
        wheel.setScaleX(1.1);
        wheel.setScaleY(1.1);

        Label title = TripUi.bold("Switch to\nOperator Mode?", 24);
        title.setTextAlignment(TextAlignment.CENTER);
        title.setWrapText(true);

        Label subtitle = FetchUi.subtitle("Collect fares and manage your\njeepney shift.");
        subtitle.setTextAlignment(TextAlignment.CENTER);

        VBox top = new VBox(10, wheel, title, subtitle);
        top.setAlignment(Pos.TOP_CENTER);
        VBox.setMargin(top, new Insets(14, 0, 18, 0));

        body.getChildren().addAll(
                top,
                infoPanel(),
                FetchUi.spacer(),
                FetchUi.limeButton("Continue to operator login", () -> Navigator.go("operatorLogin")),
                FetchUi.link("Stay in Passenger mode", () -> Navigator.go("profile")));
    }

    // ----- "Made for your route" panel -----

    private VBox infoPanel() {
        Label accept = FetchUi.small("Accept xPress Cards and app QR taps.\nKeep this phone open during your shift.",
                Theme.WHITE);
        accept.setWrapText(true);
        Label signIn = FetchUi.small("Sign in with your operator account to continue.", Theme.WHITE);
        signIn.setWrapText(true);

        VBox panel = FetchUi.panel(TripUi.bold("Made for your route", 13), accept, signIn);
        panel.setSpacing(8);
        panel.setPadding(new Insets(16, 14, 16, 14));
        return panel;
    }
}
