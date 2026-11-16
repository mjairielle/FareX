package com.farex.ui;

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
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;

// Profile tab (inside MainShell): who you are, your first card, settings, log out, Operator's Mode
public class ProfileView extends VBox {

    private static final Color ROW = Color.web("#1E1E1E");

    public ProfileView() {
        DemoAccount account = MockData.getCurrentAccount();

        Region push = new Region();
        VBox.setVgrow(push, Priority.ALWAYS);

        Region line = new Region();
        line.setMinHeight(1);
        line.setPrefHeight(1);
        line.setMaxHeight(1);
        line.setBackground(Theme.solid(Color.web("#3A3A3A")));
        VBox.setMargin(line, new Insets(8, 10, 4, 10));

        setSpacing(14);
        setPadding(new Insets(14, 18, 16, 18));
        getChildren().addAll(
                header(account),
                cardPanel(account),
                settingsRow("Account Information", "accountInfo"),
                settingsRow("Privacy & Security", "privacy"),
                line,
                logOutRow(),
                push,
                operatorButton());
    }

    // ----- Photo, name, email, phone -----

    private Node header(DemoAccount account) {
        String name = (account == null) ? "FareX User" : account.getFullName();
        Label nameLabel = new Label(name);
        nameLabel.setTextFill(Theme.WHITE);
        nameLabel.setFont(Theme.bold(17));
        nameLabel.setWrapText(true);

        VBox text = new VBox(2, nameLabel);
        if (account != null) {
            if (!account.getEmail().isEmpty()) {
                text.getChildren().add(FetchUi.small(account.getEmail(), Theme.WHITE));
            }
            if (!account.getPhone().isEmpty()) {
                text.getChildren().add(FetchUi.small(account.getPhone(), Theme.WHITE));
            }
        }

        HBox header = new HBox(18, new Avatar(account, 76), text);
        header.setAlignment(Pos.CENTER_LEFT);
        header.setPadding(new Insets(4, 0, 4, 12));
        return header;
    }

    // ----- First card: logo, "xPress Card", "**** 6767", "₱ 676.50", Active chip -----

    private Node cardPanel(DemoAccount account) {
        DemoCard card = (account == null) ? null : account.getPrimaryCard();

        StackPane logo = new StackPane(Assets.logo(22, 4));
        logo.setMinSize(34, 34);
        logo.setMaxSize(34, 34);
        logo.setBackground(Theme.rounded(Color.web("#0A0A0A"), 6));

        Label title = new Label("xPress Card");
        title.setTextFill(Theme.LIME);
        title.setFont(Theme.bold(12));

        VBox text = new VBox(1, title);
        HBox.setHgrow(text, Priority.ALWAYS);
        Node chip;

        if (card == null) {
            text.getChildren().add(FetchUi.small("No card yet · tap to add one", Theme.WHITE));
            chip = statusChip("Add", Theme.LIME);
        } else {
            Label balance = new Label("₱ " + card.getBalanceText());
            balance.setTextFill(Theme.LIME);
            balance.setFont(Theme.bold(19));
            text.getChildren().addAll(FetchUi.small("**** " + card.getLastFour(), Theme.WHITE), balance);
            chip = card.isFrozen() ? statusChip("Frozen", Theme.ERROR) : statusChip("Active", Theme.LIME);
        }

        HBox panel = new HBox(14, logo, text, chip);
        panel.setAlignment(Pos.CENTER_LEFT);
        panel.setPadding(new Insets(10, 14, 10, 14));
        panel.setBackground(Theme.rounded(Color.web("#1C1C1C"), 4));
        panel.setBorder(new Border(new BorderStroke(Color.web("#3A3A3A"),
                BorderStrokeStyle.SOLID, new CornerRadii(4), new BorderWidths(1))));
        if (card == null) {
            panel.setCursor(Cursor.HAND);
            panel.setOnMouseClicked(e -> Navigator.go("addCard"));
        }
        return panel;
    }

    private Label statusChip(String text, Color color) {
        Label chip = new Label(text);
        chip.setTextFill(Theme.BLACK);
        chip.setFont(Font.font("System", 8));
        chip.setPadding(new Insets(2, 10, 2, 10));
        chip.setBackground(Theme.rounded(color, 50));
        return chip;
    }

    // ----- "Account Information ›" / "Privacy & Security ›" -----

    private Node settingsRow(String text, String target) {
        Label label = new Label(text);
        label.setTextFill(Theme.WHITE);
        label.setFont(Theme.bold(13));
        Region fill = new Region();
        HBox.setHgrow(fill, Priority.ALWAYS);
        Label chevron = new Label("›");
        chevron.setTextFill(Theme.WHITE);
        chevron.setFont(Theme.bold(20));

        HBox row = new HBox(8, label, fill, chevron);
        row.setAlignment(Pos.CENTER_LEFT);
        row.setPadding(new Insets(16, 18, 16, 20));
        row.setBackground(Theme.rounded(ROW, 8));
        row.setCursor(Cursor.HAND);
        row.setOnMouseClicked(e -> Navigator.go(target));
        return row;
    }

    private Node logOutRow() {
        Label label = new Label("Log Out");
        label.setTextFill(Color.web("#FF2B2B"));
        label.setFont(Theme.bold(12));
        label.setMaxWidth(Double.MAX_VALUE);
        label.setAlignment(Pos.CENTER);
        label.setPadding(new Insets(6));
        label.setBackground(Theme.rounded(ROW, 6));
        label.setCursor(Cursor.HAND);
        label.setOnMouseClicked(e -> Navigator.showOverlay(new LogoutDialog()));
        return label;
    }

    private Node operatorButton() {
        Button operator = new Button("OPERATOR’S MODE");
        Theme.limeButton(operator);
        operator.setFont(Theme.bold(12));
        operator.setPadding(new Insets(14));
        operator.setMaxWidth(Double.MAX_VALUE);
        operator.setOnAction(e -> Navigator.go("operatorMode"));
        return operator;
    }
}
