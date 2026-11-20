package com.farex.ui.auth;

import javafx.scene.control.TextField;

import com.farex.ui.common.Navigator;
import com.farex.ui.common.PopupDialog;
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
        new PopupDialog("Jeepney plate", "Which jeepney do you drive? An admin approves new operators before the first login.")
                .add(plate)
                .button("Create Account", () -> send("OPERATOR", plate.getText().trim()))
                .show();
    }

    private static void send(String serverRole, String plate) {
        String error = MockData.registerOnServer(serverRole.isEmpty() ? "PASSENGER" : serverRole, plate);
        if (error == null) {
            Navigator.go("congrats");
        } else {
            new PopupDialog(WarningDialog.warningIcon(), "Couldn't create account", error)
                    .button("OK", () -> { })
                    .show();
        }
    }
}
