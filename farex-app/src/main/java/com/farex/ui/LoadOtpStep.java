package com.farex.ui;

import java.util.Locale;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

// Buy Load, step 6 (no lime header): the registration OTP screen, but the right code (123456)
// saves a pending load, then onPaid shows the receipt
public class LoadOtpStep extends OtpView {

    private final Runnable onPaid;

    // onBack: its own back arrow (the Figma has no lime header here)
    public LoadOtpStep(Runnable onPaid, Runnable onBack) {
        super("We sent a code to your", sentTo(BuyLoadState.getMethod()));
        this.onPaid = onPaid;

        // Figma: content starts near the top, with a back arrow
        setAlignment(Pos.TOP_CENTER);
        setSpacing(22);
        setPadding(new Insets(0, 8, 0, 8));   // the screen's body already pads the edges

        Label back = new Label("‹");
        back.setTextFill(Theme.WHITE);
        back.setFont(Theme.bold(36));
        back.setCursor(Cursor.HAND);
        back.setOnMouseClicked(e -> {
            stopTimer();
            onBack.run();
        });
        HBox backRow = new HBox(back);
        VBox.setMargin(backRow, new Insets(0, 0, 14, 0));
        getChildren().add(0, backRow);
    }

    // "GCash" -> "GCash Account"
    private static String sentTo(String method) {
        return "Others".equals(method) ? "Card/e-Wallet Account" : method + " Account";
    }

    // Paying does not change the balance yet: the ₱200 waits as a pending load
    // until it is collected with Fetch Load (676.50 stays 676.50 until then)
    @Override
    protected void onCodeAccepted() {
        PendingLoad load = new PendingLoad(BuyLoadState.getAmount(),
                BuyLoadState.getMethod(), MockData.nextReference());
        BuyLoadState.getCard().addPendingLoad(load);
        addPaidNotification();
        addTransaction();
        onPaid.run();
    }

    // ₱200 with GCash -> "You have paid ₱200.00 using GCash."
    private void addPaidNotification() {
        DemoAccount account = MockData.getCurrentAccount();
        if (account == null) {
            return;
        }
        String method = BuyLoadState.getMethod();
        String using = "Others".equals(method) ? "your card/e-wallet" : method;
        String message = String.format(Locale.US, "You have paid **₱%,.2f** using %s.",
                BuyLoadState.getAmount(), using);
        account.addNotification(new DemoNotification("Buy Load Transaction", message, false));
    }

    // Transactions tab: "Buy Load  4:05 PM | GCash  + ₱ 200.00"
    private void addTransaction() {
        DemoAccount account = MockData.getCurrentAccount();
        if (account == null) {
            return;
        }
        String method = BuyLoadState.getMethod();
        String via = "Others".equals(method) ? "Card/e-Wallet" : method;
        account.addTransaction(DemoTransaction.LOAD, "Buy Load", via, BuyLoadState.getAmount());
    }
}
