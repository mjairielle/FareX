package com.farex.ui;

// Lost Card / 12: the transfer was not confirmed; the balance is still on the frozen card
public class LostTransferFailedView extends SubScreen {

    public LostTransferFailedView() {
        super("Replace or Report Lost Card", (String) null);   // no back arrow, like the Figma

        DemoCard card = LostCardState.getCard();

        body.setSpacing(12);
        body.getChildren().addAll(
                FetchUi.resultHeader(WarningDialog.warningIcon(),
                        "Transfer not confirmed",
                        "The server did not confirm the balance transfer. Your old card is still frozen."),
                FetchUi.spacer(),
                FetchUi.panel(
                        FetchUi.row("Source UID", FetchUi.masked(card)),
                        FetchUi.row("Replacement UID", LostCardState.maskedNewCard()),
                        FetchUi.row("Amount requested", FetchUi.peso(card.getBalance())),
                        FetchUi.row("Transfer status", "Not confirmed")),
                FetchUi.note("Your balance remains protected in the account ledger. Check the status before "
                        + "retrying. Do not request another replacement card."),
                FetchUi.limeButton("Check transfer status", () -> Navigator.go("lostTransferReview")));
    }
}
