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
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;

// Buy Load, step 2: choose how to pay
public class PaymentMethodView extends SubScreen {

    private static final Border OUTLINE = new Border(new BorderStroke(
            Theme.LIME, BorderStrokeStyle.SOLID, new CornerRadii(6), new BorderWidths(1.5)));
    private static final Border NO_OUTLINE = new Border(new BorderStroke(
            Color.TRANSPARENT, BorderStrokeStyle.SOLID, new CornerRadii(6), new BorderWidths(1.5)));

    public PaymentMethodView() {
        super("Payment", "buyLoad");

        Label caption = new Label("Payment method(s)");
        caption.setTextFill(Theme.WHITE);
        caption.setFont(Theme.bold(13));

        VBox rows = new VBox(20,
                methodRow("GCash", null, "icon_gcash", "#012CB7"),
                methodRow("MariBank", null, "icon_maribank", "#EA5F00"),
                methodRow("BPI", null, "icon_bpi", "#A42A25"),
                methodRow("Others", "Debit/Credit Card/e-Wallet", "icon_others", "#FFFFFF"));

        body.getChildren().addAll(caption, rows);
    }

    // One row: logo, name (and an optional line under it), chevron
    private HBox methodRow(String name, String subtitle, String iconName, String nameColor) {
        Label nameLabel = new Label(name);
        nameLabel.setTextFill(Color.web(nameColor));
        nameLabel.setFont(Theme.bold(subtitle == null ? 20 : 18));

        VBox text = new VBox(-2, nameLabel);
        text.setAlignment(Pos.CENTER_LEFT);
        if (subtitle != null) {
            Label sub = new Label(subtitle);
            sub.setTextFill(Theme.WHITE);
            sub.setFont(Font.font("System", 10));
            text.getChildren().add(sub);
        }

        Region gap = new Region();
        HBox.setHgrow(gap, Priority.ALWAYS);

        Label chevron = new Label("›");
        chevron.setTextFill(Theme.WHITE);
        chevron.setFont(Theme.bold(22));

        HBox row = new HBox(20, Icons.view(iconName, 34), text, gap, chevron);
        row.setAlignment(Pos.CENTER_LEFT);
        row.setPadding(new Insets(0, 20, 0, 24));
        row.setMinHeight(58);
        row.setPrefHeight(58);
        row.setBackground(Theme.rounded(Color.web("#201F1F"), 6));
        row.setCursor(Cursor.HAND);

        // Lime outline on hover, and on the method picked before (when coming back)
        Runnable paint = () -> row.setBorder(
                row.isHover() || name.equals(BuyLoadState.getMethod()) ? OUTLINE : NO_OUTLINE);
        paint.run();
        row.hoverProperty().addListener((obs, was, now) -> paint.run());

        row.setOnMouseClicked(e -> {
            if (!name.equals(BuyLoadState.getMethod())) {
                BuyLoadState.setAccountNumber(null);   // a new method needs a new number
            }
            BuyLoadState.setMethod(name);
            Navigator.go("loadAccount");
        });
        return row;
    }
}
