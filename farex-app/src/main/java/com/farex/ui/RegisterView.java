package com.farex.ui;

import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;

/** Figma "Registration" screen: contact, password, confirm password. */
public class RegisterView extends AuthenticationScreen {

    public RegisterView() {
        TextField contactField = new TextField();
        Theme.inputField(contactField, "Email/Contact Number");

        PasswordField passwordField = new PasswordField();
        Theme.inputField(passwordField, "Password");

        PasswordField confirmField = new PasswordField();
        Theme.inputField(confirmField, "Confirm Password");

        Button signUpButton = new Button("Sign up");
        Theme.limeButton(signUpButton);
        signUpButton.setMaxWidth(Double.MAX_VALUE);

        signUpButton.setOnAction(e -> {
            String contact = contactField.getText().trim();
            String password = passwordField.getText();
            String confirm = confirmField.getText();

            if (contact.isEmpty() || password.isEmpty() || confirm.isEmpty()) {
                showError("Please fill in every field.");
            } else if (!password.equals(confirm)) {
                showError("Passwords do not match.");   // e.g. farex123 vs farex12
            } else {
                MockData.setPendingContact(contact);     // the OTP screen shows this
                Navigator.go("otp");                     // built in Part 3
            }
        });

        // Enter key moves down the form, then submits
        contactField.setOnAction(e -> passwordField.requestFocus());
        passwordField.setOnAction(e -> confirmField.requestFocus());
        confirmField.setOnAction(e -> signUpButton.fire());

        Label question = Theme.bodyText("Already have an account?", Theme.GRAY);
        Label logIn = Theme.linkLabel("Log in");
        logIn.setOnMouseClicked(e -> Navigator.go("login"));
        HBox bottom = new HBox(6, question, logIn);
        bottom.setAlignment(Pos.CENTER);

        addToCard(contactField, passwordField, confirmField, errorLabel, signUpButton, bottom);
    }
}
