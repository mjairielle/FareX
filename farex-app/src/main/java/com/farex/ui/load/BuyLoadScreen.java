package com.farex.ui.load;

import com.farex.ui.common.PopupDialog;
import com.farex.ui.common.WarningDialog;
import com.farex.ui.common.StepScreen;

// Buy Load, all in one screen (opened from Home):
//   card (Buy Load) -> payment method (Payment) -> account number (Payment) -> amount (Buy Load)
//   -> review -> receipt (Load Receipt). PayMongo confirms the payment; there is no OTP step of our own.
// Each step is its own class (LoadCardStep, LoadMethodStep, ...) and keeps its Figma title.
// Back arrow = previous step; on the receipt it leaves to Home (the payment is done).
public class BuyLoadScreen extends StepScreen {

    public BuyLoadScreen() {
        super("Buy Load", "dashboard");
        restartAt(this::cardStep);
    }

    @Override
    protected void onExit() {
        BuyLoadState.clear();
    }

    private void cardStep() {
        body.getChildren().add(new LoadCardStep(() -> showStep(this::methodStep)));
    }

    private void methodStep() {
        setTitle("Payment");
        body.getChildren().add(new LoadMethodStep(() -> showStep(this::accountStep)));
    }

    private void accountStep() {
        setTitle("Payment");
        body.getChildren().add(new LoadAccountStep(() -> showStep(this::amountStep)));
    }

    private void amountStep() {
        body.getChildren().add(new LoadAmountStep(() -> showStep(this::reviewStep)));
    }

    private void reviewStep() {
        setTitle("");
        body.getChildren().add(new LoadReviewStep(() -> {
            String error = LoadPayment.pay();
            if (error == null) {
                restartAt(this::receiptStep);
            } else {
                new PopupDialog(WarningDialog.warningIcon(), "Payment not started", error).button("OK", () -> { }).show();
            }
        }));
    }

    // restartAt forgets the earlier steps, so back from the receipt goes Home
    private void receiptStep() {
        setTitle("Load Receipt");
        body.getChildren().add(new LoadReceiptStep());
    }
}
