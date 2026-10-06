package com.farex.ui;

import java.util.Locale;

/**
 * Stand-in for a card until the backend supplies real ones.
 * cardNumber is text, 10 digits: "0067676767" (the "00" prefix + the last 8 digits).
 */
public class DemoCard {

    private final String cardNumber;
    private final String holderName;
    private final double balance;
    private final String expiry;

    public DemoCard(String cardNumber, String holderName, double balance, String expiry) {
        this.cardNumber = cardNumber;
        this.holderName = holderName;
        this.balance = balance;
        this.expiry = expiry;
    }

    public String getCardNumber() {
        return cardNumber;
    }

    public String getHolderName() {
        return holderName;
    }

    public double getBalance() {
        return balance;
    }

    public String getExpiry() {
        return expiry;
    }

    /** "0067676767" -> "00 6767 6767" */
    public String getFormattedNumber() {
        return cardNumber.substring(0, 2) + " " + cardNumber.substring(2, 6) + " " + cardNumber.substring(6);
    }

    /** 676.5 -> "676.50" (the peso sign is added by the screen) */
    public String getBalanceText() {
        return String.format(Locale.US, "%,.2f", balance);
    }
}
