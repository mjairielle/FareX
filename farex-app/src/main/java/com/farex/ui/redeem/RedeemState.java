package com.farex.ui.redeem;

import com.farex.Api;
import com.farex.ui.common.CommonUi;
import com.farex.ui.common.Navigator;
import com.farex.ui.data.DemoAccount;
import com.farex.ui.data.DemoCard;
import com.farex.ui.data.DemoNotification;
import com.farex.ui.data.DemoTransaction;
import com.farex.ui.data.MockData;
import com.farex.ui.data.PendingLoad;

// Remembers the Redeem choice between screens and does the redemption itself.
// The credit goes to the account's first card as a pending load, collected later with Fetch Load.
public final class RedeemState {

    // The server's perks: 100, 200, 500 pts -> ₱10, ₱20, ₱50 (farex-shared Rewards.PERKS)
    public static final int[] CHOICES = {100, 200, 500};
    private static final int DEFAULT_CHOICE = 100;

    private static int selected = DEFAULT_CHOICE;
    private static String lastReference = "";

    private RedeemState() {
    }

    // 10 points = ₱1 of transit credit, same as the server
    public static double credit(int points) {
        return points / 10.0;
    }

    public static int getSelected() {
        return selected;
    }

    public static void setSelected(int points) {
        selected = points;
    }

    public static String getLastReference() {
        return lastReference;
    }

    public static void clear() {
        selected = DEFAULT_CHOICE;
        lastReference = "";
    }

    public static DemoAccount getAccount() {
        return MockData.getCurrentAccount();
    }

    // Where the credit goes: the first card (•••• 6767)
    public static DemoCard getCard() {
        DemoAccount account = getAccount();
        return (account == null) ? null : account.getPrimaryCard();
    }

    public static int getAvailable() {
        DemoAccount account = getAccount();
        return (account == null) ? 0 : account.getPointBalance();
    }

    // 250 available, 100 selected -> true; 150 available, 250 selected -> false
    public static boolean hasEnough() {
        return selected <= getAvailable();
    }

    // 100 pts -> 250 becomes 150, ₱10.00 credit. Live: the server takes the points and credits the card
    // balance at once (perk "P10"). Mock: the credit waits on the card as FX-RD-1025 for Fetch Load.
    // False = the server refused, nothing changed.
    public static boolean redeem() {
        DemoAccount account = getAccount();
        lastReference = MockData.nextReference("RD");
        if (MockData.isLive()) {
            try {
                Api.redeem(getCard().getCardNumber(), "P" + selected / 10);
                MockData.refresh();
            } catch (Api.ApiException e) {
                return false;
            }
        } else {
            account.deductPoints(selected);
            getCard().addPendingLoad(new PendingLoad(credit(selected), "Points", lastReference));
            account.addTransaction(DemoTransaction.REWARD, "Points Redeemed",
                    CommonUi.pts(selected) + " → transit credit", credit(selected));
        }
        account.addNotification(new DemoNotification("Rewards Claimed",
                "You have claimed **" + CommonUi.peso(credit(selected)) + "** rewards.", false));
        return true;
    }

    // ----- Leaving -----

    public static void goHome() {
        clear();
        Navigator.go("dashboard");
    }
}
