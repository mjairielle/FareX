package com.farex.ui;

// Remembers the Redeem choice between screens and does the redemption itself.
// The credit goes to the account's first card as a pending load, collected later with Fetch Load.
public final class RedeemState {

    public static final int[] CHOICES = {50, 100, 250};
    private static final int DEFAULT_CHOICE = 100;

    private static int selected = DEFAULT_CHOICE;
    private static String lastReference = "";

    private RedeemState() {
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

    // 100 pts -> 250 becomes 150, ₱100.00 waits on the card as FX-RD-1025
    public static void redeem() {
        DemoAccount account = getAccount();
        lastReference = MockData.nextReference("RD");
        account.deductPoints(selected);
        getCard().addPendingLoad(new PendingLoad(selected, "Points", lastReference));
        account.addNotification(new DemoNotification("Rewards Claimed",
                "You have claimed **" + FetchUi.peso(selected) + "** rewards.", false));
    }

    // ----- Leaving -----

    public static void goHome() {
        clear();
        Navigator.go("dashboard");
    }
}
