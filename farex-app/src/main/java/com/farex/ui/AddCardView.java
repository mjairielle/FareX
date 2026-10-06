package com.farex.ui;

import javafx.application.Platform;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;

/**
 * Figma "Add a xPress Card": type the last 8 digits, press Save Card.
 * The card is added to the logged-in demo account and Home switches to the "with card" state.
 */
public class AddCardView extends SubScreen {

    // Until the backend supplies the real cardholder name
    private static final String DEMO_HOLDER = "Aron Ferrancullo";

    private final Label errorLabel = new Label();

    public AddCardView() {
        super("Add a xPress Card", "dashboard");

        Label caption = new Label("xPress Card Number (last 8 digits)");
        caption.setTextFill(Theme.WHITE);
        caption.setFont(Font.font("System", 12));

        CardNumberField numberField = new CardNumberField();

        errorLabel.setTextFill(Theme.ERROR);
        errorLabel.setWrapText(true);
        errorLabel.setVisible(false);
        errorLabel.managedProperty().bind(errorLabel.visibleProperty());

        Region spacer = new Region();
        VBox.setVgrow(spacer, Priority.ALWAYS);

        Button saveButton = new Button("Save Card");
        Theme.limeButton(saveButton);
        saveButton.setMaxWidth(Double.MAX_VALUE);
        saveButton.setOnAction(e -> saveCard(numberField));
        numberField.setOnEnter(saveButton::fire);

        body.getChildren().addAll(caption, numberField, errorLabel, spacer, saveButton);
        Platform.runLater(numberField::focusField);
    }

    private void saveCard(CardNumberField numberField) {
        DemoAccount account = MockData.getCurrentAccount();

        if (!numberField.isComplete()) {
            showError("Enter the last 8 digits of your card, for example 6767 6767.");
            return;
        }
        if (account == null) {
            showError("Please log in again.");
            return;
        }

        String fullNumber = numberField.getFullNumber();   // "00" + 8 digits
        for (DemoCard existing : account.getCards()) {
            if (existing.getCardNumber().equals(fullNumber)) {
                showError("This card is already added.");
                return;
            }
        }

        account.addCard(new DemoCard(fullNumber, DEMO_HOLDER, 0.00, "09/29"));
        Navigator.go("dashboard");
    }

    private void showError(String message) {
        errorLabel.setText(message);
        errorLabel.setVisible(true);
    }
}
