package com.farex.ui;

// Remembers Profile settings and the half-typed edit while the photo sheet is open.
// Fake storage until the backend keeps these.
public final class ProfileState {

    // ----- Location access choices -----
    public static final String WHILE_USING = "While using the app";
    public static final String ASK = "Ask next time";
    public static final String NEVER = "Never";

    private static String location = WHILE_USING;

    // ----- Notification toggles (Account & security is always on) -----
    private static boolean tripUpdates = true;
    private static boolean cardAlerts = true;
    private static boolean offers = false;

    // ----- Edit account draft: kept while changing the photo, cleared on Save/Cancel -----
    private static String draftName;
    private static String draftEmail;
    private static String draftPhone;

    private ProfileState() {
    }

    public static String getLocation() {
        return location;
    }

    // "While using the app" also lets the Trip Planner track without asking
    public static void setLocation(String choice) {
        location = choice;
        TripState.setLocationAllowed(WHILE_USING.equals(choice));
    }

    public static boolean isTripUpdates() {
        return tripUpdates;
    }

    public static boolean isCardAlerts() {
        return cardAlerts;
    }

    public static boolean isOffers() {
        return offers;
    }

    public static void setNotifications(boolean trip, boolean card, boolean promos) {
        tripUpdates = trip;
        cardAlerts = card;
        offers = promos;
    }

    public static boolean hasDraft() {
        return draftName != null;
    }

    public static void saveDraft(String name, String email, String phone) {
        draftName = name;
        draftEmail = email;
        draftPhone = phone;
    }

    public static String getDraftName() {
        return draftName;
    }

    public static String getDraftEmail() {
        return draftEmail;
    }

    public static String getDraftPhone() {
        return draftPhone;
    }

    public static void clearDraft() {
        draftName = null;
        draftEmail = null;
        draftPhone = null;
    }
}
