package com.farex.ui;

/**
 * Fake data so the screens work before the real backend is connected.
 * Will be replaced by the real Passenger / User classes in Part 4.
 */
public final class MockData {

    public static final String DEMO_CONTACT = "aron01@gmail.com";
    public static final String DEMO_PASSWORD = "farex123";

    // The email/number typed on the Register screen, shown later on the OTP screen
    private static String pendingContact = "";

    private MockData() {
    }

    public static boolean checkLogin(String contact, String password) {
        return DEMO_CONTACT.equalsIgnoreCase(contact) && DEMO_PASSWORD.equals(password);
    }

    public static void setPendingContact(String contact) {
        pendingContact = contact;
    }

    public static String getPendingContact() {
        return pendingContact;
    }
}
