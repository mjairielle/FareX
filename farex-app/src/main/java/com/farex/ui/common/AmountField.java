package com.farex.ui.common;

import java.util.Locale;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.Background;
import javafx.scene.layout.Border;
import javafx.scene.layout.BorderStroke;
import javafx.scene.layout.BorderStrokeStyle;
import javafx.scene.layout.BorderWidths;
import javafx.scene.layout.CornerRadii;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;

// Peso amount box: shows "₱0.00" when empty, takes digits with up to 2 decimals
public class AmountField extends HBox {

    private static final Border IDLE_BORDER = new Border(new BorderStroke(
            Color.TRANSPARENT, BorderStrokeStyle.SOLID, new CornerRadii(8), new BorderWidths(1.5)));
    private static final Border FOCUS_BORDER = new Border(new BorderStroke(
            Theme.LIME, BorderStrokeStyle.SOLID, new CornerRadii(8), new BorderWidths(1.5)));

    private final TextField field = new TextField();
    private Runnable onChange;

    public AmountField() {
        setAlignment(Pos.CENTER_LEFT);
        setSpacing(1);
        setPadding(new Insets(0, 14, 0, 14));
        setBackground(Theme.rounded(Theme.FIELD, 8));
        setBorder(IDLE_BORDER);

        // The peso sign only shows once something is typed; the prompt has its own
        Label peso = new Label("₱");
        peso.setTextFill(Theme.WHITE);
        peso.setFont(Font.font(Theme.FONT, 14));
        peso.managedProperty().bind(peso.visibleProperty());
        peso.setVisible(false);

        field.setPromptText("₱0.00");
        field.setBackground(Background.EMPTY);
        field.setBorder(null);
        field.setPadding(new Insets(13, 0, 13, 0));
        field.setFont(Font.font(Theme.FONT, 14));
        field.setStyle("-fx-text-fill: white; -fx-prompt-text-fill: #6F6F6F;");
        HBox.setHgrow(field, Priority.ALWAYS);

        getChildren().addAll(peso, field);

        // "12", "12.5", "4999.99" are fine; "12.555", "12345" or letters are refused
        field.textProperty().addListener((obs, oldText, newText) -> {
            if (!newText.matches("\\d{0,4}(\\.\\d{0,2})?")) {
                field.setText(oldText);
                return;
            }
            peso.setVisible(!newText.isEmpty());
            if (onChange != null) {
                onChange.run();
            }
        });

        field.focusedProperty().addListener((obs, was, now) ->
                setBorder(now ? FOCUS_BORDER : IDLE_BORDER));
    }

    // -1 when the box is empty or just "."
    public double getAmount() {
        String text = field.getText();
        if (text.isEmpty() || text.equals(".")) {
            return -1;
        }
        return Double.parseDouble(text);
    }

    // 200 -> "200.00"
    public void setAmount(double amount) {
        field.setText(String.format(Locale.US, "%.2f", amount));
        field.positionCaret(field.getText().length());
    }

    public void setOnChange(Runnable action) {
        onChange = action;
    }

    public void setOnEnter(Runnable action) {
        field.setOnAction(e -> action.run());
    }

    public void focusField() {
        field.requestFocus();
    }
}
