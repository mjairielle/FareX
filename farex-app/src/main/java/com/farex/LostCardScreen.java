package com.farex;

import farex.shared.data.Store;
import java.util.List;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;

/**
 * Lost or Stolen Card Screen - Follows Card System flowchart (02-card-system.md).
 * Permanently freezes the lost card UID on the server and transfers the remaining
 * balance to a new replacement xPress card.
 */
public class LostCardScreen extends Screen {
    private final ComboBox<String> cardSelector = combo();
    private final TextField newCardField = field("Enter replacement card last 8 digits");
    private final Label notice = lime("", 13);

    public LostCardScreen() {
        getChildren().addAll(
                back(() -> Nav.show(new PassengerHome())),
                heading("Report Lost / Stolen Card"),
                text("Protect your funds immediately. When you report a card lost, it is permanently frozen across all offline and online transit terminals. Your balance moves to your replacement card.")
        );

        List<String> cards = Api.cardsOf();
        if (cards.isEmpty()) {
            getChildren().add(text("You currently have no active cards linked to your account."));
            return;
        }

        for (String c : cards) {
            long bal = Api.balance(c);
            cardSelector.getItems().add(Store.formatUid(c) + " (Balance: " + peso(bal) + ")");
        }
        cardSelector.setValue(cardSelector.getItems().get(0));

        VBox form = new VBox(10);
        form.setStyle("-fx-background-color: #1c1c1c; -fx-padding: 16; -fx-background-radius: 8;");
        form.getChildren().addAll(
                text("Select Lost Card:"),
                cardSelector,
                text("Replacement Card Last 8 Digits:"),
                newCardField,
                primary("FREEZE & TRANSFER BALANCE", this::handleLostCard)
        );

        getChildren().addAll(form, notice, error);
    }

    private void handleLostCard() {
        String sel = cardSelector.getValue();
        if (sel == null) throw new IllegalStateException("Please select a card to report");
        String oldUid = sel.substring(0, 12).replace(" ", "");

        String newLast8 = newCardField.getText().trim();
        if (!newLast8.matches("\\d{8}")) throw new IllegalArgumentException("Replacement card needs exactly 8 digits");

        String[] res = Api.reportLostCard(oldUid, newLast8);
        String newUid = res[0];
        long newBal = Long.parseLong(res[1]);

        notice.setText("Card " + Store.formatUid(oldUid) + " has been permanently frozen.\n"
                + "Replacement Card " + Store.formatUid(newUid) + " is active with balance " + peso(newBal) + ".");
        notice.setStyle("-fx-text-fill: #c8ff00; -fx-font-weight: bold; -fx-padding: 10; -fx-background-color: #1e331e; -fx-background-radius: 6;");

        // Clear input and reload
        newCardField.clear();
        getChildren().add(primary("Done", () -> Nav.show(new PassengerHome())));
    }
}
<<<<<<< Updated upstream

=======
>>>>>>> Stashed changes
