package com.farex.ui.operator;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Group;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;
import javafx.scene.text.TextAlignment;

import com.farex.ui.common.CommonUi;
import com.farex.ui.common.RoutePicker;
import com.farex.ui.common.Navigator;
import com.farex.ui.common.SubScreen;
import com.farex.ui.common.Theme;
import com.farex.ui.data.MockData;
import com.farex.ui.trip.TripUi;

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

        Label subtitle = CommonUi.subtitle("Collect fares and manage your\njeepney shift.");
        subtitle.setTextAlignment(TextAlignment.CENTER);

        VBox top = new VBox(10, wheel, title, subtitle);
        top.setAlignment(Pos.TOP_CENTER);
        VBox.setMargin(top, new Insets(14, 0, 18, 0));

        body.getChildren().addAll(top, infoPanel());
        // Same login, no second password: the account switches role, waits for approval, or asks to become an operator
        if (MockData.isPending("OPERATOR")) {
            body.getChildren().addAll(
                    CommonUi.note("Your operator request is waiting for admin approval. You can switch once it is approved."),
                    CommonUi.spacer());
        } else if (MockData.hasRole("OPERATOR")) {
            body.getChildren().addAll(CommonUi.spacer(),
                    CommonUi.limeButton("Switch to Operator Mode", () -> OperatorState.switchTo("OPERATOR")));
        } else {
            TextField plate = new TextField();
            Theme.inputField(plate, "Jeepney plate, e.g. ABC-123");
            RoutePicker route = new RoutePicker();
            Label error = CommonUi.small("", Theme.ERROR);
            error.setWrapText(true);
            body.getChildren().addAll(
                    CommonUi.note("This account is for riding only. Add your jeepney and its route to become an operator. "
                            + "An admin approves it before you can switch."),
                    plate, route, error, CommonUi.spacer(),
                    CommonUi.limeButton("Become an operator", () -> {
                        String why = plate.getText().isBlank() ? "Enter your jeepney plate."
                                : route.hasRoutes() && route.routeId().isEmpty() ? "Choose the route your jeepney runs."
                                : MockData.addRole("OPERATOR", plate.getText().trim(), route.routeId());
                        if (why != null) {
                            error.setText(why);
                        } else {
                            Navigator.go("operatorMode");   // shows "waiting for admin approval"
                        }
                    }));
        }
        body.getChildren().add(CommonUi.link("Stay in Passenger mode", () -> Navigator.go("profile")));
    }

    // ----- "Made for your route" panel -----

    private VBox infoPanel() {
        Label accept = CommonUi.small("Accept xPress Cards and app QR taps.\nKeep this phone open during your shift.",
                Theme.WHITE);
        accept.setWrapText(true);
        Label signIn = CommonUi.small("Uses the same FareX login. Switch back to Passenger anytime.", Theme.WHITE);
        signIn.setWrapText(true);

        VBox panel = CommonUi.panel(TripUi.bold("Made for your route", 13), accept, signIn);
        panel.setSpacing(8);
        panel.setPadding(new Insets(16, 14, 16, 14));
        return panel;
    }
}
