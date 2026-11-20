package com.farex.ui.load;

import java.util.Locale;

import com.farex.Api;
import com.farex.ui.FarexApp;
import com.farex.ui.data.DemoAccount;
import com.farex.ui.data.DemoNotification;
import com.farex.ui.data.DemoTransaction;
import com.farex.ui.data.MockData;
import com.farex.ui.data.PendingLoad;

// Buy Load, "Confirm" on the review step. There is no OTP of our own: PayMongo confirms the payment.
// Paying does not change the balance yet: the ₱200 waits as a pending load until Fetch Load
// (676.50 stays 676.50 until then). Live: the server opens a PayMongo checkout; its session id is
// the reference, and Fetch Load asks the server whether it was paid before the balance goes up.
final class LoadPayment {

    private LoadPayment() {
    }

    // null when the pending load is saved, else why not (e.g. "Amount must be P5.00 to P5,000.00")
    static String pay() {
        String reference = MockData.nextReference();
        if (MockData.isLive()) {
            try {
                String[] checkout = Api.startLoad(BuyLoadState.getCard().getCardNumber(),
                        Math.round(BuyLoadState.getAmount() * 100));
                reference = checkout[0];
                if (!"fake".equals(checkout[1])) {
                    FarexApp.openUrl(checkout[1]);   // pay there, then Fetch Load
                }
            } catch (Api.ApiException e) {
                return e.getMessage();
            }
        }
        PendingLoad load = new PendingLoad(BuyLoadState.getAmount(), BuyLoadState.getMethod(), reference);
        BuyLoadState.getCard().addPendingLoad(load);
        addPaidNotification();
        addTransaction();
        return null;
    }

    // ₱200 with GCash -> "You have paid ₱200.00 using GCash."
    private static void addPaidNotification() {
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
    private static void addTransaction() {
        DemoAccount account = MockData.getCurrentAccount();
        if (account == null || MockData.isLive()) {   // live: the server's ledger row shows once paid
            return;
        }
        String method = BuyLoadState.getMethod();
        String via = "Others".equals(method) ? "Card/e-Wallet" : method;
        account.addTransaction(DemoTransaction.LOAD, "Buy Load", via, BuyLoadState.getAmount());
    }
}
