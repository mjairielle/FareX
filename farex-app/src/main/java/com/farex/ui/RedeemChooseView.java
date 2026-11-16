package com.farex.ui;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.control.Label;
import javafx.scene.layout.Border;
import javafx.scene.layout.BorderStroke;
import javafx.scene.layout.BorderStrokeStyle;
import javafx.scene.layout.BorderWidths;
import javafx.scene.layout.CornerRadii;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;

// Redeem Points / 01: pick 50, 100 or 250 points
public class RedeemChooseView extends SubScreen {

    private static final Border CHOICE_BORDER = new Border(new BorderStroke(
            Color.web("#BDBDBD"), BorderStrokeStyle.SOLID, new CornerRadii(6), new BorderWidths(1)));

    private final Label pointsValue = FetchUi.smallBold("", Theme.WHITE);
    private final Label creditValue = FetchUi.smallBold("", Theme.WHITE);
    private final Label hint = FetchUi.small("", Theme.WHITE);
    private final Label[] choiceLabels = new Label[RedeemState.CHOICES.length];

    public RedeemChooseView() {
        super("Redeem Points", "dashboard");

        DemoCard card = RedeemState.getCard();

        VBox heading = new VBox(8,
                FetchUi.title("Turn points into trips"),
                FetchUi.subtitle("Redeem earned xPress Points for transit credit on your linked card."));
        VBox.setMargin(heading, new Insets(0, 0, 6, 0));

        // ----- Available points -----
        Label available = new Label(RedeemState.getAvailable() + " xPress Points");
        available.setTextFill(Theme.LIME);
        available.setFont(Theme.bold(22));
        VBox availablePanel = FetchUi.panel(available, FetchUi.small("Available to redeem", FetchUi.SOFT_TEXT));

        // ----- Choice box (lime outline) -----
        HBox choices = new HBox(8);
        for (int i = 0; i < RedeemState.CHOICES.length; i++) {
            int points = RedeemState.CHOICES[i];
            Label choice = new Label(FetchUi.pts(points));
            choice.setFont(Theme.bold(11));
            choice.setAlignment(Pos.CENTER);
            choice.setMaxWidth(Double.MAX_VALUE);
            choice.setPadding(new Insets(10, 0, 10, 0));
            choice.setCursor(Cursor.HAND);
            choice.setOnMouseClicked(e -> select(points));
            HBox.setHgrow(choice, Priority.ALWAYS);
            choiceLabels[i] = choice;
            choices.getChildren().add(choice);
        }
        VBox.setMargin(choices, new Insets(4, 0, 0, 0));

        VBox choicePanel = FetchUi.panel(
                FetchUi.row("Points to redeem", pointsValue),
                FetchUi.row("Transit credit", creditValue),
                choices);
        choicePanel.setBorder(new Border(new BorderStroke(Theme.LIME,
                BorderStrokeStyle.SOLID, new CornerRadii(8), new BorderWidths(1.2))));

        String destination = (card == null) ? "No card" : FetchUi.masked(card);
        hint.setWrapText(true);

        body.setSpacing(12);
        body.getChildren().addAll(
                heading,
                availablePanel,
                choicePanel,
                FetchUi.row("Credit destination", destination),
                FetchUi.panel(hint));

        // No card: the credit has nowhere to go
        if (card == null) {
            body.getChildren().addAll(
                    FetchUi.note("Add an xPress card first. Redeemed credit is collected onto a card."),
                    FetchUi.spacer(),
                    FetchUi.limeButton("Add Card", () -> Navigator.go("addCard")));
        } else {
            body.getChildren().addAll(
                    FetchUi.spacer(),
                    FetchUi.limeButton("Review redemption", () ->
                            Navigator.go(RedeemState.hasEnough() ? "redeemReview" : "redeemNotEnough")));
        }
        body.getChildren().add(FetchUi.link("Cancel", RedeemState::goHome));

        select(RedeemState.getSelected());
    }

    // 100 -> "100 pts", "₱100.00", the 100 tile turns lime, and the hint updates
    private void select(int points) {
        RedeemState.setSelected(points);
        pointsValue.setText(FetchUi.pts(points));
        creditValue.setText(FetchUi.peso(points));
        hint.setText(points + " points is selected. Your points will only be deducted "
                + "after the redemption is confirmed.");

        for (int i = 0; i < choiceLabels.length; i++) {
            boolean on = RedeemState.CHOICES[i] == points;
            Label choice = choiceLabels[i];
            choice.setTextFill(on ? Theme.BLACK : Theme.WHITE);
            choice.setBackground(on ? Theme.rounded(Theme.LIME, 6) : null);
            choice.setBorder(on ? null : CHOICE_BORDER);
        }
    }
}
