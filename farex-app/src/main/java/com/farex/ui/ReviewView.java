package com.farex.ui;

import java.util.Locale;

import javafx.geometry.Insets;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.effect.DropShadow;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;

// Buy Load, step 5: check everything before paying
public class ReviewView extends SubScreen {

    private static final double SERVICE_FEE = 0.00;

    public ReviewView() {
        super("", "loadAmount");

        DemoCard card = BuyLoadState.getCard();
        double amount = BuyLoadState.getAmount();
        String method = BuyLoadState.getMethod() + " (" + BuyLoadState.getAccountNumber() + ")";

        Label title = new Label("Review Payment Details");
        title.setTextFill(Theme.WHITE);
        title.setFont(Theme.bold(17));
        Label subtitle = new Label("Review your detail before proceeding to buy a xPress Load");
        subtitle.setTextFill(Color.web("#E0E0E0"));
        subtitle.setFont(Font.font("System", 10));
        VBox heading = new VBox(2, title, subtitle);

        Region divider = new Region();
        divider.setMinHeight(1);
        divider.setPrefHeight(1);
        divider.setMaxHeight(1);
        divider.setBackground(Theme.solid(Color.web("#333333")));

        // Gaps copy the Figma panel's spacing
        VBox panel = new VBox(
                row("xPress Card Number", card.getCardNumber(), true),
                gap(44),
                row("Amount", peso(amount), false),
                gap(6),
                row("Payment Method", method, false),
                gap(84),
                row("Service Fee", peso(SERVICE_FEE), false),
                gap(26),
                row("Total Amount", peso(amount + SERVICE_FEE), false));
        panel.setPadding(new Insets(26, 18, 26, 18));
        panel.setBackground(Theme.rounded(Color.web("#201F1F"), 6));
        panel.setEffect(new DropShadow(14, Color.rgb(0, 0, 0, 0.5)));
        VBox.setMargin(panel, new Insets(30, 12, 0, 12));

        Region spacer = new Region();
        VBox.setVgrow(spacer, Priority.ALWAYS);

        Button confirm = new Button("Confirm");
        Theme.limeButton(confirm);
        confirm.setMaxWidth(Double.MAX_VALUE);
        confirm.setOnAction(e -> Navigator.go("loadOtp"));

        body.getChildren().addAll(heading, divider, panel, spacer, confirm);
    }

    // "Amount ............ ₱200.00"
    private HBox row(String label, String value, boolean bold) {
        FontWeight weight = bold ? FontWeight.BOLD : FontWeight.NORMAL;
        Label left = new Label(label);
        left.setTextFill(Theme.WHITE);
        left.setFont(Font.font("System", weight, 12));
        Label right = new Label(value);
        right.setTextFill(Theme.WHITE);
        right.setFont(Font.font("System", weight, 12));
        Region fill = new Region();
        HBox.setHgrow(fill, Priority.ALWAYS);
        return new HBox(8, left, fill, right);
    }

    private Region gap(double height) {
        Region gap = new Region();
        gap.setMinHeight(height);
        gap.setPrefHeight(height);
        return gap;
    }

    // 200 -> "₱200.00", 1000 -> "₱1,000.00"
    private String peso(double value) {
        return String.format(Locale.US, "₱%,.2f", value);
    }
}
