package com.farex.ui.load;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import javafx.application.Platform;
import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.Border;
import javafx.scene.layout.BorderStroke;
import javafx.scene.layout.BorderStrokeStyle;
import javafx.scene.layout.BorderWidths;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.CornerRadii;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;

import com.farex.ui.common.AmountField;
import com.farex.ui.common.CardTile;
import com.farex.ui.common.Theme;
import com.farex.ui.data.DemoCard;

// Buy Load, step 4 (title "Buy Load"): how much to load
public class LoadAmountStep extends VBox {

    private final Runnable onDone;

    private static final double MIN = 5.00;
    private static final double MAX = 5000.00;
    private static final double[] PRESETS = {10, 15, 20, 50, 100, 200, 300, 500};

    private static final Border PRESET_IDLE = new Border(new BorderStroke(
            Color.web("#0A0A0A"), BorderStrokeStyle.SOLID, new CornerRadii(6), new BorderWidths(1.5)));
    private static final Border PRESET_PICKED = new Border(new BorderStroke(
            Theme.LIME, BorderStrokeStyle.SOLID, new CornerRadii(6), new BorderWidths(1.5)));

    private final AmountField amountField = new AmountField();
    private final List<Label> presetBoxes = new ArrayList<>();
    private final Label errorLabel = new Label();

    // onDone runs when the step is done (BuyLoadScreen shows the following step)
    public LoadAmountStep(Runnable onDone) {
        super(16);
        VBox.setVgrow(this, Priority.ALWAYS);
        this.onDone = onDone;
        DemoCard card = BuyLoadState.getCard();
        if (card != null) {
            getChildren().add(new CardTile(card));
        }

        Label caption = new Label("Enter desired load amount");
        caption.setTextFill(Theme.WHITE);
        caption.setFont(Font.font("System", 12));

        Label helper = new Label("From ₱5.00 to ₱5,000.00 only");
        helper.setTextFill(Theme.WHITE);
        helper.setFont(Font.font("System", 10));

        errorLabel.setTextFill(Theme.ERROR);
        errorLabel.setWrapText(true);
        errorLabel.setVisible(false);
        errorLabel.managedProperty().bind(errorLabel.visibleProperty());

        VBox fieldGroup = new VBox(6, amountField, helper, errorLabel);

        Region divider = new Region();
        divider.setMinHeight(1);
        divider.setPrefHeight(1);
        divider.setMaxHeight(1);
        divider.setBackground(Theme.solid(Color.web("#333333")));

        Region spacer = new Region();
        VBox.setVgrow(spacer, Priority.ALWAYS);

        Button next = new Button("Next");
        Theme.limeButton(next);
        next.setMaxWidth(Double.MAX_VALUE);
        next.setOnAction(e -> goNext());
        amountField.setOnEnter(next::fire);

        // Typed or tapped, the matching preset gets the lime outline
        amountField.setOnChange(() -> {
            errorLabel.setVisible(false);
            highlightPreset();
        });

        getChildren().addAll(caption, fieldGroup, divider, presetGrid(), spacer, next);

        if (BuyLoadState.getAmount() > 0) {   // coming back from the next step
            amountField.setAmount(BuyLoadState.getAmount());
        }
        Platform.runLater(amountField::focusField);
    }

    // 4 columns x 2 rows of equal-width boxes
    private GridPane presetGrid() {
        GridPane grid = new GridPane();
        grid.setHgap(8);
        grid.setVgap(10);
        for (int col = 0; col < 4; col++) {
            ColumnConstraints column = new ColumnConstraints();
            column.setPercentWidth(25);
            grid.getColumnConstraints().add(column);
        }

        for (int i = 0; i < PRESETS.length; i++) {
            double value = PRESETS[i];
            Label box = new Label(String.format(Locale.US, "₱%.2f", value));   // "₱10.00"
            box.setTextFill(Color.web("#D6D6D6"));
            box.setFont(Font.font("System", 13));
            box.setAlignment(Pos.CENTER);
            box.setMaxWidth(Double.MAX_VALUE);
            box.setMinHeight(52);
            box.setPrefHeight(52);
            box.setBackground(Theme.rounded(Color.web("#201F1F"), 6));
            box.setBorder(PRESET_IDLE);
            box.setCursor(Cursor.HAND);
            box.setOnMouseClicked(e -> amountField.setAmount(value));
            presetBoxes.add(box);
            grid.add(box, i % 4, i / 4);   // preset 0..3 on row 0, 4..7 on row 1
        }
        return grid;
    }

    private void highlightPreset() {
        double amount = amountField.getAmount();
        for (int i = 0; i < PRESETS.length; i++) {
            presetBoxes.get(i).setBorder(amount == PRESETS[i] ? PRESET_PICKED : PRESET_IDLE);
        }
    }

    private void goNext() {
        double amount = amountField.getAmount();
        if (amount < MIN || amount > MAX) {
            errorLabel.setText("Enter an amount from ₱5.00 to ₱5,000.00.");
            errorLabel.setVisible(true);
            return;
        }
        BuyLoadState.setAmount(amount);
        onDone.run();
    }
}
