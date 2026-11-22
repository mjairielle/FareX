package com.farex.ui.load;

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

import com.farex.ui.common.CommonUi;
import com.farex.ui.common.Icons;
import com.farex.ui.common.Theme;

// Buy Load, step 2 (title "Payment"): choose how to pay
public class LoadMethodStep extends VBox {

    private final Runnable onDone;

    private static final Border OUTLINE = new Border(new BorderStroke(
            Theme.LIME, BorderStrokeStyle.SOLID, new CornerRadii(6), new BorderWidths(1.5)));
    private static final Border NO_OUTLINE = new Border(new BorderStroke(
            Color.TRANSPARENT, BorderStrokeStyle.SOLID, new CornerRadii(6), new BorderWidths(1.5)));

    // onDone runs when the step is done (BuyLoadScreen shows the following step)
    public LoadMethodStep(Runnable onDone) {
        super(16);
        VBox.setVgrow(this, Priority.ALWAYS);
        this.onDone = onDone;
        Label caption = new Label("Payment method(s)");
        caption.setTextFill(Theme.WHITE);
        caption.setFont(Theme.bold(13));

        VBox rows = new VBox(20,
                methodRow("GCash", "Mobile number", "icon_gcash", "#012CB7"),
                methodRow("MariBank", "Bank account", "icon_maribank", "#EA5F00"),
                methodRow("BPI", "Bank account", "icon_bpi", "#A42A25"),
                methodRow("Others", "Debit/Credit Card/e-Wallet", "icon_others", "#FFFFFF"));

        getChildren().addAll(caption, rows, CommonUi.note("You approve every payment on PayMongo's secure page, "
                + "in your GCash, bank or card app. FareX never sees your PIN or password."));
    }

    // The chosen method as a small logo + name, for the steps after this one: "[logo] GCash"
    static HBox badge(String name) {
        String icon = switch (name) {
            case "GCash" -> "icon_gcash";
            case "MariBank" -> "icon_maribank";
            case "BPI" -> "icon_bpi";
            default -> "icon_others";
        };
        Label label = new Label("Others".equals(name) ? "Card / e-Wallet" : name);
        label.setTextFill(Theme.WHITE);
        label.setFont(Theme.bold(16));
        HBox badge = new HBox(12, Icons.view(icon, 28), label);
        badge.setAlignment(Pos.CENTER_LEFT);
        return badge;
    }

    // One row: logo, name with a line under it, chevron
    private HBox methodRow(String name, String subtitle, String iconName, String nameColor) {
        Label nameLabel = new Label(name);
        nameLabel.setTextFill(Color.web(nameColor));
        nameLabel.setFont(Theme.bold(18));

        Label sub = new Label(subtitle);
        sub.setTextFill(Theme.GRAY);
        sub.setFont(Font.font(Theme.FONT, 10));
        VBox text = new VBox(-2, nameLabel, sub);
        text.setAlignment(Pos.CENTER_LEFT);

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
            onDone.run();
        });
        return row;
    }
}
