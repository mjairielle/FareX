package com.farex;

import farex.shared.data.Store;
import farex.shared.user.*;
import java.util.List;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;

/**
 * Passenger Profile Screen - Follows Passenger App flowchart (07-passenger-app.md).
 * Displays user info, passenger discount type, editable name, linked cards,
 * card management (link, lost card), and logout.
 */
public class ProfileScreen extends Screen {
    private final TextField nameField;
    private final Label notice = lime("", 13);

    public ProfileScreen() {
        User u = Session.user;
        Passenger p = u instanceof Passenger pass ? pass : null;
        String passType = p != null ? p.getPassengerType() : "REGULAR";

        getChildren().addAll(
                back(() -> Nav.show(new PassengerHome())),
                heading("My Profile"),
                text("Account details and verified discount status.")
        );

        // User Info Card
        VBox infoCard = new VBox(8);
        infoCard.setStyle("-fx-background-color: #1c1c1c; -fx-padding: 16; -fx-background-radius: 8;");

        nameField = field("Full Name");
        nameField.setText(u.getName());

        Label emailLbl = text("Email: " + u.getEmail());
        Label roleLbl = text("Account Role: " + u.role());
        Label discountLbl = lime("Transit Fare Type: " + passType + (passType.equals("REGULAR") ? " (Standard fare)" : " (20% Automatic Discount)"), 13);

        Button btnSaveName = primary("Save Name Changes", () -> {
            String newName = nameField.getText().trim();
            if (newName.isBlank()) throw new IllegalArgumentException("Name cannot be empty");
            Api.updateProfile(newName);
            // Update current session user in-memory
            if (p != null) {
                Session.user = new Passenger(p.getId(), newName, p.getEmail(), p.getPassengerType());
            }
            notice.setText("Profile updated successfully!");
        });

        infoCard.getChildren().addAll(
                text("Account Name:"), nameField,
                btnSaveName, emailLbl, roleLbl, discountLbl
        );

        // Linked Cards Card
        VBox cardsCard = new VBox(8);
        cardsCard.setStyle("-fx-background-color: #1c1c1c; -fx-padding: 16; -fx-background-radius: 8;");
        Label cardsTitle = lime("Linked xPress Cards", 14);
        cardsCard.getChildren().add(cardsTitle);

        List<String> cards = Api.cardsOf();
        if (cards.isEmpty()) {
            cardsCard.getChildren().add(text("No xPress cards linked yet."));
        } else {
            for (String c : cards) {
                HBox cRow = new HBox(10);
                cRow.setAlignment(Pos.CENTER_LEFT);
                cRow.setStyle("-fx-background-color: #262626; -fx-padding: 8; -fx-background-radius: 4;");
                Label cUid = new Label(Store.formatUid(c));
                cUid.setStyle("-fx-text-fill: white; -fx-font-weight: bold;");
                Region sp = new Region();
                HBox.setHgrow(sp, Priority.ALWAYS);
                Label cBal = lime(peso(Api.balance(c)), 13);
                cRow.getChildren().addAll(cUid, sp, cBal);
                cardsCard.getChildren().add(cRow);
            }
        }

        HBox cardActions = new HBox(8);
        Button btnAdd = secondary("Link New Card", () -> Nav.show(new AddCardScreen()));
        Button btnLost = secondary("Report Lost Card", () -> Nav.show(new LostCardScreen()));
        btnAdd.setMaxWidth(Double.MAX_VALUE);
        btnLost.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(btnAdd, Priority.ALWAYS);
        HBox.setHgrow(btnLost, Priority.ALWAYS);
        cardActions.getChildren().addAll(btnAdd, btnLost);
        cardsCard.getChildren().add(cardActions);

        // Logout
        Button btnLogout = secondary("Log Out", this::logout);

        getChildren().addAll(infoCard, cardsCard, notice, btnLogout, error);
    }
}

