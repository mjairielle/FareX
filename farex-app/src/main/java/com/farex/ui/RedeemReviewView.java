package com.farex.ui;

import javafx.geometry.Insets;
import javafx.scene.layout.VBox;

// Redeem Points / 02: check points, credit and the card before confirming
public class RedeemReviewView extends SubScreen {

    public RedeemReviewView() {
        super("Redeem Points", "redeemChoose");

        DemoCard card = RedeemState.getCard();
        int points = RedeemState.getSelected();

        VBox heading = new VBox(8,
                FetchUi.title("Review your redemption"),
                FetchUi.subtitle("Check the points and destination before confirming."));
        VBox.setMargin(heading, new Insets(0, 0, 6, 0));

        // 100 pts on ₱676.50 -> ₱776.50 after fetch, 250 - 100 = 150 pts left
        VBox details = FetchUi.panel(
                FetchUi.row("Points to redeem", FetchUi.pts(points)),
                FetchUi.row("Transit credit", FetchUi.peso(points)),
                FetchUi.row("Destination", FetchUi.masked(card)),
                FetchUi.row("Current balance", FetchUi.peso(card.getBalance())),
                FetchUi.row("Balance after fetch", FetchUi.peso(card.getBalance() + points)),
                FetchUi.row("Points remaining", FetchUi.pts(RedeemState.getAvailable() - points)));

        body.setSpacing(12);
        body.getChildren().addAll(
                heading,
                details,
                FetchUi.note("Credit will be ready to fetch after redemption. Use Fetch Load to update "
                        + "your physical card. Confirmed redemptions cannot be reversed."),
                FetchUi.spacer(),
                FetchUi.limeButton("Confirm redemption", () -> Navigator.go("redeemProgress")),
                FetchUi.link("Edit amount", () -> Navigator.go("redeemChoose")));
    }
}
