package com.farex.ui.load;

import com.farex.ui.common.StepScreen;

// Buy Load, all in one screen (opened from Home):
//   card (Buy Load) -> payment method (Payment) -> account number (Payment) -> amount (Buy Load)
//   -> review -> OTP (own back arrow, no lime bar) -> receipt (Load Receipt)
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
        body.getChildren().add(new LoadReviewStep(() -> showStep(this::otpStep)));
    }

    private void otpStep() {
        setHeaderShown(false);
        body.getChildren().add(new LoadOtpStep(() -> restartAt(this::receiptStep), this::back));
    }

    // restartAt forgets the earlier steps, so back from the receipt goes Home
    private void receiptStep() {
        setTitle("Load Receipt");
        body.getChildren().add(new LoadReceiptStep());
    }
}
