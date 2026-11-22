package com.farex.ui;

// Redeem Points / 04: points taken, credit waiting on the card for Fetch Load
public class RedeemSuccessView extends SubScreen {

    public RedeemSuccessView() {
        super("Redeem Points", "dashboard");

        DemoCard card = RedeemState.getCard();
        int points = RedeemState.getSelected();

        body.setSpacing(12);
        body.getChildren().addAll(
                FetchUi.resultHeader(FetchUi.doubleCheck(),
                        "Points redeemed!",
                        "Your transit credit is ready to fetch. Your physical card has not been updated yet."),
                FetchUi.spacer(),
                FetchUi.panel(
                        FetchUi.row("Points redeemed", FetchUi.pts(points)),
                        FetchUi.row("Points remaining", FetchUi.pts(RedeemState.getAvailable())),
                        FetchUi.row("Ready to fetch", FetchUi.peso(points)),
                        FetchUi.row("xPress card", FetchUi.masked(card)),
                        FetchUi.row("Reference", RedeemState.getLastReference())),
                FetchUi.note("Next: hold your card to your phone in Fetch Load to collect this credit."),
                FetchUi.limeButton("Back to Home", RedeemState::goHome),
                FetchUi.link("View Transaction", () -> {
                    RedeemState.clear();
                    Navigator.go("transactions");
                }));
    }
}
