package com.farex.ui.data;

import java.time.LocalDate;
import java.time.LocalDateTime;

// One line on the Transactions tab. Fake data until the backend keeps the real history.
// Example: Buy Load, "3:00 PM | GCash", +500.00 on Sept 25, 2026
public class DemoTransaction {

    // ----- Kinds (each has its own icon) -----
    public static final String LOAD = "load";          // lime wallet
    public static final String FARE = "fare";          // cyan jeepney
    public static final String TRANSFER = "transfer";  // salmon card
    public static final String REWARD = "reward";      // trophy

    private final LocalDateTime when;
    private final String kind;
    private final String title;
    private final String detail;
    private final double amount;   // + money in, - money out

    public DemoTransaction(LocalDateTime when, String kind, String title, String detail, double amount) {
        this.when = when;
        this.kind = kind;
        this.title = title;
        this.detail = detail;
        this.amount = amount;
    }

    public LocalDate getDate() {
        return when.toLocalDate();
    }

    public String getKind() {
        return kind;
    }

    public String getTitle() {
        return title;
    }

    public String getDetail() {
        return detail;
    }

    public double getAmount() {
        return amount;
    }

    public String getIconName() {
        switch (kind) {
            case LOAD:
                return "icon_tx_load";
            case FARE:
                return "icon_tx_fare";
            case TRANSFER:
                return "icon_tx_transfer";
            default:
                return "icon_trophy";
        }
    }
}
