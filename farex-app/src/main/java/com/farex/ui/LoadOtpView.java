package com.farex.ui;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

// Buy Load, step 6: the registration OTP screen, but the right code (123456)
// adds the load and opens the receipt
public class LoadOtpView extends OtpView {

    public LoadOtpView() {
        super("We sent a code to your", sentTo(BuyLoadState.getMethod()));

        // Figma: content starts near the top, with a back arrow
        setAlignment(Pos.TOP_CENTER);
        setSpacing(22);
        setPadding(new Insets(26, 28, 28, 28));

        Label back = new Label("‹");
        back.setTextFill(Theme.WHITE);
        back.setFont(Theme.bold(36));
        back.setCursor(Cursor.HAND);
        back.setOnMouseClicked(e -> {
            stopTimer();
            Navigator.go("loadReview");
        });
        HBox backRow = new HBox(back);
        VBox.setMargin(backRow, new Insets(0, 0, 14, 0));
        getChildren().add(0, backRow);
    }

    // "GCash" -> "GCash Account"
    private static String sentTo(String method) {
        return "Others".equals(method) ? "Card/e-Wallet Account" : method + " Account";
    }

    @Override
    protected void onCodeAccepted() {
        BuyLoadState.getCard().addLoad(BuyLoadState.getAmount());
        Navigator.go("loadReceipt");
    }
}
