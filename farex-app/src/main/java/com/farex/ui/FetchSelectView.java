package com.farex.ui;

import javafx.geometry.Insets;
import javafx.scene.Cursor;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;

// Fetch Load / 01: shows the card and the paid top-up waiting to be collected
public class FetchSelectView extends SubScreen {

    public FetchSelectView() {
        super("Fetch Load", "dashboard");

        DemoCard card = FetchLoadState.getCard();
        PendingLoad load = card.getPendingLoad();

        Label title = FetchUi.title("Fetch your pending load");
        Label subtitle = FetchUi.subtitle(
                "Move paid load onto your xPress card. Choose the card and top-up to collect.");
        VBox heading = new VBox(8, title, subtitle);
        VBox.setMargin(heading, new Insets(0, 0, 6, 0));

        // ----- Selected card -----
        Label change = FetchUi.smallBold("Change", Theme.LIME);
        change.setCursor(Cursor.HAND);
        change.setOnMouseClicked(e -> FetchLoadState.pickCard());

        Label holder = new Label(card.getHolderName());
        holder.setTextFill(Theme.WHITE);
        holder.setFont(Theme.bold(14));

        VBox cardPanel = FetchUi.panel(
                FetchUi.row("Selected card", change),
                holder,
                FetchUi.row("xPress " + FetchUi.masked(card), FetchUi.peso(card.getBalance())));

        // ----- Selected top-up -----
        Label ready = FetchUi.small(FetchUi.paidVia(load.getMethod()) + " • Ready to fetch", Theme.LIME);

        VBox loadPanel = FetchUi.panel(
                FetchUi.row("Selected top-up", FetchUi.peso(load.getAmount())),
                ready,
                FetchUi.row("Reference", load.getReference()));

        // ----- Buttons -----
        body.setSpacing(12);
        body.getChildren().addAll(
                heading,
                cardPanel,
                loadPanel,
                FetchUi.note("Keep your physical card nearby and turn on NFC. You will not be charged again."),
                FetchUi.spacer(),
                FetchUi.limeButton("Continue to fetch", () -> Navigator.go("fetchHold")),
                FetchUi.link("Cancel", () -> {
                    FetchLoadState.clear();
                    Navigator.go("dashboard");
                }));
    }
}
