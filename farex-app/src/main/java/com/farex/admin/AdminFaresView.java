package com.farex.admin;

import com.farex.AdminScreen;
import com.farex.Api;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;

import java.time.LocalDate;
import java.util.Collections;
import java.util.List;

/**
 * Builds the Admin Fare Schedules management tab view.
 * Single Responsibility: Fare schedule publishing and rates management.
 */
public class AdminFaresView {
    private final AdminScreen screen;
    private final VBox container;
    private final Label notice;
    private final Runnable onRefresh;

    public AdminFaresView(AdminScreen screen, VBox container, Label notice, Runnable onRefresh) {
        this.screen = screen;
        this.container = container;
        this.notice = notice;
        this.onRefresh = onRefresh;
    }

    public void build() {
        container.getChildren().addAll(
                screen.limeLabel("Fare Schedules & Rates", 16),
                screen.textLabel("Publish updated base fares and kilometer rates with future effective dates.")
        );

        List<String[]> fares;
        try {
            fares = Api.adminFares();
        } catch (Exception e) {
            fares = Collections.emptyList();
        }

        for (String[] f : fares) {
            if (f.length < 3) continue;
            VBox row = new VBox(4);
            row.setStyle("-fx-background-color: #1c1c1c; -fx-padding: 10; -fx-background-radius: 6;");
            long base = Long.parseLong(f[1]);
            long rate = Long.parseLong(f[2]);
            String eff = f.length > 3 ? f[3] : "-";
            Label title = screen.limeLabel(f[0] + " Fare Schedule" + (eff.equals("-") ? " (Active)" : " (Effective: " + eff + ")"), 13);
            Label desc = screen.textLabel("Base Fare: " + screen.formatPeso(base) + "  |  Rate per KM: " + screen.formatPeso(rate));
            row.getChildren().addAll(title, desc);
            container.getChildren().add(row);
        }

        // Form to Publish New Fare
        VBox pubCard = new VBox(8);
        pubCard.setStyle("-fx-background-color: #1c1c1c; -fx-padding: 14; -fx-background-radius: 6;");
        Label pubTitle = screen.limeLabel("Publish New Fare Schedule", 14);
        TextField effDateField = screen.createField("Effective Date (YYYY-MM-DD), e.g. " + LocalDate.now().plusMonths(1));
        TextField baseField = screen.createField("Base Fare in centavos (e.g. 1400 for P14.00)");
        TextField rateField = screen.createField("Rate per KM in centavos (e.g. 190 for P1.90)");

        Button btnPublish = screen.primaryButton("Publish Fare Schedule", () -> {
            String eff = effDateField.getText().trim();
            long b = Long.parseLong(baseField.getText().trim());
            long r = Long.parseLong(rateField.getText().trim());
            Api.adminPublishFare(eff, b, r);
            notice.setText("Published new fare schedule effective " + eff);
            onRefresh.run();
        });

        pubCard.getChildren().addAll(pubTitle, effDateField, baseField, rateField, btnPublish);
        container.getChildren().add(pubCard);
    }
}
