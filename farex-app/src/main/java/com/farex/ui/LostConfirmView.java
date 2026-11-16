package com.farex.ui;

// Lost Card / 02: warn before freezing
public class LostConfirmView extends SubScreen {

    public LostConfirmView() {
        super("Replace or Report Lost Card", "lostChoose");

        DemoCard card = LostCardState.getCard();
        boolean lost = LostCardState.LOST.equals(LostCardState.getReason());

        body.setSpacing(12);
        body.getChildren().addAll(
                FetchUi.resultHeader(WarningDialog.warningIcon(),
                        lost ? "Freeze your lost card?" : "Freeze your damaged card?",
                        "After verification, we’ll ask the server to freeze the old card UID. "
                                + "It can no longer be used once confirmed."),
                FetchUi.spacer(),
                FetchUi.panel(
                        FetchUi.row("xPress card", FetchUi.masked(card)),
                        FetchUi.row("Reason", LostCardState.getReason()),
                        FetchUi.row("Ledger balance", FetchUi.peso(card.getBalance()))),
                FetchUi.note("Your balance stays in your account ledger until a replacement is verified. "
                        + "The old card stays blocked even if you find it later."),
                FetchUi.limeButton("Verify and freeze card", () -> Navigator.go("lostOtp")),
                FetchUi.link("Go back", () -> Navigator.go("lostChoose")));
    }
}
