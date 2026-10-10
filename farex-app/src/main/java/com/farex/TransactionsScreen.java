package com.farex;

import farex.shared.txn.Txn;
import java.util.List;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

public class TransactionsScreen extends Screen {
    public TransactionsScreen() {
        VBox rows = new VBox(8);
        rows.setStyle("-fx-background-color: #111111;");
        List<Txn> history = Api.history();
        if (history.isEmpty()) rows.getChildren().add(text("No transactions yet."));
        for (Txn t : history) rows.getChildren().add(receiptRow(t));

        ScrollPane scroll = new ScrollPane(rows);
        scroll.setFitToWidth(true);
        scroll.setStyle("-fx-background: #111111; -fx-background-color: #111111;");
        setVgrow(scroll, Priority.ALWAYS);

        getChildren().addAll(back(() -> Nav.show(new PassengerHome())), heading("Transactions"), scroll);
    }
}
