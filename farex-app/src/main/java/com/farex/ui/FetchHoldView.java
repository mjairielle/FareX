package com.farex.ui;

import javafx.geometry.Insets;
import javafx.scene.layout.VBox;

// Fetch Load / 02: shows the card to hold against the phone
public class FetchHoldView extends SubScreen {

    public FetchHoldView() {
        super("Fetch Load", "fetchCurrent");

        DemoCard card = FetchLoadState.getCard();

        VBox heading = new VBox(8,
                FetchUi.title("Hold your card near your phone"),
                FetchUi.subtitle("Turn on NFC, then place your xPress card against the back of your phone."));
        VBox.setMargin(heading, new Insets(0, 0, 6, 0));

        body.setSpacing(12);
        body.getChildren().addAll(
                heading,
                new CardTile(card),
                FetchUi.note("Remove thick phone cases. Keep the card still until confirmed. "
                        + "No NFC? Use an authorized load-fetch terminal."),
                FetchUi.spacer(),
                // A laptop has no NFC, so this starts the fake card read
                FetchUi.limeButton("Start reading card", () -> Navigator.go("fetchProgress")),
                FetchUi.link("Choose another card", FetchLoadState::pickCard));
    }
}
