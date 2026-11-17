package com.farex.ui.redeem;

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

import com.farex.ui.common.CommonUi;
import com.farex.ui.common.Navigator;
import com.farex.ui.common.PopupDialog;
import com.farex.ui.common.StepScreen;
import com.farex.ui.common.Theme;
import com.farex.ui.common.WarningDialog;
import com.farex.ui.data.DemoCard;

// Redeem Points, all in one screen (opened from Home):
//   "Turn points into trips" (01) -> "Review your redemption" (02) -> "Redeeming your points…" (03)
// Success (04), failure (05) and not enough points (06) are pop-ups. Back arrow = previous step.
public class RedeemScreen extends StepScreen {

    private static final Border CHOICE_BORDER = new Border(new BorderStroke(
            Color.web("#BDBDBD"), BorderStrokeStyle.SOLID, new CornerRadii(6), new BorderWidths(1)));

    private final Label pointsValue = CommonUi.smallBold("", Theme.WHITE);
    private final Label creditValue = CommonUi.smallBold("", Theme.WHITE);
    private final Label hint = CommonUi.small("", Theme.WHITE);
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
            Label choice = new Label(CommonUi.pts(points));
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

        VBox choicePanel = CommonUi.panel(
                CommonUi.row("Points to redeem", pointsValue),
                CommonUi.row("Transit credit", creditValue),
                choices);
        choicePanel.setBorder(new Border(new BorderStroke(Theme.LIME,
                BorderStrokeStyle.SOLID, new CornerRadii(8), new BorderWidths(1.2))));
        hint.setWrapText(true);

        body.getChildren().addAll(
                heading("Turn points into trips",
                        "Redeem earned xPress Points for transit credit on your linked card."),
                CommonUi.panel(available, CommonUi.small("Available to redeem", CommonUi.SOFT_TEXT)),
                choicePanel,
                CommonUi.row("Credit destination", (card == null) ? "No card" : CommonUi.masked(card)),
                CommonUi.panel(hint));

        // No card: the credit has nowhere to go
        if (card == null) {
            body.getChildren().addAll(
                    CommonUi.note("Add an xPress card first. Redeemed credit is collected onto a card."),
                    CommonUi.spacer(),
                    CommonUi.limeButton("Add Card", () -> Navigator.go("addCard")));
        } else {
            body.getChildren().addAll(
                    CommonUi.spacer(),
                    CommonUi.limeButton("Review redemption", () -> {
                        if (RedeemState.hasEnough()) {
                            showStep(this::reviewStep);
                        } else {
                            showNotEnough();
                        }
                    }));
        }
        body.getChildren().add(CommonUi.link("Cancel", RedeemState::goHome));

        select(RedeemState.getSelected());
    }

    // 100 -> "100 pts", "₱100.00", the 100 tile turns lime, and the hint updates
    private void select(int points) {
        RedeemState.setSelected(points);
        pointsValue.setText(CommonUi.pts(points));
        creditValue.setText(CommonUi.peso(points));
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
                CommonUi.panel(
                        CommonUi.row("Points to redeem", CommonUi.pts(points)),
                        CommonUi.row("Transit credit", CommonUi.peso(points)),
                        CommonUi.row("Destination", CommonUi.masked(card)),
                        CommonUi.row("Current balance", CommonUi.peso(card.getBalance())),
                        CommonUi.row("Balance after fetch", CommonUi.peso(card.getBalance() + points)),
                        CommonUi.row("Points remaining", CommonUi.pts(RedeemState.getAvailable() - points))),
                CommonUi.note("Credit will be ready to fetch after redemption. Use Fetch Load to update "
                        + "your physical card. Confirmed redemptions cannot be reversed."),
                CommonUi.spacer(),
                CommonUi.limeButton("Confirm redemption", () -> showStep(this::redeemingStep)),
                CommonUi.link("Edit amount", this::back));
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

        new PopupDialog(CommonUi.doubleCheck(), "Points redeemed!",
                "Your transit credit is ready to fetch. Your physical card has not been updated yet.")
                .add(CommonUi.panel(
                        CommonUi.row("Points redeemed", CommonUi.pts(points)),
                        CommonUi.row("Points remaining", CommonUi.pts(RedeemState.getAvailable())),
                        CommonUi.row("Ready to fetch", CommonUi.peso(points)),
                        CommonUi.row("xPress card", CommonUi.masked(card)),
                        CommonUi.row("Reference", RedeemState.getLastReference())),
                        CommonUi.note("Next: hold your card to your phone in Fetch Load to collect this credit."))
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
                .add(CommonUi.panel(
                        CommonUi.row("Requested", CommonUi.pts(RedeemState.getSelected())),
                        CommonUi.row("Available balance", CommonUi.pts(RedeemState.getAvailable())),
                        CommonUi.row("Transit credit issued", CommonUi.peso(0))),
                        CommonUi.note("Check your connection and try again. If a request is still pending, "
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
                .add(CommonUi.panel(
                        CommonUi.row("Requested", CommonUi.pts(requested)),
                        CommonUi.row("Available", CommonUi.pts(available)),
                        CommonUi.row("Points needed", CommonUi.pts(requested - available))),
                        CommonUi.note("Choose " + available + " points or less. No points were deducted "
                                + "and no transit credit was issued."))
                .button("Choose a lower amount", () -> { })   // the choice screen is right behind
                .link("Back to Home", RedeemState::goHome)
                .show();
    }

    // ----- Helpers -----

    private VBox heading(String title, String subtitle) {
        VBox heading = new VBox(8, CommonUi.title(title), CommonUi.subtitle(subtitle));
        VBox.setMargin(heading, new Insets(0, 0, 6, 0));
        return heading;
    }
}
