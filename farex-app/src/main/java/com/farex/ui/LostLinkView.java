package com.farex.ui;

import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.scene.control.Label;
import javafx.scene.layout.Border;
import javafx.scene.layout.BorderStroke;
import javafx.scene.layout.BorderStrokeStyle;
import javafx.scene.layout.BorderWidths;
import javafx.scene.layout.CornerRadii;
import javafx.scene.layout.VBox;

// Lost Card / 08: type the new card's last 8 digits; the details fill in once all 8 are there
public class LostLinkView extends SubScreen {

    private final CardNumberField numberField = new CardNumberField();
    private final Label uidValue = FetchUi.smallBold("", Theme.WHITE);
    private final Label checkValue = FetchUi.smallBold("", Theme.WHITE);
    private final Label errorLabel = new Label();

    public LostLinkView() {
        super("Replace or Report Lost Card", "lostReplace");

        VBox heading = new VBox(8,
                FetchUi.title("Link your new card"),
                FetchUi.subtitle("Enter the last 8 digits, then hold the issued replacement card "
                        + "to your phone to verify its UID."));
        VBox.setMargin(heading, new Insets(0, 0, 6, 0));

        // ----- Number box (lime outline) -----
        VBox numberPanel = FetchUi.panel(FetchUi.small("xPress card number (last 8 digits)", Theme.WHITE), numberField);
        numberPanel.setBorder(new Border(new BorderStroke(Theme.LIME,
                BorderStrokeStyle.SOLID, new CornerRadii(8), new BorderWidths(1.2))));

        errorLabel.setTextFill(Theme.ERROR);
        errorLabel.setWrapText(true);
        errorLabel.setVisible(false);
        errorLabel.managedProperty().bind(errorLabel.visibleProperty());

        VBox details = FetchUi.panel(
                FetchUi.row("Issuance status", "Issued by staff"),
                FetchUi.row("New card UID", uidValue),
                FetchUi.row("Card check", checkValue),
                FetchUi.row("Starting balance", FetchUi.peso(0)));

        numberField.setOnDigitsChanged(digits -> refresh());
        numberField.setOnEnter(this::next);

        // Coming back from the review: keep the number typed before
        String saved = LostCardState.getNewCardNumber();
        if (!saved.isEmpty()) {
            numberField.setDigits(saved.substring(2));
        }
        refresh();

        body.setSpacing(12);
        body.getChildren().addAll(
                heading,
                numberPanel,
                errorLabel,
                details,
                FetchUi.note("Use a new, unused card issued to you. If the card number or UID does not match, "
                        + "scan it again before continuing."),
                FetchUi.spacer(),
                FetchUi.limeButton("Review balance transfer", this::next),
                FetchUi.link("Use another card", () -> {
                    numberField.setDigits("");
                    numberField.focusField();
                }));

        Platform.runLater(numberField::focusField);
    }

    // "8842 8842" typed -> UID "•••• 8842", check "Verified"; fewer digits -> waiting
    private void refresh() {
        errorLabel.setVisible(false);
        if (numberField.isComplete()) {
            String digits = numberField.getDigits();
            uidValue.setText("•••• " + digits.substring(4));
            checkValue.setText("Verified");
            checkValue.setTextFill(Theme.LIME);
        } else {
            uidValue.setText("—");
            checkValue.setText("Waiting for 8 digits");
            checkValue.setTextFill(Theme.GRAY);
        }
    }

    private void next() {
        if (!numberField.isComplete()) {
            showError("Enter the last 8 digits of the new card, for example 8842 8842.");
            return;
        }
        String fullNumber = numberField.getFullNumber();
        DemoAccount account = MockData.getCurrentAccount();
        if (account != null && account.hasCardNumber(fullNumber)) {
            showError("This card is already linked to your account. Use a new card.");
            return;
        }
        LostCardState.setNewCardNumber(fullNumber);
        Navigator.go("lostTransferReview");
    }

    private void showError(String message) {
        errorLabel.setText(message);
        errorLabel.setVisible(true);
    }
}
