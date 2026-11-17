package com.farex.ui;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.control.Label;
import javafx.scene.layout.Border;
import javafx.scene.layout.BorderStroke;
import javafx.scene.layout.BorderStrokeStyle;
import javafx.scene.layout.BorderWidths;
import javafx.scene.layout.CornerRadii;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;

// Redeem Points, all in one screen (opened from Home):
//   "Turn points into trips" (01) -> "Review your redemption" (02) -> "Redeeming your points…" (03)
// Success (04), failure (05) and not enough points (06) are pop-ups. Back arrow = previous step.
public class RedeemScreen extends StepScreen {

    private static final Border CHOICE_BORDER = new Border(new BorderStroke(
            Color.web("#BDBDBD"), BorderStrokeStyle.SOLID, new CornerRadii(6), new BorderWidths(1)));

    private final Label pointsValue = FetchUi.smallBold("", Theme.WHITE);
    private final Label creditValue = FetchUi.smallBold("", Theme.WHITE);
    private final Label hint = FetchUi.small("", Theme.WHITE);
    private final Label[] choiceLabels = new Label[RedeemState.CHOICES.length];

    public RedeemScreen() {
        super("Redeem Points", "dashboard");
        restartAt(this::chooseStep);
    }

    @Override
    protected void onExit() {
        RedeemState.clear();
    }

    // ----- Redeem Points / 01: pick 50, 100 or 250 points -----

    private void chooseStep() {
        DemoCard card = RedeemState.getCard();

        Label available = new Label(RedeemState.getAvailable() + " xPress Points");
        available.setTextFill(Theme.LIME);
        available.setFont(Theme.bold(22));

        // Choice box (lime outline): 50 pts / 100 pts / 250 pts
        HBox choices = new HBox(8);
        for (int i = 0; i < RedeemState.CHOICES.length; i++) {
            int points = RedeemState.CHOICES[i];
            Label choice = new Label(FetchUi.pts(points));
            choice.setFont(Theme.bold(11));
            choice.setAlignment(Pos.CENTER);
            choice.setMaxWidth(Double.MAX_VALUE);
            choice.setPadding(new Insets(10, 0, 10, 0));
            choice.setCursor(Cursor.HAND);
            choice.setOnMouseClicked(e -> select(points));
            HBox.setHgrow(choice, Priority.ALWAYS);
            choiceLabels[i] = choice;
            choices.getChildren().add(choice);
        }
        VBox.setMargin(choices, new Insets(4, 0, 0, 0));

        VBox choicePanel = FetchUi.panel(
                FetchUi.row("Points to redeem", pointsValue),
                FetchUi.row("Transit credit", creditValue),
                choices);
        choicePanel.setBorder(new Border(new BorderStroke(Theme.LIME,
                BorderStrokeStyle.SOLID, new CornerRadii(8), new BorderWidths(1.2))));
        hint.setWrapText(true);

        body.getChildren().addAll(
                heading("Turn points into trips",
                        "Redeem earned xPress Points for transit credit on your linked card."),
                FetchUi.panel(available, FetchUi.small("Available to redeem", FetchUi.SOFT_TEXT)),
                choicePanel,
                FetchUi.row("Credit destination", (card == null) ? "No card" : FetchUi.masked(card)),
                FetchUi.panel(hint));

        // No card: the credit has nowhere to go
        if (card == null) {
            body.getChildren().addAll(
                    FetchUi.note("Add an xPress card first. Redeemed credit is collected onto a card."),
                    FetchUi.spacer(),
                    FetchUi.limeButton("Add Card", () -> Navigator.go("addCard")));
        } else {
            body.getChildren().addAll(
                    FetchUi.spacer(),
                    FetchUi.limeButton("Review redemption", () -> {
                        if (RedeemState.hasEnough()) {
                            showStep(this::reviewStep);
                        } else {
                            showNotEnough();
                        }
                    }));
        }
        body.getChildren().add(FetchUi.link("Cancel", RedeemState::goHome));

        select(RedeemState.getSelected());
    }

    // 100 -> "100 pts", "₱100.00", the 100 tile turns lime, and the hint updates
    private void select(int points) {
        RedeemState.setSelected(points);
        pointsValue.setText(FetchUi.pts(points));
        creditValue.setText(FetchUi.peso(points));
        hint.setText(points + " points is selected. Your points will only be deducted "
                + "after the redemption is confirmed.");

        for (int i = 0; i < choiceLabels.length; i++) {
            boolean on = RedeemState.CHOICES[i] == points;
            Label choice = choiceLabels[i];
            choice.setTextFill(on ? Theme.BLACK : Theme.WHITE);
            choice.setBackground(on ? Theme.rounded(Theme.LIME, 6) : null);
            choice.setBorder(on ? null : CHOICE_BORDER);
        }
    }

    // ----- Redeem Points / 02: check points, credit and the card before confirming -----

    private void reviewStep() {
        DemoCard card = RedeemState.getCard();
        int points = RedeemState.getSelected();

        // 100 pts on ₱676.50 -> ₱776.50 after fetch, 250 - 100 = 150 pts left
        body.getChildren().addAll(
                heading("Review your redemption", "Check the points and destination before confirming."),
                FetchUi.panel(
                        FetchUi.row("Points to redeem", FetchUi.pts(points)),
                        FetchUi.row("Transit credit", FetchUi.peso(points)),
                        FetchUi.row("Destination", FetchUi.masked(card)),
                        FetchUi.row("Current balance", FetchUi.peso(card.getBalance())),
                        FetchUi.row("Balance after fetch", FetchUi.peso(card.getBalance() + points)),
                        FetchUi.row("Points remaining", FetchUi.pts(RedeemState.getAvailable() - points))),
                FetchUi.note("Credit will be ready to fetch after redemption. Use Fetch Load to update "
                        + "your physical card. Confirmed redemptions cannot be reversed."),
                FetchUi.spacer(),
                FetchUi.limeButton("Confirm redemption", () -> showStep(this::redeemingStep)),
                FetchUi.link("Edit amount", this::back));
    }

    // ----- Redeem Points / 03: fake server check (Esc = failure) -----

    private void redeemingStep() {
        body.getChildren().add(new RedeemProgressStep(this::showSuccess, this::showFailure));
    }

    // ----- Results (pop-ups) -----

    // Redeem Points / 04: points taken, credit waiting on the card for Fetch Load
    private void showSuccess() {
        DemoCard card = RedeemState.getCard();
        int points = RedeemState.getSelected();

        new PopupDialog(FetchUi.doubleCheck(), "Points redeemed!",
                "Your transit credit is ready to fetch. Your physical card has not been updated yet.")
                .add(FetchUi.panel(
                        FetchUi.row("Points redeemed", FetchUi.pts(points)),
                        FetchUi.row("Points remaining", FetchUi.pts(RedeemState.getAvailable())),
                        FetchUi.row("Ready to fetch", FetchUi.peso(points)),
                        FetchUi.row("xPress card", FetchUi.masked(card)),
                        FetchUi.row("Reference", RedeemState.getLastReference())),
                        FetchUi.note("Next: hold your card to your phone in Fetch Load to collect this credit."))
                .button("Back to Home", RedeemState::goHome)
                .link("View Transaction", () -> {
                    RedeemState.clear();
                    Navigator.go("transactions");
                })
                .show();
    }

    // Redeem Points / 05: the redemption did not go through; no points were taken
    private void showFailure() {
        new PopupDialog(WarningDialog.warningIcon(), "Couldn’t redeem points",
                "The service could not complete this redemption. Your points have not been deducted.")
                .add(FetchUi.panel(
                        FetchUi.row("Requested", FetchUi.pts(RedeemState.getSelected())),
                        FetchUi.row("Available balance", FetchUi.pts(RedeemState.getAvailable())),
                        FetchUi.row("Transit credit issued", FetchUi.peso(0))),
                        FetchUi.note("Check your connection and try again. If a request is still pending, "
                                + "check its status before resubmitting."))
                .button("Try again", this::back)        // back to the review step
                .link("Back to Home", RedeemState::goHome)
                .show();
    }

    // Redeem Points / 06: more points picked than the account has.
    // Example: 150 available, 250 picked -> "Points needed: 100 pts"
    private void showNotEnough() {
        int requested = RedeemState.getSelected();
        int available = RedeemState.getAvailable();

        new PopupDialog(WarningDialog.warningIcon(), "Not enough points",
                "You need more earned points for the amount you selected.")
                .add(FetchUi.panel(
                        FetchUi.row("Requested", FetchUi.pts(requested)),
                        FetchUi.row("Available", FetchUi.pts(available)),
                        FetchUi.row("Points needed", FetchUi.pts(requested - available))),
                        FetchUi.note("Choose " + available + " points or less. No points were deducted "
                                + "and no transit credit was issued."))
                .button("Choose a lower amount", () -> { })   // the choice screen is right behind
                .link("Back to Home", RedeemState::goHome)
                .show();
    }

    // ----- Helpers -----

    private VBox heading(String title, String subtitle) {
        VBox heading = new VBox(8, FetchUi.title(title), FetchUi.subtitle(subtitle));
        VBox.setMargin(heading, new Insets(0, 0, 6, 0));
        return heading;
    }
}
