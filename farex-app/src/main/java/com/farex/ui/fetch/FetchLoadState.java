package com.farex.ui.fetch;

import javafx.scene.Parent;

import com.farex.Api;
import com.farex.ui.common.Navigator;
import com.farex.ui.data.DemoAccount;
import com.farex.ui.data.DemoCard;
import com.farex.ui.data.DemoNotification;
import com.farex.ui.data.MockData;
import com.farex.ui.data.PendingLoad;

// Remembers which card Fetch Load is working on, the load being fetched,
// and the balance before it was added (FetchLoadScreen shows them)
public final class FetchLoadState {

    private static DemoCard card;

    // For the result screens: the load being fetched, and the balance before it was added
    private static PendingLoad lastLoad;
    private static double previousBalance;

    private FetchLoadState() {
    }

    // The chosen card, or the account's first card if none was chosen yet
    public static DemoCard getCard() {
        if (card == null) {
            DemoAccount account = MockData.getCurrentAccount();
            card = (account == null) ? null : account.getPrimaryCard();
        }
        return card;
    }

    public static void setCard(DemoCard newCard) {
        card = newCard;
    }

    public static void clear() {
        card = null;
        lastLoad = null;
        previousBalance = 0;
    }

    public static PendingLoad getLastLoad() {
        return lastLoad;
    }

    public static void setLastLoad(PendingLoad load) {
        lastLoad = load;
    }

    public static double getPreviousBalance() {
        return previousBalance;
    }

    // The card read finished: ₱676.50 + ₱200.00 -> ₱876.50, and a "Load Fetched" message.
    // Live: the server credits it only once PayMongo says paid. False = not paid yet, load stays pending.
    public static boolean collect(PendingLoad load) {
        DemoCard current = getCard();
        lastLoad = load;
        previousBalance = current.getBalance();
        if (MockData.isLive()) {
            try {
                if (!Api.confirmLoad(load.getReference())) {
                    return false;
                }
            } catch (Api.ApiException e) {
                return false;
            }
        }
        current.collect(load);
        if (MockData.isLive()) {
            try {
                MockData.refresh();                          // the server's balance, not our sum
                card = findCard(current.getCardNumber());   // refresh made new card objects
            } catch (Api.ApiException e) {
                // paid and credited; the screen shows our sum until the next refresh
            }
        }

        DemoAccount account = MockData.getCurrentAccount();
        if (account != null) {
            account.addNotification(new DemoNotification("Load Fetched",
                    "Your load has been **successfully** fetched.", false));
        }
        return true;
    }

    private static DemoCard findCard(String number) {
        for (DemoCard c : MockData.getCurrentAccount().getCards()) {
            if (c.getCardNumber().equals(number)) return c;
        }
        return getCard();
    }

    // ----- Opening and leaving -----

    // From Home: always start again with the first card
    public static Parent start() {
        clear();
        return new FetchLoadScreen();
    }

    // Leave Fetch Load completely
    public static void goHome() {
        clear();
        Navigator.go("dashboard");
    }
}
