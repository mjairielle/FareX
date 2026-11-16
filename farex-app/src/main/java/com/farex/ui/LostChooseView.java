package com.farex.ui;

import javafx.geometry.Insets;
import javafx.scene.Cursor;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.layout.Border;
import javafx.scene.layout.BorderStroke;
import javafx.scene.layout.BorderStrokeStyle;
import javafx.scene.layout.BorderWidths;
import javafx.scene.layout.CornerRadii;
import javafx.scene.layout.VBox;

// Lost Card / 01: pick the card and the reason (Lost or stolen / Damaged card)
public class LostChooseView extends SubScreen {

    private static final String TITLE = "Replace or Report Lost Card";

    public LostChooseView() {
        super(TITLE, "dashboard");

        DemoCard card = LostCardState.getCard();

        VBox heading = new VBox(8,
                FetchUi.title("Replace or report a card"),
                FetchUi.subtitle("Lost your card or need a replacement? Start by selecting the affected card."));
        VBox.setMargin(heading, new Insets(0, 0, 6, 0));

        // ----- Selected card -----
        Label change = FetchUi.smallBold("Change", Theme.LIME);
        change.setCursor(Cursor.HAND);
        change.setOnMouseClicked(e -> LostCardState.pickCard());

        Label holder = new Label(card.getHolderName());
        holder.setTextFill(Theme.WHITE);
        holder.setFont(Theme.bold(14));

        VBox cardPanel = FetchUi.panel(
                FetchUi.row("Selected card", change),
                holder,
                FetchUi.row("xPress " + FetchUi.masked(card), "Active"),
                FetchUi.row("Current balance", FetchUi.peso(card.getBalance())));

        // ----- Reasons: the chosen one is on top with a lime outline -----
        boolean lost = LostCardState.LOST.equals(LostCardState.getReason());
        Node chosen = reasonPanel(lost ? LostCardState.LOST : LostCardState.DAMAGED, true);
        Node other = reasonPanel(lost ? LostCardState.DAMAGED : LostCardState.LOST, false);

        body.setSpacing(12);
        body.getChildren().addAll(
                heading,
                cardPanel,
                chosen,
                other,
                FetchUi.spacer(),
                FetchUi.limeButton("Continue", () -> Navigator.go("lostConfirm")),
                FetchUi.link("Cancel", LostCardState::goHome));
    }

    private Node reasonPanel(String reason, boolean selected) {
        String description = LostCardState.LOST.equals(reason)
                ? "Freeze this card and protect its balance."
                : "Replace a card that no longer works.";

        VBox panel = FetchUi.panel(
                FetchUi.row(selected ? "Selected reason" : "Other reason", reason),
                FetchUi.small(description, Theme.WHITE));

        if (selected) {
            panel.setBorder(new Border(new BorderStroke(Theme.LIME,
                    BorderStrokeStyle.SOLID, new CornerRadii(8), new BorderWidths(1.2))));
        } else {
            // Tapping the other reason swaps the two
            panel.setCursor(Cursor.HAND);
            panel.setOnMouseClicked(e -> {
                LostCardState.setReason(reason);
                Navigator.go("lostChoose");
            });
        }
        return panel;
    }
}
