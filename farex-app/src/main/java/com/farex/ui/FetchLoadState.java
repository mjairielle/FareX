package com.farex.ui;

import javafx.scene.Parent;

// Remembers which card Fetch Load is working on, and picks the right screen for it:
// a pending load -> "Fetch your pending load", none -> "No pending load"
public final class FetchLoadState {

    private static DemoCard card;

    private FetchLoadState() {
    }

    // The chosen card, or the account's first card if none was chosen yet
    public static DemoCard getCard() {
        if (card == null) {
            DemoAccount account = MockData.getCurrentAccount();
            card = (account == null) ? null : account.getPrimaryCard();
        }
        return card;
    }

    public static void setCard(DemoCard newCard) {
        card = newCard;
    }

    public static void clear() {
        card = null;
    }

    // ----- Screens -----

    // From Home: always start again with the first card
    public static Parent start() {
        clear();
        return currentScreen();
    }

    public static Parent currentScreen() {
        DemoCard current = getCard();
        if (current != null && current.hasPendingLoad()) {
            return new FetchSelectView();
        }
        return new FetchNoneView();
    }

    // "Change" / "Choose another card": pick a card, then show its screen
    public static void pickCard() {
        Navigator.showOverlay(new CardPickerDialog(getCard(), picked -> {
            setCard(picked);
            Navigator.go("fetchCurrent");
        }));
    }
}
