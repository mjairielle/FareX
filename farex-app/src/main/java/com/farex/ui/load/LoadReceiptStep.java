package com.farex.ui.load;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.text.Font;

import com.farex.ui.common.CheckRing;
import com.farex.ui.common.Navigator;
import com.farex.ui.common.Theme;
import com.farex.ui.data.DemoCard;

// Buy Load, last step (title "Load Receipt"): the receipt after a successful load
public class LoadReceiptStep extends VBox {

    // "Oct 26, 2026 • 4:05 PM"
    private static final DateTimeFormatter WHEN =
            DateTimeFormatter.ofPattern("MMM d, yyyy • h:mm a", Locale.US);
    private static final double RING = 104;
    private static final double SERVICE_FEE = 0.00;

    public LoadReceiptStep() {
        super(16);
        VBox.setVgrow(this, Priority.ALWAYS);

        DemoCard card = BuyLoadState.getCard();
        double amount = BuyLoadState.getAmount();

        // ----- Status -----
        Label statusCaption = new Label("TRANSACTION STATUS");
        statusCaption.setTextFill(Color.web("#E6E6E6"));
        statusCaption.setFont(Theme.bold(10));
        Label successful = new Label("Successful");
        successful.setTextFill(Theme.WHITE);
        successful.setFont(Theme.bold(17));
        HBox statusLine = new HBox(8, new Circle(4, Theme.LIME), successful);
        statusLine.setAlignment(Pos.CENTER_LEFT);
        VBox status = new VBox(2, statusCaption, statusLine);
        status.setPadding(new Insets(16, 18, 15, 18));

        // ----- Details -----
        HBox dateRow = row("Date & Time", LocalDateTime.now().format(WHEN));
        VBox.setMargin(dateRow, new Insets(53, 0, 0, 0));
        VBox details = new VBox(7,
                row("xPress Card Number", card.getCardNumber()),
                row("Amount", peso(amount)),
                row("Payment Method", methodValue()),
                row("Service Fee", peso(SERVICE_FEE)),
                dateRow);
        details.setPadding(new Insets(22, 18, 20, 18));

        // ----- Total -----
        Label totalLabel = new Label("Total Deducted");
        totalLabel.setTextFill(Theme.WHITE);
        totalLabel.setFont(Theme.bold(16));
        Label totalAmount = new Label(peso(amount + SERVICE_FEE));
        totalAmount.setTextFill(Theme.LIME);
        totalAmount.setFont(Theme.bold(21));
        Region fill = new Region();
        HBox.setHgrow(fill, Priority.ALWAYS);
        HBox total = new HBox(8, totalLabel, fill, totalAmount);
        total.setAlignment(Pos.CENTER_LEFT);
        total.setPadding(new Insets(22, 18, RING / 2 + 24, 18));   // room for the ring's top half

        VBox panel = new VBox(status, line(), details, line(), total);
        panel.setBackground(Theme.rounded(Color.web("#201F1F"), 6));

        // The ring sits on the panel's bottom edge: half inside, half below
        CheckRing ring = new CheckRing(RING, Theme.DARK);
        VBox.setMargin(ring, new Insets(-RING / 2, 0, 0, 0));
        VBox receipt = new VBox(panel, ring);
        receipt.setAlignment(Pos.TOP_CENTER);
        VBox.setMargin(receipt, new Insets(28, 12, 0, 12));

        Region spacer = new Region();
        VBox.setVgrow(spacer, Priority.ALWAYS);

        Button done = new Button("Done");
        Theme.limeButton(done);
        done.setMaxWidth(Double.MAX_VALUE);
        done.setOnAction(e -> {
            BuyLoadState.clear();
            Navigator.go("dashboard");
        });

        getChildren().addAll(receipt, spacer, done);
    }

    // "GCash" with the number in small text under it
    private VBox methodValue() {
        Label method = new Label(BuyLoadState.getMethod());
        method.setTextFill(Theme.WHITE);
        method.setFont(Theme.bold(13));
        Label number = new Label(BuyLoadState.getAccountNumber());
        number.setTextFill(Color.web("#D6D6D6"));
        number.setFont(Font.font("System", 9));
        VBox value = new VBox(0, method, number);
        value.setAlignment(Pos.CENTER_RIGHT);
        return value;
    }

    private HBox row(String label, String value) {
        Label text = new Label(value);
        text.setTextFill(Theme.WHITE);
        text.setFont(Theme.bold(13));
        return row(label, text);
    }

    private HBox row(String label, Node value) {
        Label left = new Label(label);
        left.setTextFill(Color.web("#E6E6E6"));
        left.setFont(Font.font("System", 13));
        Region fill = new Region();
        HBox.setHgrow(fill, Priority.ALWAYS);
        HBox row = new HBox(8, left, fill, value);
        row.setAlignment(Pos.CENTER_LEFT);
        return row;
    }

    private Region line() {
        Region line = new Region();
        line.setMinHeight(1);
        line.setPrefHeight(1);
        line.setMaxHeight(1);
        line.setBackground(Theme.solid(Color.web("#2C2C2C")));
        return line;
    }

    // 200 -> "₱ 200.00" (the receipt puts a space after the sign, like the Figma)
    private String peso(double value) {
        return String.format(Locale.US, "₱ %,.2f", value);
    }
}
