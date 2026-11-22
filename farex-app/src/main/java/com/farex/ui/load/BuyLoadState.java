package com.farex.ui.load;

import com.farex.ui.data.DemoCard;

// Remembers the choices made during Buy Load (card, payment, amount) between screens.
// The payment code can read everything it needs from here.
public final class BuyLoadState {

    private static DemoCard card;
    private static String method;
    private static String accountNumber;
    private static double amount;

    private BuyLoadState() {
    }

    public static DemoCard getCard() {
        return card;
    }

    public static void setCard(DemoCard newCard) {
        card = newCard;
    }

    public static String getMethod() {
        return method;
    }

    public static void setMethod(String newMethod) {
        method = newMethod;
    }

    public static String getAccountNumber() {
        return accountNumber;
    }

    public static void setAccountNumber(String newAccountNumber) {
        accountNumber = newAccountNumber;
    }

    public static double getAmount() {
        return amount;
    }

    public static void setAmount(double newAmount) {
        amount = newAmount;
    }

    // Live: the PayMongo checkout opened for this load: its session id (the reference) and page
    private static String checkoutId;
    private static String checkoutUrl;

    public static String getCheckoutId() {
        return checkoutId;
    }

    public static String getCheckoutUrl() {
        return checkoutUrl;
    }

    public static void setCheckout(String id, String url) {
        checkoutId = id;
        checkoutUrl = url;
    }

    public static void clear() {
        checkoutId = null;
        checkoutUrl = null;
        card = null;
        method = null;
        accountNumber = null;
        amount = 0;
    }
}
