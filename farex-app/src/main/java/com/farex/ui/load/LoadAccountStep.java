package com.farex.ui.load;

import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.Border;
import javafx.scene.layout.BorderStroke;
import javafx.scene.layout.BorderStrokeStyle;
import javafx.scene.layout.BorderWidths;
import javafx.scene.layout.CornerRadii;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;

import com.farex.ui.common.Theme;

// Buy Load, step 3 (title "Payment"): the phone or account number for the chosen payment method
public class LoadAccountStep extends VBox {

    private final Runnable onDone;

    private static final Border IDLE_BORDER = new Border(new BorderStroke(
            Color.TRANSPARENT, BorderStrokeStyle.SOLID, new CornerRadii(8), new BorderWidths(1.5)));
    private static final Border FOCUS_BORDER = new Border(new BorderStroke(
            Theme.LIME, BorderStrokeStyle.SOLID, new CornerRadii(8), new BorderWidths(1.5)));

    private final TextField field = new TextField();
    private final Label errorLabel = new Label();
    private final boolean isGcash = "GCash".equals(BuyLoadState.getMethod());

    // onDone runs when the step is done (BuyLoadScreen shows the following step)
    public LoadAccountStep(Runnable onDone) {
        super(16);
        VBox.setVgrow(this, Priority.ALWAYS);
        this.onDone = onDone;
        Label caption = new Label("Enter Phone/Account Number");
        caption.setTextFill(Theme.WHITE);
        caption.setFont(Font.font("System", 12));

        field.setPromptText("Phone/Account Number");
        field.setBackground(Theme.rounded(Theme.FIELD, 8));
        field.setBorder(IDLE_BORDER);
        field.setPadding(new Insets(15, 14, 15, 14));
        field.setFont(Font.font("System", 14));
        field.setStyle("-fx-text-fill: white; -fx-prompt-text-fill: #6F6F6F;");
        field.focusedProperty().addListener((obs, was, now) ->
                field.setBorder(now ? FOCUS_BORDER : IDLE_BORDER));

        // Digits only: GCash numbers are 11 digits, bank/other accounts up to 16
        int maxDigits = isGcash ? 11 : 16;
        field.textProperty().addListener((obs, oldText, newText) -> {
            String digits = newText.replaceAll("\\D", "");
            if (digits.length() > maxDigits) {
                digits = digits.substring(0, maxDigits);
            }
            if (!digits.equals(newText)) {
                field.setText(digits);
                field.positionCaret(digits.length());
            }
            errorLabel.setVisible(false);
        });

        errorLabel.setTextFill(Theme.ERROR);
        errorLabel.setWrapText(true);
        errorLabel.setVisible(false);
        errorLabel.managedProperty().bind(errorLabel.visibleProperty());

        Region spacer = new Region();
        VBox.setVgrow(spacer, Priority.ALWAYS);

        Button next = new Button("Next");
        Theme.limeButton(next);
        next.setMaxWidth(Double.MAX_VALUE);
        next.setOnAction(e -> goNext());
        field.setOnAction(e -> next.fire());

        getChildren().addAll(caption, field, errorLabel, spacer, next);

        if (BuyLoadState.getAccountNumber() != null) {   // coming back from the next step
            field.setText(BuyLoadState.getAccountNumber());
        }
        Platform.runLater(field::requestFocus);
    }

    private void goNext() {
        String number = field.getText();
        if (isGcash && !number.matches("09\\d{9}")) {
            showError("Enter your 11-digit GCash number, for example 09123456789.");
            return;
        }
        if (!isGcash && number.length() < 10) {
            showError("Enter your account number (10 to 16 digits).");
            return;
        }
        BuyLoadState.setAccountNumber(number);
        onDone.run();
    }

    private void showError(String message) {
        errorLabel.setText(message);
        errorLabel.setVisible(true);
    }
}
