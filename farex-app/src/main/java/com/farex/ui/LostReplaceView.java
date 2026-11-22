package com.farex.ui;

// Lost Card / 07: the old card is frozen; continue once the new card is in hand
public class LostReplaceView extends SubScreen {

    public LostReplaceView() {
        super("Replace or Report Lost Card", "dashboard");

        DemoCard card = LostCardState.getCard();

        body.setSpacing(12);
        body.getChildren().addAll(
                FetchUi.title("Get a replacement card"),
                FetchUi.panel(
                        FetchUi.row("Lost card", FetchUi.masked(card)),
                        FetchUi.row("Old card status", "Frozen"),
                        FetchUi.row("Protected balance", FetchUi.peso(card.getBalance())),
                        FetchUi.row("Report reference", card.getFreezeReference())),
                FetchUi.note("Already received your new card? Continue to link it. "
                        + "Your balance stays protected until transfer is complete."),
                FetchUi.spacer(),
                FetchUi.limeButton("I have my replacement card", () -> Navigator.go("lostLink")));
    }
}
