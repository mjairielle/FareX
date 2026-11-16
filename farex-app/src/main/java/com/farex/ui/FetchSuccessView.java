package com.farex.ui;

import javafx.scene.control.Label;

// Fetch Load / 04: ₱200.00 added, ₱676.50 -> ₱876.50
public class FetchSuccessView extends SubScreen {

    public FetchSuccessView() {
        super("Fetch Load", null);   // no back arrow: the load is already on the card

        DemoCard card = FetchLoadState.getCard();
        PendingLoad load = FetchLoadState.getLastLoad();

        Label added = new Label(FetchUi.peso(load.getAmount()) + " added");
        added.setTextFill(Theme.LIME);
        added.setFont(Theme.bold(22));

        body.setSpacing(12);
        body.getChildren().addAll(
                FetchUi.resultHeader(FetchUi.doubleCheck(),
                        "Load fetched successfully",
                        "Your xPress card is updated and ready for your next trip."),
                FetchUi.spacer(),
                FetchUi.panel(
                        added,
                        FetchUi.row("Previous balance", FetchUi.peso(FetchLoadState.getPreviousBalance())),
                        FetchUi.row("New card balance", FetchUi.peso(card.getBalance())),
                        FetchUi.row("xPress card", FetchUi.masked(card)),
                        FetchUi.row("Reference", load.getReference())),
                FetchUi.note("This top-up is marked as collected. No additional payment was made."),
                FetchUi.limeButton("Back to Home", FetchLoadState::goHome),
                FetchUi.link("View transactions", () -> {
                    FetchLoadState.clear();
                    Navigator.go("transactions");
                }));
    }
}
