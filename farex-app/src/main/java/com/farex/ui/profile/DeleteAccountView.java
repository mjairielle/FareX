package com.farex.ui.profile;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Group;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

import javafx.scene.control.PasswordField;

import com.farex.ui.common.Loading;
import com.farex.ui.common.CommonUi;
import com.farex.ui.common.Navigator;
import com.farex.ui.common.PopupDialog;
import com.farex.ui.common.SubScreen;
import com.farex.ui.common.Theme;
import com.farex.ui.common.WarningDialog;
import com.farex.ui.data.DemoAccount;
import com.farex.ui.data.DemoCard;
import com.farex.ui.data.MockData;
import com.farex.ui.home.LogoutDialog;
import com.farex.ui.trip.TripUi;

// Profile / 10: last check before deleting the account. Deleting shows "Account deleted!" (11) as a pop-up
public class DeleteAccountView extends SubScreen {

    public DeleteAccountView() {
        super("Delete Account", "privacy");

        DemoAccount account = MockData.getCurrentAccount();
        DemoCard card = account.getPrimaryCard();

        VBox heading = new VBox(6,
                CommonUi.title("Delete account"),
                CommonUi.subtitle("Review what happens before you leave FareX."));
        VBox.setMargin(heading, new Insets(0, 0, 6, 0));

        // ----- Warning panel -----
        Group icon = WarningDialog.warningIcon();
        icon.setScaleX(0.55);
        icon.setScaleY(0.55);
        HBox warningTitle = new HBox(4, new Group(icon), TripUi.bold("Delete your account?", 14));
        warningTitle.setAlignment(Pos.CENTER_LEFT);
        VBox warning = CommonUi.panel(warningTitle,
                CommonUi.subtitle("This permanently deletes your FareX account. You won’t be able to "
                        + "sign in or recover your profile after deletion."));
        warning.setPadding(new Insets(14, 16, 18, 16));

        // ----- Balance panel: "₱ 676.50   Current balance" -----
        Label balance = new Label("₱ " + (card == null ? "0.00" : card.getBalanceText()));
        balance.setTextFill(Theme.LIME);
        balance.setFont(Theme.bold(20));
        HBox balanceRow = new HBox(28, balance, CommonUi.small("Current balance", Theme.WHITE));
        balanceRow.setAlignment(Pos.CENTER_LEFT);
        VBox balancePanel = CommonUi.panel(TripUi.bold("Review your xPress Card balance", 12), balanceRow);
        balancePanel.setPadding(new Insets(14, 16, 16, 16));

        body.setSpacing(12);
        body.getChildren().addAll(
                heading,
                warning,
                balancePanel,
                CommonUi.subtitle("Check your remaining balance and any pending transactions before "
                        + "deleting your account."),
                CommonUi.spacer(),
                TripUi.bold("This action cannot be undone.", 13),
                CommonUi.subtitle("Not ready to leave? Keep your account and update your privacy preferences instead."),
                CommonUi.limeButton("Delete my account", this::confirmPassword),
                CommonUi.link("Keep my account", () -> Navigator.go("privacy")));
    }

    // Last check: the password, so a phone left unlocked cannot delete the account
    private void confirmPassword() {
        PasswordField password = new PasswordField();
        Theme.inputField(password, "Password");
        Label error = CommonUi.small("", Theme.ERROR);
        error.setWrapText(true);
        new PopupDialog(WarningDialog.warningIcon(), "Enter your password",
                "Your account, both passenger and operator sides, is deleted. Any card balance is removed and cannot be refunded.")
                .add(password, error)
                .button("Delete my account", () -> Loading.run("Deleting your account…",
                        () -> MockData.deleteCurrentAccount(password.getText()), why -> {
                            if (why != null) {
                                new PopupDialog(WarningDialog.warningIcon(), "Account not deleted", why)
                                        .button("OK", () -> { })
                                        .show();
                                return;
                            }
                            LogoutDialog.clearFlows();
                            showDeleted();
                        }))
                .link("Keep my account", () -> { })
                .show();
    }

    // Profile / 11 (pop-up): account removed and signed out (its login no longer works until restart).
    // The only way out is the button: there is no account to go back to.
    private void showDeleted() {
        new PopupDialog(CommonUi.doubleCheck(), "Account deleted!",
                "Your FareX account has been permanently deleted. You’re now signed out.")
                .add(CommonUi.small("Thank you for travelling with FareX.", Theme.WHITE))
                .button("Log in / Sign up", () -> Navigator.go("welcome"))
                .show();
    }
}
