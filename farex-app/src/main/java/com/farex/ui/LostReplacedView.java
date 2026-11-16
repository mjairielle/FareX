package com.farex.ui;

// Lost Card / 11: the new card is active with the old balance; the old card is permanently frozen
public class LostReplacedView extends SubScreen {

    public LostReplacedView() {
        super("Replace or Report Lost Card", null);   // no back arrow: the transfer is done

        DemoCard oldCard = LostCardState.getCard();
        DemoCard newCard = LostCardState.getReplacement();

        body.setSpacing(12);
        body.getChildren().addAll(
                FetchUi.resultHeader(FetchUi.doubleCheck(),
                        "Replacement complete",
                        "The server transferred your ledger balance. Your new card is active."),
                FetchUi.spacer(),
                FetchUi.panel(
                        FetchUi.row("New card UID", FetchUi.masked(newCard)),
                        FetchUi.row("New card balance", FetchUi.peso(newCard.getBalance())),
                        FetchUi.row("Old card UID", FetchUi.masked(oldCard)),
                        FetchUi.row("Old card balance", FetchUi.peso(oldCard.getBalance())),
                        FetchUi.row("Old card status", "Permanently frozen"),
                        FetchUi.row("Transfer reference", LostCardState.getTransferReference())),
                FetchUi.note("The old UID can no longer be used. Your account now uses the replacement card."),
                FetchUi.limeButton("View replacement card", () -> {
                    LostCardState.clear();
                    Navigator.go("allCards");
                }),
                FetchUi.link("Back to Home", LostCardState::goHome));
    }
}
