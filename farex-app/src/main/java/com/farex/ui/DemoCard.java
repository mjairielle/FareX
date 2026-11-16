package com.farex.ui;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class DemoCard {

    private final String cardNumber;
    private final String holderName;
    private double balance;
    private final String expiry;
    private final List<PendingLoad> pendingLoads = new ArrayList<>();

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

    // "0067676767" -> "6767"
    public String getLastFour() {
        return cardNumber.substring(cardNumber.length() - 4);
    }

    public String getBalanceText() {
        return String.format(Locale.US, "%,.2f", balance);
    }

    // ----- Pending loads -----

    // Buy Load adds one here; the balance does not change yet
    public void addPendingLoad(PendingLoad load) {
        pendingLoads.add(load);
    }

    public boolean hasPendingLoad() {
        return !pendingLoads.isEmpty();
    }

    // The oldest one is fetched first
    public PendingLoad getPendingLoad() {
        return pendingLoads.isEmpty() ? null : pendingLoads.get(0);
    }

    // Fetch Load: ₱676.50 + pending ₱200.00 -> ₱876.50, and the pending load is gone
    public void collect(PendingLoad load) {
        if (pendingLoads.remove(load)) {
            addLoad(load.getAmount());
        }
    }
}
