package com.farex.ui;

// Lost Card / 06: the freeze was not confirmed; the card may still work
public class LostFailedView extends SubScreen {

    public LostFailedView() {
        super("Replace or Report Lost Card", "dashboard");

        DemoCard card = LostCardState.getCard();

        body.setSpacing(12);
        body.getChildren().addAll(
                FetchUi.resultHeader(WarningDialog.warningIcon(),
                        "Report not confirmed",
                        "We couldn’t confirm the server freeze. Your card may still be active."),
                FetchUi.spacer(),
                FetchUi.panel(
                        FetchUi.row("xPress card", FetchUi.masked(card)),
                        FetchUi.row("Freeze status", "Not confirmed"),
                        FetchUi.row("Balance transfer", "Not started")),
                // Already verified with the code, so it retries the freeze directly
                FetchUi.limeButton("Try Again", () -> Navigator.go("lostProgress")));
    }
}
