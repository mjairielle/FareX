package com.farex;

import javafx.scene.control.TextField;

public class AddCardScreen extends Screen {
    public AddCardScreen() {
        TextField digits = field("6767 6767");
        getChildren().addAll(back(() -> Nav.show(new PassengerHome())),
                heading("Add a xPress Card"),
                text("xPress Card Number (last 8 digits)"), digits,
                primary("Save Card", () -> {
                    Session.store.addCard(digits.getText().replace(" ", ""), Session.user.getId());
                    Nav.show(new PassengerHome());
                }),
                error);
    }
}
