package com.farex.ui;

import javafx.scene.Parent;
import javafx.scene.control.Label;

// Remembers the Lost Card choices (card, reason) and freezes the card.
// A card that is already frozen skips straight to "Get a replacement card".
public final class LostCardState {

    public static final String LOST = "Lost or stolen";
    public static final String DAMAGED = "Damaged card";

    private static DemoCard card;
    private static String reason = LOST;

    private LostCardState() {
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

    public static String getReason() {
        return reason;
    }

    public static void setReason(String newReason) {
        reason = newReason;
    }

    public static void clear() {
        card = null;
        reason = LOST;
    }

    // Report confirmed: •••• 6767 is frozen as FX-LC-1026, plus a notification
    public static void freeze() {
        DemoCard current = getCard();
        current.freeze(MockData.nextReference("LC"));

        DemoAccount account = MockData.getCurrentAccount();
        if (account != null) {
            account.addNotification(new DemoNotification("Card Frozen",
                    "Your card **" + FetchUi.masked(current) + "** has been frozen.", false));
        }
    }

    // ----- Screens -----

    // From Home: start again with the first card
    public static Parent start() {
        clear();
        return currentScreen();
    }

    public static Parent currentScreen() {
        DemoCard current = getCard();
        if (current != null && current.isFrozen()) {
            return new PlaceholderView("Replace or Report Lost Card", "dashboard");   // 07, Part 17
        }
        return new LostChooseView();
    }

    // "Change": the card list shows Active / Frozen on the right
    public static void pickCard() {
        Navigator.showOverlay(new CardPickerDialog(getCard(), picked -> {
            setCard(picked);
            Navigator.go("lostCurrent");
        }, LostCardState::statusLabel));
    }

    private static Label statusLabel(DemoCard c) {
        return c.isFrozen()
                ? FetchUi.smallBold("Frozen", Theme.ERROR)
                : FetchUi.smallBold("Active", Theme.LIME);
    }

    public static void goHome() {
        clear();
        Navigator.go("dashboard");
    }
}
