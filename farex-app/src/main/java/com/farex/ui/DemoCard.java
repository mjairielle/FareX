package com.farex.ui;

import java.util.Locale;

public class DemoCard {

    private final String cardNumber;
    private final String holderName;
    private double balance;
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

    // Stand-in until the backend updates balances: 676.50 + 200 -> 876.50
    public void addLoad(double amount) {
        balance += amount;
    }

    public String getExpiry() {
        return expiry;
    }

    public String getFormattedNumber() {
        return cardNumber.substring(0, 2) + " " + cardNumber.substring(2, 6) + " " + cardNumber.substring(6);
    }

    public String getBalanceText() {
        return String.format(Locale.US, "%,.2f", balance);
    }
}
