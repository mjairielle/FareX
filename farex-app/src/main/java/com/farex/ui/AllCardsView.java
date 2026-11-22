package com.farex.ui;

import java.util.ArrayList;
import java.util.List;

import javafx.geometry.Insets;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

// View All Cards: the first card on top with its balance, the rest below as plain cards
public class AllCardsView extends SubScreen {

    public AllCardsView() {
        super("View All Cards", "dashboard");

        DemoAccount account = MockData.getCurrentAccount();
        List<DemoCard> cards = (account == null) ? new ArrayList<>() : account.getCards();

        VBox list = new VBox(12);
        list.setPadding(new Insets(0, 0, 24, 0));

        // ----- My card -----
        if (cards.isEmpty()) {
            list.getChildren().add(Theme.bodyText("You have no xPress cards yet.", Theme.GRAY));
        } else {
            list.getChildren().addAll(sectionLabel("MY CARD"), new CardTile(cards.get(0)));
        }

        // ----- Other cards -----
        // Example: 3 cards -> cards 1 and 2 (Kenn, Andrei) go here
        if (cards.size() > 1) {
            Label other = sectionLabel("OTHER CARDS");
            VBox.setMargin(other, new Insets(8, 0, 0, 0));
            list.getChildren().add(other);
            for (int i = 1; i < cards.size(); i++) {
                list.getChildren().add(new CardTile(cards.get(i), false));
            }
        }

        ScrollPane scroll = new ScrollPane(list);
        scroll.setFitToWidth(true);
        scroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scroll.setVbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scroll.setStyle("-fx-background: transparent; -fx-background-color: transparent;");
        VBox.setVgrow(scroll, Priority.ALWAYS);

        // Less top space than other sub screens; the scroll area runs to the bottom edge
        body.setPadding(new Insets(16, 20, 0, 20));
        body.getChildren().add(scroll);
    }

    private Label sectionLabel(String text) {
        Label label = new Label(text);
        label.setTextFill(Theme.GRAY);
        label.setFont(Theme.bold(11));
        return label;
    }
}
