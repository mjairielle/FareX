package com.farex.ui;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import javafx.scene.text.TextAlignment;

// Fetch Load / 06: the chosen card has no paid top-ups waiting
public class FetchNoneView extends SubScreen {

    public FetchNoneView() {
        // Figma has no back arrow here, but this screen opens straight from Home,
        // so the arrow stays as the way back
        super("Fetch Load", "dashboard");

        DemoCard card = FetchLoadState.getCard();

        // ----- Message -----
        Label title = FetchUi.title("No pending load");
        title.setFont(Theme.bold(20));
        Label text = FetchUi.subtitle("There are no paid top-ups waiting to be collected for this card.");
        text.setTextAlignment(TextAlignment.CENTER);
        text.setMaxWidth(260);

        VBox message = new VBox(12, WarningDialog.warningIcon(), title, text);
        message.setAlignment(Pos.TOP_CENTER);
        VBox.setMargin(message, new Insets(46, 0, 0, 0));

        // ----- Card details -----
        VBox details = (card == null)
                ? FetchUi.note("You have no xPress card yet.")
                : FetchUi.panel(
                        FetchUi.row("xPress card", FetchUi.masked(card)),
                        FetchUi.row("Current balance", FetchUi.peso(card.getBalance())),
                        FetchUi.row("Pending load", FetchUi.peso(0)));

        body.setSpacing(12);
        body.getChildren().addAll(
                message,
                FetchUi.spacer(),
                details,
                FetchUi.note("Just paid? Refresh the status before purchasing again. "
                        + "You can also check another linked card."),
                // Rebuilds the screen: if a load is pending now, it opens "Fetch your pending load"
                FetchUi.limeButton("Refresh status", () -> Navigator.go("fetchCurrent")),
                FetchUi.link("Choose another card", FetchLoadState::pickCard));
    }
}
