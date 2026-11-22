package com.farex.ui;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Group;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

// Profile / 10: last check before deleting the account
public class DeleteAccountView extends SubScreen {

    public DeleteAccountView() {
        super("Delete Account", "privacy");

        DemoAccount account = MockData.getCurrentAccount();
        DemoCard card = account.getPrimaryCard();

        VBox heading = new VBox(6,
                FetchUi.title("Delete account"),
                FetchUi.subtitle("Review what happens before you leave FareX."));
        VBox.setMargin(heading, new Insets(0, 0, 6, 0));

        // ----- Warning panel -----
        Group icon = WarningDialog.warningIcon();
        icon.setScaleX(0.55);
        icon.setScaleY(0.55);
        HBox warningTitle = new HBox(4, new Group(icon), TripUi.bold("Delete your account?", 14));
        warningTitle.setAlignment(Pos.CENTER_LEFT);
        VBox warning = FetchUi.panel(warningTitle,
                FetchUi.subtitle("This permanently deletes your FareX account. You won’t be able to "
                        + "sign in or recover your profile after deletion."));
        warning.setPadding(new Insets(14, 16, 18, 16));

        // ----- Balance panel: "₱ 676.50   Current balance" -----
        Label balance = new Label("₱ " + (card == null ? "0.00" : card.getBalanceText()));
        balance.setTextFill(Theme.LIME);
        balance.setFont(Theme.bold(20));
        HBox balanceRow = new HBox(28, balance, FetchUi.small("Current balance", Theme.WHITE));
        balanceRow.setAlignment(Pos.CENTER_LEFT);
        VBox balancePanel = FetchUi.panel(TripUi.bold("Review your xPress Card balance", 12), balanceRow);
        balancePanel.setPadding(new Insets(14, 16, 16, 16));

        body.setSpacing(12);
        body.getChildren().addAll(
                heading,
                warning,
                balancePanel,
                FetchUi.subtitle("Check your remaining balance and any pending transactions before "
                        + "deleting your account."),
                FetchUi.spacer(),
                TripUi.bold("This action cannot be undone.", 13),
                FetchUi.subtitle("Not ready to leave? Keep your account and update your privacy preferences instead."),
                FetchUi.limeButton("Delete my account", () -> {
                    MockData.deleteCurrentAccount();
                    LogoutDialog.clearFlows();
                    Navigator.go("accountDeleted");
                }),
                FetchUi.link("Keep my account", () -> Navigator.go("privacy")));
    }
}
