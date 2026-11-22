package com.farex.ui;

// Lost Card / 05: the card is frozen; its ₱676.50 waits for a replacement card
public class LostReportedView extends SubScreen {

    public LostReportedView() {
        super("Replace or Report Lost Card", "dashboard");

        DemoCard card = LostCardState.getCard();

        body.setSpacing(12);
        body.getChildren().addAll(
                FetchUi.resultHeader(FetchUi.doubleCheck(),
                        "Lost card reported",
                        "The server has frozen your old card UID. Your lost card can no longer be used."),
                FetchUi.spacer(),
                FetchUi.panel(
                        FetchUi.row("Old card", FetchUi.masked(card)),
                        FetchUi.row("Server status", "Frozen"),
                        FetchUi.row("Protected balance", FetchUi.peso(card.getBalance())),
                        FetchUi.row("Report reference", card.getFreezeReference())),
                FetchUi.note("Your balance has not moved yet. Get a replacement card to transfer it, "
                        + "or return later from Card protection."),
                FetchUi.limeButton("Get replacement card", () -> Navigator.go("lostReplace")),
                FetchUi.link("Back to Home", LostCardState::goHome));
    }
}
