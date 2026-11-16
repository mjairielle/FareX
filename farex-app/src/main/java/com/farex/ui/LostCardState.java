package com.farex.ui;

import javafx.scene.Parent;
import javafx.scene.control.Label;

// Remembers the Lost Card choices (card, reason, new card number), freezes the old card
// and moves its balance to the replacement. A frozen card skips to "Get a replacement card".
public final class LostCardState {

    public static final String LOST = "Lost or stolen";
    public static final String DAMAGED = "Damaged card";

    private static DemoCard card;
    private static String reason = LOST;

    // Replacement: the typed number, the new card, and the transfer reference
    private static String newCardNumber = "";
    private static DemoCard replacement;
    private static String transferReference = "";
    private static double transferredAmount;

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

    public static String getNewCardNumber() {
        return newCardNumber;
    }

    public static void setNewCardNumber(String fullNumber) {
        newCardNumber = fullNumber;
    }

    // "0088428842" -> "•••• 8842"
    public static String maskedNewCard() {
        return "•••• " + newCardNumber.substring(newCardNumber.length() - 4);
    }

    public static DemoCard getReplacement() {
        return replacement;
    }

    public static String getTransferReference() {
        return transferReference;
    }

    public static double getTransferredAmount() {
        return transferredAmount;
    }

    public static void clear() {
        card = null;
        reason = LOST;
        newCardNumber = "";
        replacement = null;
        transferReference = "";
        transferredAmount = 0;
    }

    // ----- Actions -----

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

    // Transfer confirmed: new card 0088428842 gets ₱676.50 and takes the old card's place
    public static void transfer() {
        DemoCard old = getCard();
        replacement = new DemoCard(newCardNumber, old.getHolderName(), 0.00, old.getExpiry());
        transferredAmount = old.getBalance();
        old.transferTo(replacement);
        transferReference = MockData.nextReference("TR");

        DemoAccount account = MockData.getCurrentAccount();
        if (account != null) {
            account.replaceCard(old, replacement);
            account.addNotification(new DemoNotification("Card Replaced",
                    "Your balance moved to **" + FetchUi.masked(replacement) + "**.", false));
            account.addTransaction(DemoTransaction.TRANSFER, "Balance Transfer",
                    "Old xPress Card → New xPress Card", transferredAmount);
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
            return new LostReplaceView();
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
