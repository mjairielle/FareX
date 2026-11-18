package com.farex.ui.lostcard;

import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.scene.Cursor;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.layout.Border;
import javafx.scene.layout.BorderStroke;
import javafx.scene.layout.BorderStrokeStyle;
import javafx.scene.layout.BorderWidths;
import javafx.scene.layout.CornerRadii;
import javafx.scene.layout.VBox;

import com.farex.ui.common.CardNumberField;
import com.farex.ui.common.CardPickerDialog;
import com.farex.ui.common.CommonUi;
import com.farex.ui.common.Navigator;
import com.farex.ui.common.PopupDialog;
import com.farex.ui.common.StepScreen;
import com.farex.ui.common.Theme;
import com.farex.ui.common.WarningDialog;
import com.farex.ui.data.DemoAccount;
import com.farex.ui.data.DemoCard;
import com.farex.ui.data.MockData;

// Replace or Report Lost Card, all in one screen (opened from Home).
// Report:  choose card + reason (01) -> [confirm pop-up (02)] -> verify code (03) -> freezing (04)
//          -> [reported pop-up (05) or not confirmed pop-up (06)]
// Replace: get a replacement (07) -> link the new card (08) -> review transfer (09) -> transferring (10)
//          -> [replacement complete pop-up (11) or transfer not confirmed pop-up (12)]
// A card that is already frozen starts at 07. Back arrow = previous step.
public class LostCardScreen extends StepScreen {

    private static final Border LIME_OUTLINE = new Border(new BorderStroke(Theme.LIME,
            BorderStrokeStyle.SOLID, new CornerRadii(8), new BorderWidths(1.2)));

    public LostCardScreen() {
        super("Replace or Report Lost Card", "dashboard");
        restartAt(this::startStep);
    }

    @Override
    protected void onExit() {
        LostCardState.clear();
    }

    private void startStep() {
        DemoCard card = LostCardState.getCard();
        if (card != null && card.isFrozen()) {
            replaceStep();
        } else {
            chooseStep();
        }
    }

    // ----- 01: pick the card and the reason (Lost or stolen / Damaged card) -----

    private void chooseStep() {
        DemoCard card = LostCardState.getCard();

        Label change = CommonUi.smallBold("Change", Theme.LIME);
        change.setCursor(Cursor.HAND);
        change.setOnMouseClicked(e -> pickCard());

        Label holder = new Label(card.getHolderName());
        holder.setTextFill(Theme.WHITE);
        holder.setFont(Theme.bold(14));

        // Both reasons stay in place; the chosen one gets the lime outline
        boolean lost = LostCardState.LOST.equals(LostCardState.getReason());

        body.getChildren().addAll(
                heading("Replace or report a card",
                        "Lost your card or need a replacement? Start by selecting the affected card."),
                CommonUi.panel(
                        CommonUi.row("Selected card", change),
                        holder,
                        CommonUi.row("xPress " + CommonUi.masked(card), "Active"),
                        CommonUi.row("Current balance", CommonUi.peso(card.getBalance()))),
                reasonPanel(LostCardState.LOST, lost),
                reasonPanel(LostCardState.DAMAGED, !lost),
                CommonUi.spacer(),
                CommonUi.limeButton("Continue", this::showConfirm),
                CommonUi.link("Cancel", LostCardState::goHome));
    }

    private Node reasonPanel(String reason, boolean selected) {
        String description = LostCardState.LOST.equals(reason)
                ? "Freeze this card and protect its balance."
                : "Replace a card that no longer works.";

        VBox panel = CommonUi.panel(
                CommonUi.row(selected ? "Selected reason" : "Other reason", reason),
                CommonUi.small(description, Theme.WHITE));
        if (selected) {
            panel.setBorder(LIME_OUTLINE);
        } else {
            panel.setCursor(Cursor.HAND);
            panel.setOnMouseClicked(e -> {
                LostCardState.setReason(reason);
                redraw();
            });
        }
        return panel;
    }

    // ----- 02 (pop-up): warn before freezing -----

    private void showConfirm() {
        DemoCard card = LostCardState.getCard();
        boolean lost = LostCardState.LOST.equals(LostCardState.getReason());

        new PopupDialog(WarningDialog.warningIcon(),
                lost ? "Freeze your lost card?" : "Freeze your damaged card?",
                "After verification, we’ll ask the server to freeze the old card UID. "
                        + "It can no longer be used once confirmed.")
                .add(CommonUi.panel(
                        CommonUi.row("xPress card", CommonUi.masked(card)),
                        CommonUi.row("Reason", LostCardState.getReason()),
                        CommonUi.row("Ledger balance", CommonUi.peso(card.getBalance()))),
                        CommonUi.note("Your balance stays in your account ledger until a replacement is verified. "
                                + "The old card stays blocked even if you find it later."))
                .button("Verify and freeze card", () -> showStep(this::otpStep))
                .link("Go back", () -> { })
                .show();
    }

    // ----- 03: the 6-digit code (123456) -----

    private void otpStep() {
        body.getChildren().add(new LostOtpStep(() -> showStep(this::freezingStep)));
    }

    // ----- 04: fake server freeze (Esc = failure) -----

    private void freezingStep() {
        body.getChildren().add(new LostFreezeStep(this::showReported, this::showFreezeFailed));
    }

    // 05 (pop-up): the card is frozen; its ₱676.50 waits for a replacement card
    private void showReported() {
        DemoCard card = LostCardState.getCard();
        new PopupDialog(CommonUi.doubleCheck(), "Lost card reported",
                "The server has frozen your old card UID. Your lost card can no longer be used.")
                .add(CommonUi.panel(
                        CommonUi.row("Old card", CommonUi.masked(card)),
                        CommonUi.row("Server status", "Frozen"),
                        CommonUi.row("Protected balance", CommonUi.peso(card.getBalance())),
                        CommonUi.row("Report reference", card.getFreezeReference())),
                        CommonUi.note("Your balance has not moved yet. Get a replacement card to transfer it, "
                                + "or return later from Card protection."))
                .button("Get replacement card", () -> restartAt(this::replaceStep))
                .link("Back to Home", LostCardState::goHome)
                .show();
    }

    // 06 (pop-up): the freeze was not confirmed; the card may still work
    private void showFreezeFailed() {
        DemoCard card = LostCardState.getCard();
        new PopupDialog(WarningDialog.warningIcon(), "Report not confirmed",
                "We couldn’t confirm the server freeze. Your card may still be active.")
                .add(CommonUi.panel(
                        CommonUi.row("xPress card", CommonUi.masked(card)),
                        CommonUi.row("Freeze status", "Not confirmed"),
                        CommonUi.row("Balance transfer", "Not started")))
                // Already verified with the code, so it retries the freeze directly
                .button("Try Again", this::redraw)
                .link("Back to Home", LostCardState::goHome)
                .show();
    }

    // ----- 07: the old card is frozen; continue once the new card is in hand -----

    private void replaceStep() {
        DemoCard card = LostCardState.getCard();
        body.getChildren().addAll(
                CommonUi.title("Get a replacement card"),
                CommonUi.panel(
                        CommonUi.row("Lost card", CommonUi.masked(card)),
                        CommonUi.row("Old card status", "Frozen"),
                        CommonUi.row("Protected balance", CommonUi.peso(card.getBalance())),
                        CommonUi.row("Report reference", card.getFreezeReference())),
                CommonUi.note("Already received your new card? Continue to link it. "
                        + "Your balance stays protected until transfer is complete."),
                CommonUi.spacer(),
                CommonUi.limeButton("I have my replacement card", () -> showStep(this::linkStep)));
    }

    // ----- 08: type the new card's last 8 digits -----

    private void linkStep() {
        body.getChildren().add(new LinkStep());
    }

    // The details fill in once all 8 digits are there: "8842 8842" -> UID "•••• 8842", "Verified"
    private class LinkStep extends VBox {
        private final CardNumberField numberField = new CardNumberField();
        private final Label uidValue = CommonUi.smallBold("", Theme.WHITE);
        private final Label checkValue = CommonUi.smallBold("", Theme.WHITE);
        private final Label errorLabel = new Label();

        LinkStep() {
            super(12);
            javafx.scene.layout.VBox.setVgrow(this, javafx.scene.layout.Priority.ALWAYS);

            VBox numberPanel = CommonUi.panel(
                    CommonUi.small("xPress card number (last 8 digits)", Theme.WHITE), numberField);
            numberPanel.setBorder(LIME_OUTLINE);

            errorLabel.setTextFill(Theme.ERROR);
            errorLabel.setWrapText(true);
            errorLabel.setVisible(false);
            errorLabel.managedProperty().bind(errorLabel.visibleProperty());

            numberField.setOnDigitsChanged(digits -> refresh());
            numberField.setOnEnter(this::next);

            // Back from the review: keep the number typed before
            String saved = LostCardState.getNewCardNumber();
            if (!saved.isEmpty()) {
                numberField.setDigits(saved.substring(2));
            }
            refresh();

            getChildren().addAll(
                    heading("Link your new card", "Enter the last 8 digits, then hold the issued replacement "
                            + "card to your phone to verify its UID."),
                    numberPanel,
                    errorLabel,
                    CommonUi.panel(
                            CommonUi.row("Issuance status", "Issued by staff"),
                            CommonUi.row("New card UID", uidValue),
                            CommonUi.row("Card check", checkValue),
                            CommonUi.row("Starting balance", CommonUi.peso(0))),
                    CommonUi.note("Use a new, unused card issued to you. If the card number or UID does not "
                            + "match, scan it again before continuing."),
                    CommonUi.spacer(),
                    CommonUi.limeButton("Review balance transfer", this::next),
                    CommonUi.link("Use another card", () -> {
                        numberField.setDigits("");
                        numberField.focusField();
                    }));

            Platform.runLater(numberField::focusField);
        }

        private void refresh() {
            errorLabel.setVisible(false);
            if (numberField.isComplete()) {
                uidValue.setText("•••• " + numberField.getDigits().substring(4));
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
            showStep(LostCardScreen.this::transferReviewStep);
        }

        private void showError(String message) {
            errorLabel.setText(message);
            errorLabel.setVisible(true);
        }
    }

    // ----- 09: ₱676.50 goes from •••• 6767 (frozen) to •••• 8842 (new) -----

    private void transferReviewStep() {
        DemoCard card = LostCardState.getCard();
        double balance = card.getBalance();

        Label amount = new Label(CommonUi.peso(balance));
        amount.setTextFill(Theme.LIME);
        amount.setFont(Theme.bold(26));

        body.getChildren().addAll(
                heading("Move your protected balance", "The server will move the remaining ledger balance "
                        + "from the frozen UID to your verified new UID."),
                CommonUi.panel(
                        CommonUi.row("From frozen card", CommonUi.masked(card)),
                        CommonUi.row("To replacement", LostCardState.maskedNewCard()),
                        amount,
                        CommonUi.small("Server ledger balance to transfer", CommonUi.SOFT_TEXT),
                        CommonUi.row("New balance after transfer", CommonUi.peso(balance)),
                        CommonUi.row("Old balance after transfer", CommonUi.peso(0))),
                CommonUi.note("The old card remains permanently blocked. Confirm only if the replacement card "
                        + "is yours. This transfer does not charge your payment account."),
                CommonUi.spacer(),
                CommonUi.limeButton("Confirm balance transfer", () -> showStep(this::transferringStep)),
                CommonUi.link("Change replacement card", this::back));
    }

    // ----- 10: fake balance transfer (Esc = failure) -----

    private void transferringStep() {
        body.getChildren().add(new LostTransferStep(this::showReplaced, this::showTransferFailed));
    }

    // 11 (pop-up): the new card is active with the old balance; the old card is permanently frozen
    private void showReplaced() {
        DemoCard oldCard = LostCardState.getCard();
        DemoCard newCard = LostCardState.getReplacement();
        new PopupDialog(CommonUi.doubleCheck(), "Replacement complete",
                "The server transferred your ledger balance. Your new card is active.")
                .add(CommonUi.panel(
                        CommonUi.row("New card UID", CommonUi.masked(newCard)),
                        CommonUi.row("New card balance", CommonUi.peso(newCard.getBalance())),
                        CommonUi.row("Old card UID", CommonUi.masked(oldCard)),
                        CommonUi.row("Old card balance", CommonUi.peso(oldCard.getBalance())),
                        CommonUi.row("Old card status", "Permanently frozen"),
                        CommonUi.row("Transfer reference", LostCardState.getTransferReference())),
                        CommonUi.note("The old UID can no longer be used. "
                                + "Your account now uses the replacement card."))
                .button("View replacement card", () -> {
                    LostCardState.clear();
                    Navigator.go("allCards");
                })
                .link("Back to Home", LostCardState::goHome)
                .show();
    }

    // 12 (pop-up): the transfer was not confirmed; the balance is still on the frozen card
    private void showTransferFailed() {
        DemoCard card = LostCardState.getCard();
        new PopupDialog(WarningDialog.warningIcon(), "Transfer not confirmed",
                "The server did not confirm the balance transfer. Your old card is still frozen.")
                .add(CommonUi.panel(
                        CommonUi.row("Source UID", CommonUi.masked(card)),
                        CommonUi.row("Replacement UID", LostCardState.maskedNewCard()),
                        CommonUi.row("Amount requested", CommonUi.peso(card.getBalance())),
                        CommonUi.row("Transfer status", "Not confirmed")),
                        CommonUi.note("Your balance remains protected in the account ledger. Check the status "
                                + "before retrying. Do not request another replacement card."))
                .button("Check transfer status", this::back)   // back to the review (09)
                .show();
    }

    // ----- Helpers -----

    // "Change": the card list shows Active / Frozen on the right; then start over with that card
    private void pickCard() {
        Navigator.showOverlay(new CardPickerDialog(LostCardState.getCard(), picked -> {
            LostCardState.setCard(picked);
            restartAt(this::startStep);
        }, c -> c.isFrozen()
                ? CommonUi.smallBold("Frozen", Theme.ERROR)
                : CommonUi.smallBold("Active", Theme.LIME)));
    }

    private VBox heading(String title, String subtitle) {
        VBox heading = new VBox(8, CommonUi.title(title), CommonUi.subtitle(subtitle));
        VBox.setMargin(heading, new Insets(0, 0, 6, 0));
        return heading;
    }
}
