package com.farex;

import farex.shared.data.Store;
import farex.shared.user.User;
import java.util.List;
import javafx.collections.FXCollections;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;

public class PassengerHome extends Screen {
    private final ComboBox<String> pick = combo();
    private final Label balance = lime("", 34);

    public PassengerHome() {
        User u = Session.user;
        getChildren().add(heading("Mabuhay, " + u.getName() + "!"));

        List<String> cards = Session.store.cardsOf(u.getId());
        if (cards.isEmpty()) {
            getChildren().addAll(text("No card yet. Add your xPress card to start loading."),
                    primary("Add Card", () -> Nav.show(new AddCardScreen())));
        } else {
            pick.setItems(FXCollections.observableArrayList(cards.stream().map(Store::formatUid).toList()));
            pick.valueProperty().addListener((o, was, now) -> refreshBalance());
            pick.setValue(pick.getItems().get(0));

            VBox card = new VBox(6, text("xPress  |  Available transit balance"), balance, text(u.getName()), pick);
            card.setStyle("-fx-background-color: #1c1c1c; -fx-padding: 16;");
            getChildren().addAll(card,
                    primary("BUY LOAD", () -> Nav.show(new BuyLoadScreen(uid()))),
                    secondary("Add Card", () -> Nav.show(new AddCardScreen())));
        }

        getChildren().addAll(
                secondary("View Transactions", () -> Nav.show(new TransactionsScreen())),
                secondary("Log out", this::logout),
                error);
    }

    private String uid() {
        return pick.getValue().replace(" ", "");
    }

    private void refreshBalance() {
        balance.setText(peso(Session.store.balance(uid())));
    }
}
