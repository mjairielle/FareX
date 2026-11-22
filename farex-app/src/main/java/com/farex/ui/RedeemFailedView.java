package com.farex.ui;

// Redeem Points / 05: the redemption did not go through; no points were taken
public class RedeemFailedView extends SubScreen {

    public RedeemFailedView() {
        super("Redeem Points", "dashboard");

        body.setSpacing(12);
        body.getChildren().addAll(
                FetchUi.resultHeader(WarningDialog.warningIcon(),
                        "Couldn’t redeem points",
                        "The service could not complete this redemption. Your points have not been deducted."),
                FetchUi.spacer(),
                FetchUi.panel(
                        FetchUi.row("Requested", FetchUi.pts(RedeemState.getSelected())),
                        FetchUi.row("Available balance", FetchUi.pts(RedeemState.getAvailable())),
                        FetchUi.row("Transit credit issued", FetchUi.peso(0))),
                FetchUi.note("Check your connection and try again. If a request is still pending, "
                        + "check its status before resubmitting."),
                FetchUi.limeButton("Try again", () -> Navigator.go("redeemReview")));
    }
}
