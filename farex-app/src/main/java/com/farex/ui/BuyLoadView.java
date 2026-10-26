package com.farex.ui;

import java.util.ArrayList;
import java.util.List;

import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;

// Buy Load, step 1: pick the card to load, by typing its number or tapping a tile
public class BuyLoadView extends SubScreen {

    private final CardNumberField numberField = new CardNumberField();
    private final List<CardChoiceTile> tiles = new ArrayList<>();
    private final Label errorLabel = new Label();

    public BuyLoadView() {
        super("Buy Load", "dashboard");

        Label caption = new Label("xPress Card Number (last 8 digits)");
        caption.setTextFill(Theme.WHITE);
        caption.setFont(Font.font("System", 12));

        errorLabel.setTextFill(Theme.ERROR);
        errorLabel.setWrapText(true);
        errorLabel.setVisible(false);
        errorLabel.managedProperty().bind(errorLabel.visibleProperty());

        Region divider = new Region();
        divider.setMinHeight(1);
        divider.setPrefHeight(1);
        divider.setMaxHeight(1);
        divider.setBackground(Theme.solid(Color.web("#3B3B3B")));
        VBox.setMargin(divider, new Insets(10, 0, 0, 0));

        Label gridTitle = new Label("Your xPress Cards");
        gridTitle.setTextFill(Theme.WHITE);
        gridTitle.setFont(Theme.bold(13));
        VBox.setMargin(gridTitle, new Insets(8, 0, 0, 0));

        Button next = new Button("Next");
        Theme.limeButton(next);
        next.setMaxWidth(Double.MAX_VALUE);
        next.setOnAction(e -> goNext());
        numberField.setOnEnter(next::fire);

        // Typing a saved card's number lights up its tile
        numberField.setOnDigitsChanged(digits -> {
            errorLabel.setVisible(false);
            for (CardChoiceTile tile : tiles) {
                tile.setSelected(tile.getCard().getCardNumber().equals("00" + digits));
            }
        });

        body.getChildren().addAll(caption, numberField, errorLabel, divider, gridTitle, cardGrid(), next);

        // Coming back from the next step: keep the card that was picked
        DemoCard picked = BuyLoadState.getCard();
        if (picked != null) {
            numberField.setDigits(picked.getCardNumber().substring(2));
        }
        Platform.runLater(numberField::focusField);
    }

    // Two columns of square tiles; scrolls when there are many cards
    private ScrollPane cardGrid() {
        GridPane grid = new GridPane();
        grid.setHgap(34);
        grid.setVgap(20);
        grid.setAlignment(Pos.TOP_CENTER);

        DemoAccount account = MockData.getCurrentAccount();
        List<DemoCard> cards = (account == null) ? new ArrayList<>() : account.getCards();
        for (int i = 0; i < cards.size(); i++) {
            CardChoiceTile tile = new CardChoiceTile(cards.get(i));
            // "0067676767" -> type "67676767"; the field formats it and the listener selects the tile
            tile.setOnMouseClicked(e -> numberField.setDigits(tile.getCard().getCardNumber().substring(2)));
            tiles.add(tile);
            grid.add(tile, i % 2, i / 2);   // card 0 -> (0,0), 1 -> (1,0), 2 -> (0,1) ...
        }

        ScrollPane scroll = new ScrollPane(grid);
        scroll.setFitToWidth(true);
        scroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scroll.setVbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scroll.setStyle("-fx-background: transparent; -fx-background-color: transparent;");
        VBox.setVgrow(scroll, Priority.ALWAYS);
        return scroll;
    }

    private void goNext() {
        if (!numberField.isComplete()) {
            showError("Enter the last 8 digits of your card, for example 6767 6767.");
            return;
        }
        for (CardChoiceTile tile : tiles) {
            if (tile.getCard().getCardNumber().equals(numberField.getFullNumber())) {
                BuyLoadState.setCard(tile.getCard());
                Navigator.go("loadPayment");
                return;
            }
        }
        showError("Card not found. Add it first.");
    }

    private void showError(String message) {
        errorLabel.setText(message);
        errorLabel.setVisible(true);
    }
}
