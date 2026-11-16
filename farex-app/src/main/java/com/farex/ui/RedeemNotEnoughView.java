package com.farex.ui;

// Redeem Points / 06: picked more points than the account has.
// Example: 150 available, 250 picked -> "Points needed: 100 pts"
public class RedeemNotEnoughView extends SubScreen {

    public RedeemNotEnoughView() {
        super("Redeem Points", "redeemChoose");

        int requested = RedeemState.getSelected();
        int available = RedeemState.getAvailable();

        body.setSpacing(12);
        body.getChildren().addAll(
                FetchUi.resultHeader(WarningDialog.warningIcon(),
                        "Not enough points",
                        "You need more earned points for the amount you selected."),
                FetchUi.spacer(),
                FetchUi.panel(
                        FetchUi.row("Requested", FetchUi.pts(requested)),
                        FetchUi.row("Available", FetchUi.pts(available)),
                        FetchUi.row("Points needed", FetchUi.pts(requested - available))),
                FetchUi.note("Choose " + available + " points or less. No points were deducted "
                        + "and no transit credit was issued."),
                FetchUi.limeButton("Choose a lower amount", () -> Navigator.go("redeemChoose")),
                FetchUi.link("Back to Home", RedeemState::goHome));
    }
}
