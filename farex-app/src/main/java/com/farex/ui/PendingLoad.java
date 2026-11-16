package com.farex.ui;

// Load that was paid for (Buy Load) but not yet moved onto the card (Fetch Load).
// Example: ₱200.00, "GCash", "FX-LD-1024"
public class PendingLoad {

    private final double amount;
    private final String method;
    private final String reference;

    public PendingLoad(double amount, String method, String reference) {
        this.amount = amount;
        this.method = method;
        this.reference = reference;
    }

    public double getAmount() {
        return amount;
    }

    public String getMethod() {
        return method;
    }

    public String getReference() {
        return reference;
    }
}
