package com.farex.ui.data;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Fake data so the screens work before the real backend is connected.
 * To be removed once the logic comes from the backend.
 *
 * - aron01@gmail.com / farex123 is a demo account that already has cards.
 * - Any account registered in the app can log in later (until the app closes)
 *   and starts with no card.
 */
public final class MockData {

    public static final String DEMO_CONTACT = "aron01@gmail.com";
    public static final String DEMO_PASSWORD = "farex123";

    private static final Map<String, DemoAccount> accounts = new LinkedHashMap<>();
    private static DemoAccount currentAccount;

    // The email/number typed on the Register screen, shown later on the OTP screen
    private static String pendingContact = "";

    // References: the demo's waiting load is FX-LD-1024, the next one gets 1025
    private static int nextReference = 1025;

    static {
        DemoAccount demo = new DemoAccount(DEMO_CONTACT, DEMO_PASSWORD);
        demo.setFullName("Aron Ferrancullo");
        demo.setPhone("09123456789");
        DemoCard aron = new DemoCard("0067676767", "Aron Ferrancullo", 676.50, "09/29");
        aron.addPendingLoad(new PendingLoad(200.00, "GCash", "FX-LD-1024"));   // ready to fetch
        demo.addCard(aron);
        demo.addPoints(250);
        demo.addCard(new DemoCard("0011111111", "Kenn Urbano", 150.00, "10/29"));
        demo.addCard(new DemoCard("0031313131", "Andrei Ng", 0.00, "11/29"));
        addSampleMessages(demo, true);    // bottom: "Other Messages"
        addSampleMessages(demo, false);   // top: "Latest"
        addSampleTransactions(demo);
        accounts.put(DEMO_CONTACT.toLowerCase(), demo);
    }

    // The three messages from the Figma. Each one goes on top, so they are added last-first.
    private static void addSampleMessages(DemoAccount account, boolean read) {
        account.addNotification(new DemoNotification("Load Fetched",
                "Your load has been **successfully** fetched.", read));
        account.addNotification(new DemoNotification("Rewards Claimed",
                "You have claimed **₱50.00** rewards.", read));
        account.addNotification(new DemoNotification("Buy Load Transaction",
                "You have paid **₱300.00** using GCash.", read));
    }

    // The Transactions history from the Figma (Sept 23-25, 2026), oldest first so the newest ends on top
    private static void addSampleTransactions(DemoAccount account) {
        String trip = "Location → Location";
        sample(account, "2026-09-23T07:10", DemoTransaction.FARE, "Pay Fare", trip, -13);
        sample(account, "2026-09-23T09:30", DemoTransaction.FARE, "Pay Fare", trip, -21);
        sample(account, "2026-09-23T13:15", DemoTransaction.FARE, "Pay Fare", trip, -11);
        sample(account, "2026-09-23T17:40", DemoTransaction.FARE, "Pay Fare", trip, -15);
        sample(account, "2026-09-24T08:05", DemoTransaction.FARE, "Pay Fare", trip, -11);
        sample(account, "2026-09-24T14:20", DemoTransaction.TRANSFER, "Balance Transfer",
                "Old xPress Card → New xPress Card", 325);
        sample(account, "2026-09-25T08:43", DemoTransaction.FARE, "Pay Fare", "8:43 AM | " + trip, -35);
        sample(account, "2026-09-25T10:00", DemoTransaction.LOAD, "Buy Load", "10:00 AM | BPI", 200);
        sample(account, "2026-09-25T15:00", DemoTransaction.LOAD, "Buy Load", "3:00 PM | GCash", 500);
    }

    private static void sample(DemoAccount account, String when, String kind, String title,
                               String detail, double amount) {
        account.addTransaction(new DemoTransaction(LocalDateTime.parse(when), kind, title, detail, amount));
    }

    private MockData() {
    }

    /** Checks the login. When it succeeds, that account becomes the logged-in one. */
    public static boolean checkLogin(String contact, String password) {
        DemoAccount account = accounts.get(contact.trim().toLowerCase());
        if (account != null && account.passwordMatches(password)) {
            currentAccount = account;
            return true;
        }
        return false;
    }

    /** Called by the Register screen. An existing contact is left untouched. */
    public static void registerAccount(String contact, String password) {
        accounts.putIfAbsent(contact.trim().toLowerCase(), new DemoAccount(contact.trim(), password));
    }

    public static DemoAccount getCurrentAccount() {
        return currentAccount;
    }

    public static void logout() {
        currentAccount = null;
    }

    /** Delete account: removes it until the app restarts, so its login stops working. */
    public static void deleteCurrentAccount() {
        if (currentAccount != null) {
            accounts.remove(currentAccount.getContact().toLowerCase());
            currentAccount = null;
        }
    }

    // Every card in every account with its owner, e.g. 0067676767 -> Aron's account.
    // The Operator's tap terminal uses it to charge fares to real demo cards.
    public static Map<DemoCard, DemoAccount> allCards() {
        Map<DemoCard, DemoAccount> cards = new LinkedHashMap<>();
        for (DemoAccount account : accounts.values()) {
            for (DemoCard card : account.getCards()) {
                cards.put(card, account);
            }
        }
        return cards;
    }

    public static void setPendingContact(String contact) {
        pendingContact = contact;
    }

    public static String getPendingContact() {
        return pendingContact;
    }

    // "FX-LD-1025", then "FX-LD-1026", ...
    public static String nextReference() {
        return nextReference("LD");
    }

    // One counter for every kind: load "LD", redeem "RD", ... -> "FX-RD-1025"
    public static String nextReference(String kind) {
        return "FX-" + kind + "-" + nextReference++;
    }
}
