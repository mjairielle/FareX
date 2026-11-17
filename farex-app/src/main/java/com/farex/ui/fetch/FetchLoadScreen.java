package com.farex.ui.fetch;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import javafx.scene.text.TextAlignment;

import com.farex.ui.common.CardPickerDialog;
import com.farex.ui.common.CardTile;
import com.farex.ui.common.CommonUi;
import com.farex.ui.common.Navigator;
import com.farex.ui.common.PopupDialog;
import com.farex.ui.common.StepScreen;
import com.farex.ui.common.Theme;
import com.farex.ui.common.WarningDialog;
import com.farex.ui.data.DemoCard;
import com.farex.ui.data.PendingLoad;

// Fetch Load, all in one screen (opened from Home):
//   start: "Fetch your pending load" (01), or "No pending load" (06) if the card has none
//   -> "Hold your card near your phone" (02) -> "Fetching your load…" (03)
// Success (04) and failure (05) are pop-ups over the screen. Back arrow = previous step.
public class FetchLoadScreen extends StepScreen {

    public FetchLoadScreen() {
        super("Fetch Load", "dashboard");
        restartAt(this::startStep);
    }

    @Override
    protected void onExit() {
        FetchLoadState.clear();
    }

    // ----- Start: a load to fetch, or none -----

    private void startStep() {
        DemoCard card = FetchLoadState.getCard();
        if (card != null && card.hasPendingLoad()) {
            selectStep(card);
        } else {
            noneStep(card);
        }
    }

    // Fetch Load / 01: the card and the paid top-up waiting to be collected
    private void selectStep(DemoCard card) {
        PendingLoad load = card.getPendingLoad();

        Label change = CommonUi.smallBold("Change", Theme.LIME);
        change.setCursor(Cursor.HAND);
        change.setOnMouseClicked(e -> pickCard());

        Label holder = new Label(card.getHolderName());
        holder.setTextFill(Theme.WHITE);
        holder.setFont(Theme.bold(14));

        body.getChildren().addAll(
                heading("Fetch your pending load",
                        "Move paid load onto your xPress card. Choose the card and top-up to collect."),
                CommonUi.panel(
                        CommonUi.row("Selected card", change),
                        holder,
                        CommonUi.row("xPress " + CommonUi.masked(card), CommonUi.peso(card.getBalance()))),
                CommonUi.panel(
                        CommonUi.row("Selected top-up", CommonUi.peso(load.getAmount())),
                        CommonUi.small(CommonUi.paidVia(load.getMethod()) + " • Ready to fetch", Theme.LIME),
                        CommonUi.row("Reference", load.getReference())),
                CommonUi.note("Keep your physical card nearby and turn on NFC. You will not be charged again."),
                CommonUi.spacer(),
                CommonUi.limeButton("Continue to fetch", () -> showStep(() -> holdStep(card))),
                CommonUi.link("Cancel", FetchLoadState::goHome));
    }

    // Fetch Load / 06: the chosen card has no paid top-ups waiting
    private void noneStep(DemoCard card) {
        Label title = CommonUi.title("No pending load");
        title.setFont(Theme.bold(20));
        Label text = CommonUi.subtitle("There are no paid top-ups waiting to be collected for this card.");
        text.setTextAlignment(TextAlignment.CENTER);
        text.setMaxWidth(260);

        VBox message = new VBox(12, WarningDialog.warningIcon(), title, text);
        message.setAlignment(Pos.TOP_CENTER);
        VBox.setMargin(message, new Insets(46, 0, 0, 0));

        VBox details = (card == null)
                ? CommonUi.note("You have no xPress card yet.")
                : CommonUi.panel(
                        CommonUi.row("xPress card", CommonUi.masked(card)),
                        CommonUi.row("Current balance", CommonUi.peso(card.getBalance())),
                        CommonUi.row("Pending load", CommonUi.peso(0)));

        body.getChildren().addAll(
                message,
                CommonUi.spacer(),
                details,
                CommonUi.note("Just paid? Refresh the status before purchasing again. "
                        + "You can also check another linked card."),
                // Checks again: if a load is pending now, this turns into "Fetch your pending load"
                CommonUi.limeButton("Refresh status", () -> restartAt(this::startStep)),
                CommonUi.link("Choose another card", this::pickCard));
    }

    // ----- Fetch Load / 02: hold the card against the phone -----

    private void holdStep(DemoCard card) {
        body.getChildren().addAll(
                heading("Hold your card near your phone",
                        "Turn on NFC, then place your xPress card against the back of your phone."),
                new CardTile(card),
                CommonUi.note("Remove thick phone cases. Keep the card still until confirmed. "
                        + "No NFC? Use an authorized load-fetch terminal."),
                CommonUi.spacer(),
                // A laptop has no NFC, so this starts the fake card read
                CommonUi.limeButton("Start reading card", () -> showStep(this::fetchingStep)),
                CommonUi.link("Choose another card", this::pickCard));
    }

    // ----- Fetch Load / 03: the fake card read (Esc = failure) -----

    private void fetchingStep() {
        body.getChildren().add(new FetchProgressStep(this::showSuccess, this::showFailure));
    }

    // ----- Results (pop-ups) -----

    // Fetch Load / 04: ₱200.00 added, ₱676.50 -> ₱876.50
    private void showSuccess() {
        DemoCard card = FetchLoadState.getCard();
        PendingLoad load = FetchLoadState.getLastLoad();

        Label added = new Label(CommonUi.peso(load.getAmount()) + " added");
        added.setTextFill(Theme.LIME);
        added.setFont(Theme.bold(20));

        new PopupDialog(CommonUi.doubleCheck(), "Load fetched successfully",
                "Your xPress card is updated and ready for your next trip.")
                .add(CommonUi.panel(
                        added,
                        CommonUi.row("Previous balance", CommonUi.peso(FetchLoadState.getPreviousBalance())),
                        CommonUi.row("New card balance", CommonUi.peso(card.getBalance())),
                        CommonUi.row("xPress card", CommonUi.masked(card)),
                        CommonUi.row("Reference", load.getReference())),
                        CommonUi.note("This top-up is marked as collected. No additional payment was made."))
                .button("Back to Home", FetchLoadState::goHome)
                .link("View transactions", () -> {
                    FetchLoadState.clear();
                    Navigator.go("transactions");
                })
                .show();
    }

    // Fetch Load / 05: the card read stopped; the load is still pending
    private void showFailure() {
        DemoCard card = FetchLoadState.getCard();
        PendingLoad load = FetchLoadState.getLastLoad();

        new PopupDialog(WarningDialog.warningIcon(), "Couldn’t fetch your load",
                "The card connection was interrupted before the balance update was confirmed.")
                .add(CommonUi.panel(
                        CommonUi.row("Top-up", CommonUi.peso(load.getAmount())),
                        CommonUi.row("Card", CommonUi.masked(card)),
                        CommonUi.row("Status", "Not confirmed")),
                        CommonUi.note("Your paid top-up is safe. Check its status first. If it is still pending, "
                                + "hold the same card steady and retry. Do not buy load again."))
                // Back to the start step: the load is still there, so it shows "Fetch your pending load"
                .button("Check fetch status", () -> restartAt(this::startStep))
                .link("Back to Home", FetchLoadState::goHome)
                .show();
    }

    // ----- Helpers -----

    // "Change" / "Choose another card": pick a card, then start over with it
    private void pickCard() {
        Navigator.showOverlay(new CardPickerDialog(FetchLoadState.getCard(), picked -> {
            FetchLoadState.setCard(picked);
            restartAt(this::startStep);
        }));
    }

    private VBox heading(String title, String subtitle) {
        VBox heading = new VBox(8, CommonUi.title(title), CommonUi.subtitle(subtitle));
        VBox.setMargin(heading, new Insets(0, 0, 6, 0));
        return heading;
    }
}
