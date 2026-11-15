package com.farex;

import farex.shared.data.Store;
import farex.shared.txn.Txn;
import java.util.List;
import javafx.scene.control.*;
import javafx.scene.layout.*;

/**
 * Passenger Transactions Screen - Supports viewing all activity across loads,
 * fare payments, and rewards redemptions, or filtering down to a specific card's ledger.
 * Follows flowchart 06-transactions.md.
 */
public class TransactionsScreen extends Screen {
    private final ComboBox<String> cardFilter = combo();
    private final VBox rows = new VBox(8);
    private List<Txn> allTxns = List.of();

    public TransactionsScreen() {
        this("");
    }

    public TransactionsScreen(String initialCardUid) {
        rows.setStyle("-fx-background-color: #111111;");

        try {
            allTxns = Api.history();
        } catch (Exception e) {
            error.setText("Error loading transactions: " + e.getMessage());
        }

        cardFilter.getItems().add("All Transactions");
        List<String> cards = Api.cardsOf();
        for (String c : cards) {
            cardFilter.getItems().add(Store.formatUid(c));
        }

        if (initialCardUid != null && !initialCardUid.isBlank()) {
            String formatted = Store.formatUid(initialCardUid);
            if (cardFilter.getItems().contains(formatted)) {
                cardFilter.setValue(formatted);
            } else {
                cardFilter.setValue("All Transactions");
            }
        } else {
            cardFilter.setValue("All Transactions");
        }

        cardFilter.valueProperty().addListener((o, was, now) -> refreshRows());

        ScrollPane scroll = new ScrollPane(rows);
        scroll.setFitToWidth(true);
        scroll.setStyle("-fx-background: #111111; -fx-background-color: #111111; -fx-border-color: transparent;");
        setVgrow(scroll, Priority.ALWAYS);

        getChildren().addAll(
                back(() -> Nav.show(new PassengerHome())),
                heading("Transactions"),
                text("Filter by Card:"),
                cardFilter,
                scroll,
                error
        );

        refreshRows();
    }

    private void refreshRows() {
        rows.getChildren().clear();
        String sel = cardFilter.getValue();
        String targetUid = (sel == null || sel.equals("All Transactions")) ? null : sel.replace(" ", "");

        List<Txn> filtered = targetUid == null
                ? allTxns
                : allTxns.stream().filter(t -> targetUid.equals(t.getCardUid())).toList();

        if (filtered.isEmpty()) {
            rows.getChildren().add(text("No transactions found."));
            return;
        }

        long totalFare = 0;
        long totalLoad = 0;
        for (Txn t : filtered) {
            if ("FARE_PAYMENT".equals(t.type())) totalFare += t.getAmount();
            else if ("CARD_LOAD".equals(t.type())) totalLoad += t.getAmount();
            rows.getChildren().add(receiptRow(t));
        }

        Label summary = lime(String.format("Showing %d record(s)  |  Fares: %s  |  Loaded: %s",
                filtered.size(), peso(totalFare), peso(totalLoad)), 11);
        rows.getChildren().add(0, summary);
    }
}
