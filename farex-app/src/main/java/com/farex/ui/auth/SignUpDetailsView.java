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

// Sign up, after the OTP: the account's name and phone, then Account Type creates the account
public class SignUpDetailsView extends AuthenticationScreen {

    public SignUpDetailsView() {
        Label title = Theme.bodyText("Your details", Theme.WHITE);
        title.setFont(Theme.bold(20));
        Label hint = Theme.bodyText("This name shows on your cards and receipts.", Theme.GRAY);
        hint.setWrapText(true);

        IconField nameBox = IconField.text("Full name");
        IconField phoneBox = IconField.text("Phone number (optional)");
        TextField nameField = nameBox.getField();
        TextField phoneField = phoneBox.getField();

        Button next = new Button("Continue");
        Theme.limeButton(next);
        next.setMaxWidth(Double.MAX_VALUE);
        next.setOnAction(e -> {
            String name = nameField.getText().trim();
            String phone = phoneField.getText().trim();
            if (name.isEmpty()) {
                showError("Enter your full name.");
            } else if (!phone.isEmpty() && !phone.matches("09\\d{9}")) {
                showError("Enter an 11-digit number starting with 09, or leave it empty.");
            } else {
                MockData.setPendingProfile(name, phone);
                Navigator.go("accountType");
            }
        });
        nameField.setOnAction(e -> phoneBox.focusField());
        phoneField.setOnAction(e -> next.fire());

        Label back = Theme.linkLabel("Back");
        back.setOnMouseClicked(e -> Navigator.go("register"));
        HBox bottom = new HBox(back);
        bottom.setAlignment(Pos.CENTER);

        addToCard(title, hint, nameBox, phoneBox, errorLabel, next, bottom);
    }
}
