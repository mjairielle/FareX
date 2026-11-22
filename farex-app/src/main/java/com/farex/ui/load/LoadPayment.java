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

    // null when started, else why not (e.g. "Amount must be P5.00 to P5,000.00").
    // Mock: paid at once. Live: starts a PayMongo checkout (the caller opens it on the UI thread with openCheckout());
    // nothing is saved until confirmed() says paid. Network: call it off the UI thread.
    static String pay() {
        if (!MockData.isLive()) {
            addPending(MockData.nextReference());
            addTransaction();
            return null;
        }
        try {
            String[] checkout = Api.startLoad(BuyLoadState.getCard().getCardNumber(),
                    Math.round(BuyLoadState.getAmount() * 100), BuyLoadState.getMethod());
            BuyLoadState.setCheckout(checkout[0], checkout[1]);
            return null;
        } catch (Api.ApiException e) {
            return e.getMessage();
        }
    }

    // The PayMongo page again (closed by mistake). A server without PayMongo ("fake") has none.
    static void openCheckout() {
        String url = BuyLoadState.getCheckoutUrl();
        if (url != null && !"fake".equals(url)) {
            FarexApp.openUrl(url);
        }
    }

    // Live: true once PayMongo confirms this checkout was paid; the load then waits on the card for Fetch Load.
    // Blocks on the network, so call it off the UI thread. Offline counts as not confirmed yet.
    static boolean confirmed() {
        try {
            for (String[] paid : Api.paidLoads()) {
                if (paid[0].equals(BuyLoadState.getCheckoutId())) {
                    return true;
                }
            }
        } catch (Api.ApiException offline) {
            // ask again on the next check
        }
        return false;
    }

    // UI thread, after confirmed(): the paid load shows in Fetch Load and in the notifications
    static void addPending(String reference) {
        BuyLoadState.getCard().addPendingLoadOnce(new PendingLoad(BuyLoadState.getAmount(), BuyLoadState.getMethod(), reference));
        addPaidNotification();
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
