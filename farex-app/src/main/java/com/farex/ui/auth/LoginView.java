package com.farex.ui.auth;

import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;

import com.farex.Api;
import com.farex.ui.common.IconField;
import com.farex.ui.common.Loading;
import com.farex.ui.common.PopupDialog;
import com.farex.ui.common.Navigator;
import com.farex.ui.common.Theme;
import com.farex.ui.data.MockData;
import com.farex.ui.operator.OperatorState;

public class LoginView extends AuthenticationScreen {

    public LoginView() {
        IconField contactBox = IconField.text("Email");
        IconField passwordBox = IconField.password("Password");
        TextField contactField = contactBox.getField();
        TextField passwordField = passwordBox.getField();

        Button loginButton = new Button("Login");
        Theme.limeButton(loginButton);
        loginButton.setMaxWidth(Double.MAX_VALUE);

        // A phone cannot reach the laptop's "localhost": let the user type the laptop's address
        // shortcut: kept until the app closes, save it with Gluon Storage once phones test daily
        Label server = Theme.linkLabel("Server: " + Api.base);
        server.setOnMouseClicked(e -> {
            TextField address = new TextField(Api.base);
            Theme.inputField(address, "http://192.168.1.5:8080");
            new PopupDialog("Server address", "The FareX server this app talks to, e.g. your laptop's Wi-Fi IP and port 8080.")
                    .add(address)
                    .button("Save", () -> {
                        String typed = address.getText().trim().replaceAll("/+$", "");
                        if (!typed.isEmpty()) {
                            Api.base = typed.startsWith("http") ? typed : "http://" + typed;
                        }
                        server.setText("Server: " + Api.base);
                    })
                    .link("Cancel", () -> { })
                    .show();
        });
        HBox serverRow = new HBox(server);
        serverRow.setAlignment(Pos.CENTER);
        serverRow.setVisible(false);                                 // only after "Cannot reach server"
        serverRow.managedProperty().bind(serverRow.visibleProperty());

        loginButton.setOnAction(e -> {
            String contact = contactField.getText().trim();
            String password = passwordField.getText();

            if (contact.isEmpty() || password.isEmpty()) {
                showError("Enter your email and password.");
                return;
            }
            Loading.run("Logging in…", () -> MockData.login(contact, password), error -> {
                if (error != null) {
                    showError(error);
                    serverRow.setVisible(error.startsWith("Cannot reach"));
                } else if (MockData.hasBothRoles()) {
                    Navigator.go("chooseRole");                         // "Log in as" Passenger or Operator
                } else {
                    OperatorState.openRole(MockData.currentRole());     // the server knows the account's role
                }
            });
        });

        // Enter key: contact field -> password field -> login
        contactField.setOnAction(e -> passwordBox.focusField());
        passwordField.setOnAction(e -> loginButton.fire());

        Label question = Theme.bodyText("Didn't have an account yet?", Theme.GRAY);
        Label signUp = Theme.linkLabel("Sign up");
        signUp.setOnMouseClicked(e -> Navigator.go("register"));
        HBox bottom = new HBox(6, question, signUp);
        bottom.setAlignment(Pos.CENTER);

        addToCard(contactBox, passwordBox, errorLabel, loginButton, bottom, serverRow);
    }
}