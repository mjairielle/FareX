package com.farex.ui.operator;

import farex.shared.fare.FareRule;
import farex.shared.user.Passenger;

import com.farex.ui.data.DemoCard;

// Someone who can tap on the jeep: an xPress Card from the demo accounts, or an app user.
// The fare rule comes from farex-shared's Passenger: REGULAR pays full, STUDENT gets 20% off.
public class TapRider {

    private final String id;            // "0067676767" or "APP-0821"
    private final Passenger passenger;  // farex-shared model: name + passenger type
    private final DemoCard card;        // null for app users
    private double appBalance;          // app users only (kept by the "server")

    // xPress Card: the balance lives on the card
    public TapRider(DemoCard card, String passengerType) {
        this.id = card.getCardNumber();
        this.card = card;
        this.passenger = new Passenger(id, card.getHolderName(), "", passengerType);
    }

    // App user: the balance lives on the server
    public TapRider(String appId, String name, String passengerType, double appBalance) {
        this.id = appId;
        this.card = null;
        this.passenger = new Passenger(appId, name, "", passengerType);
        this.appBalance = appBalance;
    }

    public String getId() {
        return id;
    }

    public boolean isApp() {
        return card == null;
    }

    public DemoCard getCard() {
        return card;
    }

    public FareRule getFareRule() {
        return passenger.getFareRule();
    }

    public boolean isDiscounted() {
        return getFareRule().discountPercent() > 0;
    }

    // "Aron Ferrancullo"
    public String getName() {
        return passenger.getName();
    }

    // "Regular" / "Student"
    public String getTypeLabel() {
        String type = passenger.getPassengerType();
        return type.charAt(0) + type.substring(1).toLowerCase();
    }

    // "**** 6767" or "APP-0821"
    public String getShortId() {
        return isApp() ? id : "**** " + card.getLastFour();
    }

    // Picker row title: "Aron (Regular)" or "App user (Student)"
    public String getPickerName() {
        String name = isApp() ? getName() : getName().split(" ")[0];
        return name + " (" + getTypeLabel() + ")";
    }

    // "00 6767 6767" or "APP-0821"
    public String getDisplayId() {
        return isApp() ? id : card.getFormattedNumber();
    }

    public boolean isFrozen() {
        return card != null && card.isFrozen();
    }

    // ----- Money (pesos, like DemoCard) -----

    public double getBalance() {
        return isApp() ? appBalance : card.getBalance();
    }

    public boolean canPay(long cents) {
        return getBalance() * 100 + 0.001 >= cents;
    }

    public void pay(long cents) {
        double pesos = cents / 100.0;
        if (isApp()) {
            appBalance -= pesos;
        } else {
            card.payFare(pesos);
        }
    }
}
