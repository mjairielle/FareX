package com.farex.ui;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.Border;
import javafx.scene.layout.BorderStroke;
import javafx.scene.layout.BorderStrokeStyle;
import javafx.scene.layout.BorderWidths;
import javafx.scene.layout.CornerRadii;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;

/**
 * The "00 | 6767 6767" box from the Figma. "00" is fixed, you type the last 8 digits.
 * Typing "67676767" shows "6767 6767"; letters are ignored; more than 8 digits are cut off.
 * Used by Add Card now and Buy Load later.
 */
public class CardNumberField extends HBox {

    private static final String PREFIX = "00";

    private static final Border IDLE_BORDER = new Border(new BorderStroke(
            Color.TRANSPARENT, BorderStrokeStyle.SOLID, new CornerRadii(8), new BorderWidths(1.5)));
    private static final Border FOCUS_BORDER = new Border(new BorderStroke(
            Theme.LIME, BorderStrokeStyle.SOLID, new CornerRadii(8), new BorderWidths(1.5)));

    private final TextField field = new TextField();

    public CardNumberField() {
        setAlignment(Pos.CENTER_LEFT);
        setSpacing(12);
        setPadding(new Insets(0, 14, 0, 14));
        setBackground(Theme.rounded(Theme.FIELD, 8));
        setBorder(IDLE_BORDER);

        Label prefix = new Label(PREFIX);
        prefix.setTextFill(Theme.WHITE);
        prefix.setFont(Font.font("System", 14));

        Region divider = new Region();
        divider.setMinSize(1, 18);
        divider.setPrefSize(1, 18);
        divider.setMaxSize(1, 18);
        divider.setBackground(Theme.solid(Color.web("#CFCFCF")));

        field.setPromptText("0000 0000");
        field.setBackground(javafx.scene.layout.Background.EMPTY);
        field.setBorder(null);
        field.setPadding(new Insets(15, 0, 15, 0));
        field.setFont(Font.font("System", 14));
        field.setStyle("-fx-text-fill: white; -fx-prompt-text-fill: #6F6F6F;");
        HBox.setHgrow(field, Priority.ALWAYS);

        getChildren().addAll(prefix, divider, field);

        // Keep only digits, max 8, and put a space after the 4th: "67676767" -> "6767 6767"
        field.textProperty().addListener((obs, oldText, newText) -> {
            String digits = newText.replaceAll("\\D", "");
            if (digits.length() > 8) {
                digits = digits.substring(0, 8);
            }
            String formatted = (digits.length() > 4)
                    ? digits.substring(0, 4) + " " + digits.substring(4)
                    : digits;
            if (!formatted.equals(newText)) {
                field.setText(formatted);
                field.positionCaret(formatted.length());
            }
        });

        field.focusedProperty().addListener((obs, was, now) ->
                setBorder(now ? FOCUS_BORDER : IDLE_BORDER));
    }

    /** Just the typed digits, e.g. "67676767" (0 to 8 characters). */
    public String getDigits() {
        return field.getText().replaceAll("\\D", "");
    }

    /** The full 10-digit card number, e.g. "0067676767". Only call when isComplete() is true. */
    public String getFullNumber() {
        return PREFIX + getDigits();
    }

    public boolean isComplete() {
        return getDigits().length() == 8;
    }

    /** Fills the box, e.g. setDigits("67676767"). */
    public void setDigits(String digits) {
        field.setText(digits);
    }

    public void setOnEnter(Runnable action) {
        field.setOnAction(e -> action.run());
    }

    public void focusField() {
        field.requestFocus();
    }
}
