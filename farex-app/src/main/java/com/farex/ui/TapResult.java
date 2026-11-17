package com.farex.ui;

// What a tap ended in, handed to TapResultDialog to show the matching pop-up (O3 to O6).
// Example: TapResult.tapOut(record for Philcoa → Batasan, 7.5 km) -> "Fare charged ₱28.01"
public class TapResult {

    public enum Kind {
        TAP_IN,          // O3  card tap in
        APP_TAP_IN,      // O3a app tap in, online
        APP_OFFLINE,     // O3b app tap in, no connection
        TAP_OUT,         // O4  fare charged
        INSUFFICIENT,    // O5a
        UNREADABLE,      // O5b
        INVALID_QR,      // O5c
        MAX_FARE,        // O6  no tap out before the route ended
        BLOCKED          // same look as O5: frozen card, jeep full, end of route
    }

    final Kind kind;
    String who = "";           // "**** 6767" or "APP-0821"
    boolean app;
    Double balance;            // null when unknown
    String stop = "";          // "Philcoa" (boarded at)
    String note = "";          // "Logged: 7:42 AM · GPS saved"
    long fareCents;
    long baseCents;
    double km;
    String from = "";
    String to = "";
    boolean estimated;         // app user offline: fare not confirmed yet
    String title = "";         // BLOCKED only
    String text = "";          // BLOCKED only
    int othersCharged;         // MAX_FARE: how many more passengers got a max fare

    private TapResult(Kind kind) {
        this.kind = kind;
    }

    // ----- Boarding -----

    static TapResult tapIn(TapRider rider, String stop, String note) {
        TapResult r = new TapResult(rider.isApp() ? Kind.APP_TAP_IN : Kind.TAP_IN);
        r.fillRider(rider);
        r.balance = rider.getBalance();
        r.stop = stop;
        r.note = note;
        return r;
    }

    static TapResult appOffline(TapRider rider, String stop) {
        TapResult r = new TapResult(Kind.APP_OFFLINE);
        r.fillRider(rider);
        r.stop = stop;
        return r;
    }

    // ----- Paying -----

    static TapResult tapOut(ShiftRecord record) {
        TapResult r = new TapResult(Kind.TAP_OUT);
        r.fillFare(record);
        return r;
    }

    static TapResult maxFare(ShiftRecord record, int others) {
        TapResult r = new TapResult(Kind.MAX_FARE);
        r.fillFare(record);
        r.othersCharged = others;
        return r;
    }

    // ----- Errors -----

    static TapResult insufficient() {
        return new TapResult(Kind.INSUFFICIENT);
    }

    static TapResult unreadable() {
        return new TapResult(Kind.UNREADABLE);
    }

    static TapResult invalidQr() {
        return new TapResult(Kind.INVALID_QR);
    }

    // "Card is frozen" / "This card was reported lost..."
    static TapResult blocked(String title, String text) {
        TapResult r = new TapResult(Kind.BLOCKED);
        r.title = title;
        r.text = text;
        return r;
    }

    // ----- Helpers -----

    private void fillRider(TapRider rider) {
        who = rider.getShortId();
        app = rider.isApp();
    }

    private void fillFare(ShiftRecord record) {
        who = record.getWho();
        app = record.isApp();
        fareCents = record.getCents();
        baseCents = record.getBaseCents();
        km = record.getKm();
        from = record.getFrom();
        to = record.getTo();
        balance = record.getBalanceAfter();
        estimated = record.isPaymentPending();
    }
}
