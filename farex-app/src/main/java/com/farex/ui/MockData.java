package com.farex.ui;

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

    // Load references: the demo's waiting load is FX-LD-1024, the next purchase gets FX-LD-1025
    private static int nextReference = 1025;

    static {
        DemoAccount demo = new DemoAccount(DEMO_CONTACT, DEMO_PASSWORD);
        DemoCard aron = new DemoCard("0067676767", "Aron Ferrancullo", 676.50, "09/29");
        aron.addPendingLoad(new PendingLoad(200.00, "GCash", "FX-LD-1024"));   // ready to fetch
        demo.addCard(aron);
        demo.addCard(new DemoCard("0011111111", "Kenn Urbano", 150.00, "10/29"));
        demo.addCard(new DemoCard("0031313131", "Andrei Ng", 0.00, "11/29"));
        addSampleMessages(demo, true);    // bottom: "Other Messages"
        addSampleMessages(demo, false);   // top: "Latest"
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

    public static void setPendingContact(String contact) {
        pendingContact = contact;
    }

    public static String getPendingContact() {
        return pendingContact;
    }

    // "FX-LD-1025", then "FX-LD-1026", ...
    public static String nextReference() {
        return "FX-LD-" + nextReference++;
    }
}
