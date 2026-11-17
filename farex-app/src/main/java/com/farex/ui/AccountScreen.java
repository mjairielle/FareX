package com.farex.ui;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;

// Account Information in one screen (from Profile, or "Edit" on the Operator Dashboard):
//   view (Profile / 01): name, email, phone, read-only, with an Edit button
//   -> edit (Profile / 02): the same fields become editable; "Change photo" opens the sheet (03)
// Save or Cancel goes back to view. Back arrow: edit -> view -> Profile (or Operator Dashboard).
public class AccountScreen extends StepScreen {

    private LabeledField nameField;
    private LabeledField emailField;
    private LabeledField phoneField;
    private final Label errorLabel = new Label();

    public AccountScreen() {
        super("Account Information", OperatorState.homeRoute());

        errorLabel.setTextFill(Theme.ERROR);
        errorLabel.setWrapText(true);
        errorLabel.setVisible(false);
        errorLabel.managedProperty().bind(errorLabel.visibleProperty());

        restartAt(this::viewStep);
    }

    @Override
    protected void back() {
        ProfileState.clearDraft();
        super.back();
    }

    // ----- Profile / 01: read-only -----

    private void viewStep() {
        DemoAccount account = MockData.getCurrentAccount();

        Label name = TripUi.bold(account.getFullName(), 15);
        VBox photo = new VBox(12, new Avatar(account, 80), name);
        photo.setAlignment(Pos.CENTER);
        VBox.setMargin(photo, new Insets(8, 0, 8, 0));

        Label privateNote = FetchUi.small("Only you can see your contact details.", Theme.WHITE);
        privateNote.setMaxWidth(Double.MAX_VALUE);
        privateNote.setAlignment(Pos.CENTER);

        body.getChildren().addAll(
                heading("Account information", "Your personal details, all in one place."),
                photo,
                new LabeledField("Full name", account.getFullName()),
                new LabeledField("Email address", orDash(account.getEmail())),
                new LabeledField("Phone number", orDash(account.getPhone())),
                FetchUi.spacer(),
                FetchUi.limeButton("Edit", () -> {
                    ProfileState.clearDraft();
                    showStep(this::editStep);
                }),
                privateNote);
    }

    // ----- Profile / 02: editable -----

    private void editStep() {
        DemoAccount account = MockData.getCurrentAccount();

        // Coming back from the photo sheet: keep what was typed
        boolean draft = ProfileState.hasDraft();
        nameField = LabeledField.editable("Full name", draft ? ProfileState.getDraftName() : account.getFullName());
        emailField = LabeledField.editable("Email address", draft ? ProfileState.getDraftEmail() : account.getEmail());
        phoneField = LabeledField.editable("Phone number", draft ? ProfileState.getDraftPhone() : account.getPhone());

        Label changePhoto = FetchUi.smallBold("Change photo", Theme.LIME);
        changePhoto.setCursor(Cursor.HAND);
        changePhoto.setOnMouseClicked(e -> {
            ProfileState.saveDraft(nameField.getText(), emailField.getText(), phoneField.getText());
            Navigator.showOverlay(new ChangePhotoSheet(getScene().getWindow(), this::redraw));
        });
        VBox photo = new VBox(10, new Avatar(account, 80), changePhoto);
        photo.setAlignment(Pos.CENTER);
        VBox.setMargin(photo, new Insets(8, 0, 8, 0));

        errorLabel.setVisible(false);

        body.getChildren().addAll(
                heading("Edit account information", "Keep your personal details up to date."),
                photo,
                nameField,
                emailField,
                phoneField,
                errorLabel,
                FetchUi.spacer(),
                FetchUi.limeButton("Save changes", this::save),
                FetchUi.link("Cancel", this::back));
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
            restartAt(this::viewStep);
        }
    }

    // ----- Helpers -----

    private void showError(String message) {
        errorLabel.setText(message);
        errorLabel.setVisible(true);
    }

    private VBox heading(String title, String subtitle) {
        return new VBox(6, FetchUi.title(title), FetchUi.subtitle(subtitle));
    }

    private String orDash(String value) {
        return value.isEmpty() ? "—" : value;
    }
}
