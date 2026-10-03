package com.farex.ui;

import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;

/** Figma "Login" screen. Demo account: aron01@gmail.com / farex123 */
public class LoginView extends AuthenticationScreen {

    public LoginView() {
        TextField contactField = new TextField();
        Theme.inputField(contactField, "Email/Contact Number");

        PasswordField passwordField = new PasswordField();
        Theme.inputField(passwordField, "Password");

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
                Navigator.go("dashboard");   // built in Part 4
            }
        });

        // Enter key: contact field -> password field -> login
        contactField.setOnAction(e -> passwordField.requestFocus());
        passwordField.setOnAction(e -> loginButton.fire());

        Label question = Theme.bodyText("Didn't have an account yet?", Theme.GRAY);
        Label signUp = Theme.linkLabel("Sign up");
        signUp.setOnMouseClicked(e -> Navigator.go("register"));
        HBox bottom = new HBox(6, question, signUp);
        bottom.setAlignment(Pos.CENTER);

        addToCard(contactField, passwordField, errorLabel, loginButton, bottom);
    }
}
