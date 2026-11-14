package com.farex;

import farex.shared.data.Store;
import farex.shared.user.Passenger;
import farex.shared.user.User;
import java.util.List;
import javafx.collections.FXCollections;
import javafx.scene.control.*;
import javafx.scene.layout.*;

/**
 * Passenger Home Screen - Follows Passenger App flowchart (07-passenger-app.md).
 * Center dashboard for xPress Card, Buy Load, Rewards, Trip Planner,
 * Jeepney Routes, Filtered Transactions, and Profile.
 */
public class PassengerHome extends Screen {
    private final ComboBox<String> pick = combo();
    private final Label balance = lime("", 34);

    public PassengerHome() {
        User u = Session.user;
        Passenger p = u instanceof Passenger pass ? pass : null;
        String passType = p != null ? p.getPassengerType() : "REGULAR";

        VBox content = new VBox(12);
        content.setStyle("-fx-background-color: #111111;");

        content.getChildren().add(heading("Mabuhay, " + u.getName() + "!"));
        if (!passType.equals("REGULAR")) {
            content.getChildren().add(lime("● " + passType + " 20% Discount Active", 12));
        }

        List<String> cards = Api.cardsOf();
        if (cards.isEmpty()) {
            VBox emptyCard = new VBox(8);
            emptyCard.setStyle("-fx-background-color: #1c1c1c; -fx-padding: 16; -fx-background-radius: 8;");
            emptyCard.getChildren().addAll(
                    text("No xPress card linked yet. Link your card to start riding and loading."),
                    primary("Link xPress Card", () -> Nav.show(new AddCardScreen()))
            );
            content.getChildren().add(emptyCard);
        } else {
            pick.setItems(FXCollections.observableArrayList(cards.stream().map(Store::formatUid).toList()));
            pick.valueProperty().addListener((o, was, now) -> refreshBalance());
            pick.setValue(pick.getItems().get(0));

            VBox card = new VBox(8);
            card.setStyle("-fx-background-color: #1c1c1c; -fx-padding: 16; -fx-background-radius: 8;");
            card.getChildren().addAll(
                    text("xPress Card  |  Available Transit Balance"),
                    balance,
                    text("Cardholder: " + u.getName()),
                    pick
            );

            HBox cardQuickActions = new HBox(8);
            Button btnCardTxn = secondary("Card Activity", () -> Nav.show(new TransactionsScreen(uid())));
            Button btnLost = secondary("Lost Card", () -> Nav.show(new LostCardScreen()));
            btnCardTxn.setMaxWidth(Double.MAX_VALUE);
            btnLost.setMaxWidth(Double.MAX_VALUE);
            HBox.setHgrow(btnCardTxn, Priority.ALWAYS);
            HBox.setHgrow(btnLost, Priority.ALWAYS);
            cardQuickActions.getChildren().addAll(btnCardTxn, btnLost);

            content.getChildren().addAll(
                    card,
                    cardQuickActions,
                    primary("BUY LOAD", () -> Nav.show(new BuyLoadScreen(uid()))),
                    secondary("View Rewards", () -> Nav.show(new RewardsScreen(uid(), ""))),
                    secondary("Link Another Card", () -> Nav.show(new AddCardScreen()))
            );
        }

        content.getChildren().addAll(
                secondary("Trip Planner", () -> Nav.show(new TripPlannerScreen())),
                secondary("Jeepney Routes", () -> Nav.show(new RoutesScreen())),
                secondary("All Transactions", () -> Nav.show(new TransactionsScreen())),
                secondary("My Profile", () -> Nav.show(new ProfileScreen())),
                secondary("Log out", this::logout)
        );

        ScrollPane scroll = new ScrollPane(content);
        scroll.setFitToWidth(true);
        scroll.setStyle("-fx-background: #111111; -fx-background-color: #111111; -fx-border-color: transparent;");
        setVgrow(scroll, Priority.ALWAYS);

        getChildren().addAll(scroll, error);
    }

    private String uid() {
        if (pick.getValue() == null) return "";
        return pick.getValue().replace(" ", "");
    }

    private void refreshBalance() {
        if (pick.getValue() != null) {
            balance.setText(peso(Api.balance(uid())));
        }
    }
}