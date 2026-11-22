package com.farex.ui.auth;

import javafx.scene.control.TextField;

import com.farex.ui.common.Navigator;
import com.farex.ui.common.PopupDialog;
import com.farex.ui.common.RoutePicker;
import com.farex.ui.common.Theme;
import com.farex.ui.common.WarningDialog;
import com.farex.ui.data.MockData;

/** Remembers what was picked during sign-up, and sends the new account to the server. */
public final class SignUpState {

    private static String role = "Passenger";

    private SignUpState() {
    }

    public static String getRole() {
        return role;
    }

    public static void setRole(String newRole) {
        role = newRole;
    }

    /**
     * Last sign-up step: "Passenger" goes straight to the server, "Operator" first asks the
     * jeepney plate (the server needs it). Success shows Congrats, a refusal shows why.
     */
    public static void createAccount(String newRole) {
        role = newRole;
        if (!"Operator".equals(newRole)) {
            send("", "");
            return;
        }
        TextField plate = new TextField();
        Theme.inputField(plate, "e.g. ABC-123");
        RoutePicker route = new RoutePicker();
        new PopupDialog("Jeepney plate and route", "Which jeepney do you drive, and on which route? "
                + "An admin approves new operators before the first login.")
                .add(plate, route)
                .button("Create Account", () -> {
                    if (route.hasRoutes() && route.routeId().isEmpty()) {
                        failed("Choose the route your jeepney runs.");
                    } else {
                        send("OPERATOR", plate.getText().trim(), route.routeId());
                    }
                })
                .show();
    }

    private static void send(String serverRole, String plate) {
        send(serverRole, plate, "");
    }

    private static void send(String serverRole, String plate, String route) {
        String error = MockData.registerOnServer(serverRole.isEmpty() ? "PASSENGER" : serverRole, plate, route);
        if (error == null) {
            Navigator.go("congrats");
        } else {
            failed(error);
        }
    }

    private static void failed(String why) {
        new PopupDialog(WarningDialog.warningIcon(), "Couldn't create account", why)
                .button("OK", () -> { })
                .show();
    }
}
