package com.farex;

import farex.shared.data.Store;
import javafx.collections.FXCollections;
import javafx.scene.control.*;
import javafx.scene.layout.FlowPane;

public class BuyLoadScreen extends Screen {
    public BuyLoadScreen(String uid) {
        TextField amount = field("0.00");

        FlowPane chips = new FlowPane(8, 8);
        for (int pesos : new int[] { 10, 15, 20, 50, 100, 200, 300, 500 }) {
            chips.getChildren().add(secondary("P" + pesos, () -> amount.setText(pesos + ".00")));
        }

        ComboBox<String> method = combo();
        method.setItems(FXCollections.observableArrayList("GCash", "Maya", "Bank"));
        method.setValue("GCash");

        getChildren().addAll(back(() -> Nav.show(new PassengerHome())),
                heading("Buy Load"),
                text("Card " + Store.formatUid(uid)),
                lime(peso(Session.store.balance(uid)), 30),
                text("Enter desired load amount (P5.00 to P5,000.00)"), amount, chips,
                text("Payment method (simulated, no real charge)"), method,
                primary("Pay", () -> {
                    double pesos;
                    try {
                        pesos = Double.parseDouble(amount.getText().trim());
                    } catch (NumberFormatException e) {
                        throw new IllegalArgumentException("Enter a valid amount");
                    }
                    Session.store.load(uid, Session.user.getId(), Math.round(pesos * 100), method.getValue());
                    Nav.show(new PassengerHome());
                }),
                error);
    }
}
