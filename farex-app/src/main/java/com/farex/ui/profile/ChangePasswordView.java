package com.farex.ui.profile;

import javafx.geometry.Insets;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;

import com.farex.ui.common.CommonUi;
import com.farex.ui.common.LabeledField;
import com.farex.ui.common.Navigator;
import com.farex.ui.common.PopupDialog;
import com.farex.ui.common.SubScreen;
import com.farex.ui.common.Theme;
import com.farex.ui.data.DemoAccount;
import com.farex.Api;
import com.farex.ui.data.MockData;

// Profile / 05: current password, new password, confirm. Saving shows "Password updated!" (06) as a pop-up
public class ChangePasswordView extends SubScreen {

    private final LabeledField current = LabeledField.password("Current password");
    private final LabeledField fresh = LabeledField.password("New password");
    private final LabeledField confirm = LabeledField.password("Confirm new password");
    private final Label errorLabel = new Label();

    public ChangePasswordView() {
        super("Change Password", "privacy");

        VBox heading = new VBox(6,
                CommonUi.title("Change password"),
                CommonUi.subtitle("Choose a strong password to protect your account."));
        VBox.setMargin(heading, new Insets(0, 0, 6, 0));

        errorLabel.setTextFill(Theme.ERROR);
        errorLabel.setWrapText(true);
        errorLabel.setVisible(false);
        errorLabel.managedProperty().bind(errorLabel.visibleProperty());

        body.setSpacing(12);
        body.getChildren().addAll(
                heading,
                current,
                fresh,
                confirm,
                CommonUi.subtitle("Use at least 8 characters, including a number and a symbol. "
                        + "Don’t reuse your current password."),
                errorLabel,
                CommonUi.spacer(),
                CommonUi.limeButton("Save changes", this::save),
                CommonUi.link("Cancel", () -> Navigator.go("privacy")));
    }

    // Example that passes: current "farex123", new "Farex@2026", confirm "Farex@2026"
    private void save() {
        DemoAccount account = MockData.getCurrentAccount();
        String newPassword = fresh.getField().getText();
        String now = current.getField().getText();
        boolean live = MockData.isLive();   // live: the server checks the current password

        if (!live && !account.passwordMatches(now)) {
            showError("Your current password is not correct.");
        } else if (newPassword.length() < 8) {
            showError("The new password needs at least 8 characters.");
        } else if (!newPassword.matches(".*\\d.*")) {
            showError("Add at least one number, for example 2026.");
        } else if (!newPassword.matches(".*[^A-Za-z0-9].*")) {
            showError("Add at least one symbol, for example @ or #.");
        } else if (live ? newPassword.equals(now) : account.passwordMatches(newPassword)) {
            showError("Choose a password different from your current one.");
        } else if (!newPassword.equals(confirm.getField().getText())) {
            showError("The new passwords don’t match.");
        } else if (live) {
            try {
                Api.changePassword(now, newPassword);
                showChanged();
            } catch (Api.ApiException e) {
                showError(e.getMessage());
            }
        } else {
            account.setPassword(newPassword);
            showChanged();
        }
    }

    // Profile / 06 (pop-up): password saved
    private void showChanged() {
        new PopupDialog(CommonUi.doubleCheck(), "Password updated!",
                "Your password has been changed successfully. Use your new password the next time you log in.")
                .add(CommonUi.small("You’re still signed in to FareX.", Theme.WHITE))
                .button("Back to Privacy & Security", () -> Navigator.go("privacy"))
                .show();
    }

    private void showError(String message) {
        errorLabel.setText(message);
        errorLabel.setVisible(true);
    }
}
