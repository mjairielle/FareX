package com.farex.ui.auth;

import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;

import com.farex.ui.common.IconField;
import com.farex.ui.common.Navigator;
import com.farex.ui.common.Theme;
import com.farex.ui.data.MockData;
import com.farex.ui.operator.OperatorState;

public class LoginView extends AuthenticationScreen {

    public LoginView() {
        IconField contactBox = IconField.text("Email/Contact Number");
        IconField passwordBox = IconField.password("Password");
        TextField contactField = contactBox.getField();
        TextField passwordField = passwordBox.getField();

        Button loginButton = new Button("Login");
        Theme.limeButton(loginButton);
        loginButton.setMaxWidth(Double.MAX_VALUE);

        loginButton.setOnAction(e -> {
            String contact = contactField.getText().trim();
            String password = passwordField.getText();

            if (contact.isEmpty() || password.isEmpty()) {
                showError("Enter your email/contact number and password.");
            } else if (!MockData.checkLogin(contact, password)) {
                showError("Wrong email/contact number or password.");
            } else {
                Navigator.go(OperatorState.afterLoginRoute());
            }
        });

        // Enter key: contact field -> password field -> login
        contactField.setOnAction(e -> passwordBox.focusField());
        passwordField.setOnAction(e -> loginButton.fire());

        Label question = Theme.bodyText("Didn't have an account yet?", Theme.GRAY);
        Label signUp = Theme.linkLabel("Sign up");
        signUp.setOnMouseClicked(e -> Navigator.go(OperatorState.registerRoute()));
        HBox bottom = new HBox(6, question, signUp);
        bottom.setAlignment(Pos.CENTER);

        addToCard(contactBox, passwordBox, errorLabel, loginButton, bottom);

        // Operator login only: "Back to Passenger Mode"
        if (OperatorState.isOperatorMode()) {
            Label backTo = Theme.bodyText("Back to", Theme.GRAY);
            Label passenger = Theme.linkLabel("Passenger Mode");
            passenger.setOnMouseClicked(e -> Navigator.go("profile"));
            HBox backRow = new HBox(6, backTo, passenger);
            backRow.setAlignment(Pos.CENTER);
            addToCard(backRow);
        }
    }
}