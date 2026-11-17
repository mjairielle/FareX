package com.farex.ui;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;

// Profile / 11: account removed and signed out (its login no longer works until the app restarts)
public class AccountDeletedView extends SubScreen {

    public AccountDeletedView() {
        super("Account Deleted", (String) null);   // no back arrow: there is no account to go back to

        VBox message = FetchUi.resultHeader(FetchUi.doubleCheck(), "Account deleted!",
                "Your FareX account has been permanently deleted. You’re now signed out.");
        VBox.setMargin(message, new Insets(110, 0, 0, 0));

        Label footer = FetchUi.small("Thank you for travelling with FareX.", Theme.WHITE);
        footer.setMaxWidth(Double.MAX_VALUE);
        footer.setAlignment(Pos.CENTER);

        body.setSpacing(12);
        body.getChildren().addAll(
                message,
                FetchUi.spacer(),
                FetchUi.limeButton("Log in / Sign up", () -> Navigator.go("welcome")),
                footer);
    }
}
