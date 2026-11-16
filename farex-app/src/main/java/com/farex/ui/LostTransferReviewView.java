package com.farex.ui;

import javafx.geometry.Insets;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;

// Lost Card / 09: ₱676.50 goes from •••• 6767 (frozen) to •••• 8842 (new)
public class LostTransferReviewView extends SubScreen {

    public LostTransferReviewView() {
        super("Replace or Report Lost Card", "lostLink");

        DemoCard card = LostCardState.getCard();
        double balance = card.getBalance();

        VBox heading = new VBox(8,
                FetchUi.title("Move your protected balance"),
                FetchUi.subtitle("The server will move the remaining ledger balance from the frozen UID "
                        + "to your verified new UID."));
        VBox.setMargin(heading, new Insets(0, 0, 6, 0));

        Label amount = new Label(FetchUi.peso(balance));
        amount.setTextFill(Theme.LIME);
        amount.setFont(Theme.bold(26));

        VBox details = FetchUi.panel(
                FetchUi.row("From frozen card", FetchUi.masked(card)),
                FetchUi.row("To replacement", LostCardState.maskedNewCard()),
                amount,
                FetchUi.small("Server ledger balance to transfer", FetchUi.SOFT_TEXT),
                FetchUi.row("New balance after transfer", FetchUi.peso(balance)),
                FetchUi.row("Old balance after transfer", FetchUi.peso(0)));

        body.setSpacing(12);
        body.getChildren().addAll(
                heading,
                details,
                FetchUi.note("The old card remains permanently blocked. Confirm only if the replacement card "
                        + "is yours. This transfer does not charge your payment account."),
                FetchUi.spacer(),
                FetchUi.limeButton("Confirm balance transfer", () -> Navigator.go("lostTransfer")),
                FetchUi.link("Change replacement card", () -> Navigator.go("lostLink")));
    }
}
