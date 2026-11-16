package com.farex.ui;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;

// Profile / 02: edit name, email and phone; "Change photo" opens the photo sheet (03)
public class AccountEditView extends SubScreen {

    private final LabeledField nameField;
    private final LabeledField emailField;
    private final LabeledField phoneField;
    private final Label errorLabel = new Label();

    public AccountEditView() {
        super("Account Information", "accountInfo");

        DemoAccount account = MockData.getCurrentAccount();

        // Coming back from the photo sheet: keep what was typed
        boolean draft = ProfileState.hasDraft();
        nameField = LabeledField.editable("Full name", draft ? ProfileState.getDraftName() : account.getFullName());
        emailField = LabeledField.editable("Email address", draft ? ProfileState.getDraftEmail() : account.getEmail());
        phoneField = LabeledField.editable("Phone number", draft ? ProfileState.getDraftPhone() : account.getPhone());

        VBox heading = new VBox(6,
                FetchUi.title("Edit account information"),
                FetchUi.subtitle("Keep your personal details up to date."));

        Label changePhoto = FetchUi.smallBold("Change photo", Theme.LIME);
        changePhoto.setCursor(Cursor.HAND);
        changePhoto.setOnMouseClicked(e -> {
            ProfileState.saveDraft(nameField.getText(), emailField.getText(), phoneField.getText());
            Navigator.showOverlay(new ChangePhotoSheet(getScene().getWindow()));
        });
        VBox photo = new VBox(10, new Avatar(account, 80), changePhoto);
        photo.setAlignment(Pos.CENTER);
        VBox.setMargin(photo, new Insets(8, 0, 8, 0));

        errorLabel.setTextFill(Theme.ERROR);
        errorLabel.setWrapText(true);
        errorLabel.setVisible(false);
        errorLabel.managedProperty().bind(errorLabel.visibleProperty());

        body.setSpacing(12);
        body.getChildren().addAll(
                heading,
                photo,
                nameField,
                emailField,
                phoneField,
                errorLabel,
                FetchUi.spacer(),
                FetchUi.limeButton("Save changes", this::save),
                FetchUi.link("Cancel", () -> {
                    ProfileState.clearDraft();
                    Navigator.go("accountInfo");
                }));
    }

    // Name needed; email like name@mail.com (or empty); phone like 09123456789 (or empty);
    // at least one way to reach the account
    private void save() {
        String name = nameField.getText();
        String email = emailField.getText();
        String phone = phoneField.getText();

        if (name.isEmpty()) {
            showError("Enter your full name.");
        } else if (!email.isEmpty() && !email.matches("[^@\\s]+@[^@\\s]+\\.[^@\\s]+")) {
            showError("Enter a valid email address, for example aron01@gmail.com.");
        } else if (!phone.isEmpty() && !phone.matches("09\\d{9}")) {
            showError("Enter an 11-digit number starting with 09, for example 09123456789.");
        } else if (email.isEmpty() && phone.isEmpty()) {
            showError("Keep at least an email address or a phone number.");
        } else {
            DemoAccount account = MockData.getCurrentAccount();
            account.setFullName(name);
            account.setEmail(email);
            account.setPhone(phone);
            ProfileState.clearDraft();
            Navigator.go("accountInfo");
        }
    }

    private void showError(String message) {
        errorLabel.setText(message);
        errorLabel.setVisible(true);
    }
}
