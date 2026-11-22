package com.farex.ui.data;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class DemoCard {

    private final String cardNumber;
    private final String holderName;
    private double balance;
    private final String expiry;
    private final List<PendingLoad> pendingLoads = new ArrayList<>();
    private boolean frozen = false;
    private String freezeReference = "";

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

    // A jeepney fare tapped by an operator: 676.50 - 13.00 -> 663.50
    public void payFare(double amount) {
        balance -= amount;
    }

    public String getExpiry() {
        return expiry;
    }

    // Demo cards are "00" + 8 digits ("00 6767 6767"); live cards are just the 8 printed digits ("2727 2727")
    public String getFormattedNumber() {
        if (cardNumber.length() == 10) {
            return cardNumber.substring(0, 2) + " " + cardNumber.substring(2, 6) + " " + cardNumber.substring(6);
        }
        if (cardNumber.length() == 8) {
            return cardNumber.substring(0, 4) + " " + cardNumber.substring(4);
        }
        return cardNumber;
    }

    // The 8 digits printed on the card, what the user types: "0067676767" -> "67676767", "27272727" -> "27272727"
    public String getLast8() {
        return cardNumber.length() > 8 ? cardNumber.substring(cardNumber.length() - 8) : cardNumber;
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

    // The server's balance after a payment it confirmed
    public void setBalance(double pesos) {
        balance = pesos;
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

    // ----- Lost card -----

    // Reported lost: the card is blocked, its balance waits for a replacement card
    public void freeze(String reportReference) {
        frozen = true;
        freezeReference = reportReference;
    }

    public boolean isFrozen() {
        return frozen;
    }

    // "FX-LC-1026"
    public String getFreezeReference() {
        return freezeReference;
    }

    // Replacement: ₱676.50 (and any load still waiting) moves to the new card; this one keeps ₱0.00
    public void transferTo(DemoCard replacement) {
        replacement.addLoad(balance);
        balance = 0;
        for (PendingLoad load : pendingLoads) {
            replacement.addPendingLoad(load);
        }
        pendingLoads.clear();
    }
}
