package com.farex.ui.operator;

import java.time.LocalTime;

// One fare collected this shift, e.g. 7:42 AM · **** 6767 · Philcoa → Batasan · ₱28.01.
// Saved on the device until it is synced (Part 27 shows these in History).
public class ShiftRecord {

    private final LocalTime time;
    private final boolean app;
    private final String who;          // "**** 6767" or "APP-0821"
    private final String from;
    private final String to;           // "Route end" for a max fare
    private final double km;
    private final long cents;          // what was charged
    private final long baseCents;      // regular fare before a discount
    private final Double balanceAfter; // null when unknown (app user offline)
    private final boolean maxFare;
    private boolean paymentPending;    // app user offline: not confirmed until sync
    private boolean synced = false;

    public ShiftRecord(boolean app, String who, String from, String to, double km, long cents,
                       long baseCents, Double balanceAfter, boolean maxFare, boolean paymentPending) {
        this(LocalTime.now(), app, who, from, to, km, cents, baseCents, balanceAfter, maxFare, paymentPending);
    }

    // With a set time, for the sample records of an earlier shift
    public ShiftRecord(LocalTime time, boolean app, String who, String from, String to, double km, long cents,
                       long baseCents, Double balanceAfter, boolean maxFare, boolean paymentPending) {
        this.time = time;
        this.app = app;
        this.who = who;
        this.from = from;
        this.to = to;
        this.km = km;
        this.cents = cents;
        this.baseCents = baseCents;
        this.balanceAfter = balanceAfter;
        this.maxFare = maxFare;
        this.paymentPending = paymentPending;
    }

    public LocalTime getTime() {
        return time;
    }

    public boolean isApp() {
        return app;
    }

    public String getWho() {
        return who;
    }

    public String getFrom() {
        return from;
    }

    public String getTo() {
        return to;
    }

    public double getKm() {
        return km;
    }

    public long getCents() {
        return cents;
    }

    public long getBaseCents() {
        return baseCents;
    }

    public long getDiscountCents() {
        return baseCents - cents;
    }

    public Double getBalanceAfter() {
        return balanceAfter;
    }

    public boolean isMaxFare() {
        return maxFare;
    }

    public boolean isPaymentPending() {
        return paymentPending;
    }

    public boolean isSynced() {
        return synced;
    }

    // After a successful sync: sent to the server, app payments confirmed
    public void markSynced() {
        synced = true;
        paymentPending = false;
    }
}
