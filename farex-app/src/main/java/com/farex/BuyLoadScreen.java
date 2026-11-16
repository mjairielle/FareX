package com.farex;

import farex.shared.data.Store;
import javafx.scene.control.*;
import javafx.scene.layout.FlowPane;

public class BuyLoadScreen extends Screen {
    private String session;

    public BuyLoadScreen(String uid) {
        TextField amount = field("0.00");

        FlowPane chips = new FlowPane(8, 8);
        for (int pesos : new int[] { 10, 15, 20, 50, 100, 200, 300, 500 }) {
            chips.getChildren().add(secondary("P" + pesos, () -> amount.setText(pesos + ".00")));
        }

        Button check = secondary("I have paid, check", () -> {
            if (!Api.confirmLoad(session)) throw new IllegalStateException("Not paid yet. Finish paying, then tap again.");
            Nfc.rewrite(uid);
            Nav.show(new PassengerHome());
        });
        check.setVisible(false);
        check.setManaged(false);

        getChildren().addAll(back(() -> Nav.show(new PassengerHome())),
                heading("Buy Load"),
                text("Card " + Store.formatUid(uid)),
                lime(peso(Api.balance(uid)), 30),
                text("Enter desired load amount (P5.00 to P5,000.00)"), amount, chips,
                text("You pick GCash, Maya or card on the payment page (PayMongo test mode, no real money)"),
                primary("Pay", () -> {
                    double pesos;
                    try {
                        pesos = Double.parseDouble(amount.getText().trim());
                    } catch (NumberFormatException e) {
                        throw new IllegalArgumentException("Enter a valid amount");
                    }
                    String[] s = Api.startLoad(uid, Math.round(pesos * 100));
                    session = s[0];
                    if (s[1].equals("fake")) {                 // server runs without PayMongo
                        Api.confirmLoad(session);
                        Nfc.rewrite(uid);
                        Nav.show(new PassengerHome());
                        return;
                    }
                    Nav.open(s[1]);
                    check.setVisible(true);
                    check.setManaged(true);
                }),
                check, error);
    }
}
