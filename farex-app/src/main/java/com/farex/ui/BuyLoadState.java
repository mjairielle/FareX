package com.farex.ui;

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

    public static void clear() {
        card = null;
        method = null;
        accountNumber = null;
        amount = 0;
    }
}
