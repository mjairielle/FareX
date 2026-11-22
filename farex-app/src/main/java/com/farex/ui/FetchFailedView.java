package com.farex.ui;

// Fetch Load / 05: the card read stopped before the balance was updated.
// The load is still pending, so "Check fetch status" leads back to "Fetch your pending load".
public class FetchFailedView extends SubScreen {

    public FetchFailedView() {
        super("Fetch Load", null);   // no back arrow, like the Figma

        DemoCard card = FetchLoadState.getCard();
        PendingLoad load = FetchLoadState.getLastLoad();

        body.setSpacing(12);
        body.getChildren().addAll(
                FetchUi.resultHeader(WarningDialog.warningIcon(),
                        "Couldn’t fetch your load",
                        "The card connection was interrupted before the balance update was confirmed."),
                FetchUi.spacer(),
                FetchUi.panel(
                        FetchUi.row("Top-up", FetchUi.peso(load.getAmount())),
                        FetchUi.row("Card", FetchUi.masked(card)),
                        FetchUi.row("Status", "Not confirmed")),
                FetchUi.note("Your paid top-up is safe. Check its status first. If it is still pending, "
                        + "hold the same card steady and retry. Do not buy load again."),
                FetchUi.limeButton("Check fetch status", () -> Navigator.go("fetchCurrent")),
                FetchUi.link("Back to Home", FetchLoadState::goHome));
    }
}
