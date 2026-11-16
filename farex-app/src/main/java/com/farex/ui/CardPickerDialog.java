package com.farex.ui;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.Border;
import javafx.scene.layout.BorderStroke;
import javafx.scene.layout.BorderStrokeStyle;
import javafx.scene.layout.BorderWidths;
import javafx.scene.layout.CornerRadii;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;

// Pop-up list of the account's cards. Each row shows whether that card has load waiting.
// Tapping a row closes the pop-up and hands the card to onPick.
public class CardPickerDialog extends StackPane {

    private static final Border SELECTED = new Border(new BorderStroke(
            Theme.LIME, BorderStrokeStyle.SOLID, new CornerRadii(8), new BorderWidths(1.5)));

    public CardPickerDialog(DemoCard selected, Consumer<DemoCard> onPick) {
        // Dim layer over the blurred screen; it also stops clicks reaching the screen below
        setBackground(Theme.solid(Color.rgb(0, 0, 0, 0.45)));

        Label title = new Label("Choose a card");
        title.setTextFill(Theme.WHITE);
        title.setFont(Theme.bold(18));

        VBox rows = new VBox(8);
        DemoAccount account = MockData.getCurrentAccount();
        List<DemoCard> cards = (account == null) ? new ArrayList<>() : account.getCards();
        for (DemoCard card : cards) {
            rows.getChildren().add(row(card, card == selected, onPick));
        }

        Button cancel = new Button("Cancel");
        cancel.setFont(Theme.bold(13));
        cancel.setPadding(new Insets(8));
        cancel.setMaxWidth(Double.MAX_VALUE);
        Theme.paintButton(cancel, "#141414", "#1E1E1E", "#0E0E0E", 6, "white");
        cancel.setBorder(new Border(new BorderStroke(Color.web("#4A4A4A"),
                BorderStrokeStyle.SOLID, new CornerRadii(6), new BorderWidths(1))));
        cancel.setOnAction(e -> Navigator.closeOverlay());
        VBox.setMargin(cancel, new Insets(4, 0, 0, 0));

        VBox box = new VBox(12, title, rows, cancel);
        box.setPadding(new Insets(20, 18, 18, 18));
        box.setMaxSize(300, VBox.USE_PREF_SIZE);
        box.setBackground(Theme.rounded(Color.web("#1C1C1C"), 10));
        box.setBorder(new Border(new BorderStroke(Theme.LIME,
                BorderStrokeStyle.SOLID, new CornerRadii(10), new BorderWidths(1.5))));

        getChildren().add(box);
    }

    // "Kenn Urbano / xPress •••• 1111" on the left, "₱200.00 pending" or "No pending load" on the right
    private Node row(DemoCard card, boolean isSelected, Consumer<DemoCard> onPick) {
        Label name = FetchUi.smallBold(card.getHolderName(), Theme.WHITE);
        name.setFont(Theme.bold(13));
        Label number = FetchUi.small("xPress " + FetchUi.masked(card), Theme.GRAY);
        VBox left = new VBox(2, name, number);
        HBox.setHgrow(left, Priority.ALWAYS);

        PendingLoad load = card.getPendingLoad();
        Label status = (load == null)
                ? FetchUi.small("No pending load", Theme.GRAY)
                : FetchUi.smallBold(FetchUi.peso(load.getAmount()) + " pending", Theme.LIME);

        HBox row = new HBox(8, left, status);
        row.setAlignment(Pos.CENTER_LEFT);
        row.setPadding(new Insets(10, 12, 10, 12));
        row.setBackground(Theme.rounded(Color.web("#262626"), 8));
        if (isSelected) {
            row.setBorder(SELECTED);
        }
        row.setCursor(Cursor.HAND);
        row.setOnMouseClicked(e -> {
            Navigator.closeOverlay();
            onPick.accept(card);
        });
        return row;
    }
}
