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

public class RegisterView extends AuthenticationScreen {

    public RegisterView() {
        IconField contactBox = IconField.text("Email");
        IconField passwordBox = IconField.password("Password");
        IconField confirmBox = IconField.password("Confirm Password");
        TextField contactField = contactBox.getField();
        TextField passwordField = passwordBox.getField();
        TextField confirmField = confirmBox.getField();

        Button signUpButton = new Button("Sign up");
        Theme.limeButton(signUpButton);
        signUpButton.setMaxWidth(Double.MAX_VALUE);

        signUpButton.setOnAction(e -> {
            String contact = contactField.getText().trim();
            String password = passwordField.getText();
            String confirm = confirmField.getText();

            if (contact.isEmpty() || password.isEmpty() || confirm.isEmpty()) {
                showError("Please fill in every field.");
            } else if (!contact.matches("[^\\s,@]+@[^\\s,@]+")) {
                showError("Enter a valid email, e.g. juan@mail.com.");   // the server logs in by email
            } else if (password.length() < 8) {
                showError("Password needs 8 or more characters.");
            } else if (!password.matches(".*\\d.*")) {
                showError("Add at least one number, for example 2026.");   // same rules as Change Password
            } else if (!password.matches(".*[^A-Za-z0-9].*")) {
                showError("Add at least one symbol, for example @ or #.");
            } else if (!password.equals(confirm)) {
                showError("Passwords do not match.");   // e.g. farex123 vs farex12
            } else {
                MockData.setPendingContact(contact);     // the OTP screen shows this
                MockData.setPendingPassword(password);   // sent to the server after Account Type
                Navigator.go("otp");
            }
        });

        // Enter key moves down the form, then submits
        contactField.setOnAction(e -> passwordBox.focusField());
        passwordField.setOnAction(e -> confirmBox.focusField());
        confirmField.setOnAction(e -> signUpButton.fire());

        Label question = Theme.bodyText("Already have an account?", Theme.GRAY);
        Label logIn = Theme.linkLabel("Log in");
        logIn.setOnMouseClicked(e -> Navigator.go("login"));
        HBox bottom = new HBox(6, question, logIn);
        bottom.setAlignment(Pos.CENTER);

        addToCard(contactBox, passwordBox, confirmBox, errorLabel, signUpButton, bottom);
    }
}