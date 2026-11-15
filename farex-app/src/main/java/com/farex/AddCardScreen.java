package com.farex;

import javafx.scene.control.TextField;

public class AddCardScreen extends Screen {
    public AddCardScreen() {
        TextField digits = field("53 89 CB 02");
        getChildren().addAll(back(() -> Nav.show(new PassengerHome())),
                heading("Add a xPress Card"),
                text("Card UID, 8 hex characters (the chip UID, shown by an NFC reader app)"), digits,
                primary("Save Card", () -> {
                    Api.addCard(digits.getText());
                    Nav.show(new PassengerHome());
                }),
                error);
    }
}
