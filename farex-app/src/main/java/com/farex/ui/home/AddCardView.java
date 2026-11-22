package com.farex.ui.home;

import javafx.application.Platform;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;

import com.farex.ui.common.CardNumberField;
import com.farex.ui.common.CardTaps;
import com.farex.ui.common.CommonUi;
import com.farex.ui.common.Navigator;
import com.farex.ui.common.SubScreen;
import com.farex.ui.common.Theme;
import com.farex.ui.data.DemoAccount;
import com.farex.ui.data.DemoCard;
import com.farex.Api;
import com.farex.ui.data.MockData;

public class AddCardView extends SubScreen {

    // Test account

    private final Label errorLabel = new Label();

    public AddCardView() {
        super("Add a xPress Card", "dashboard");

        Label caption = new Label("xPress Card Number (last 8 digits)");
        caption.setTextFill(Theme.WHITE);
        caption.setFont(Font.font(Theme.FONT, 12));

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

        body.getChildren().addAll(caption, numberField, errorLabel,
                CommonUi.note("Or just tap your card on the back of the phone: it is added by its chip ID."),
                spacer, saveButton);
        Platform.runLater(numberField::focusField);
        // On a phone, tapping the card links it by its chip UID: no typing
        CardTaps.claim(this, uid -> link(uid, uid));
    }

    private void saveCard(CardNumberField numberField) {
        if (!numberField.isComplete()) {
            showError("Enter the last 8 digits of your card, for example 6767 6767.");
            return;
        }
        link(numberField.getDigits(), numberField.getFullNumber());   // demo number: "00" + 8 digits
    }

    // uid: what the server knows the card by; demoNumber: the card number in demo mode
    private void link(String uid, String demoNumber) {
        DemoAccount account = MockData.getCurrentAccount();
        if (account == null) {
            showError("Please log in again.");
            return;
        }
        for (DemoCard existing : account.getCards()) {
            if (existing.getLast8().equalsIgnoreCase(uid) || existing.getCardNumber().equalsIgnoreCase(uid)) {
                showError("This card is already added.");
                return;
            }
        }

        if (MockData.isLive()) {
            try {
                Api.addCard(uid);                       // card id on the server = the chip UID (8 or 14 hex)
                MockData.refresh();
            } catch (Api.ApiException e) {
                showError(e.getMessage());               // e.g. "Card is already linked to an account"
                return;
            }
        } else {
            account.addCard(new DemoCard(demoNumber, account.getFullName(), 0.00, "09/29"));
        }
        Navigator.go("dashboard");
    }

    private void showError(String message) {
        errorLabel.setText(message);
        errorLabel.setVisible(true);
    }
}
