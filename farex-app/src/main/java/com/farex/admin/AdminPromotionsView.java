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
 * Builds the Admin Promotions & Rewards management tab view.
 * Single Responsibility: Transit rewards multiplier promotions administration.
 */
public class AdminPromotionsView {
    private final AdminScreen screen;
    private final VBox container;
    private final Label notice;
    private final Runnable onRefresh;

    public AdminPromotionsView(AdminScreen screen, VBox container, Label notice, Runnable onRefresh) {
        this.screen = screen;
        this.container = container;
        this.notice = notice;
        this.onRefresh = onRefresh;
    }

    public void build() {
        container.getChildren().addAll(
                screen.limeLabel("Rewards Promotions", 16),
                screen.textLabel("Active point multiplier promotions for passenger transit rewards.")
        );

        List<String[]> promos;
        try {
            promos = Api.adminPromos();
        } catch (Exception e) {
            promos = Collections.emptyList();
        }

        if (promos.isEmpty()) {
            container.getChildren().add(screen.textLabel("No promotions configured."));
        } else {
            for (String[] pr : promos) {
                if (pr.length < 4) continue;
                VBox row = new VBox(4);
                row.setStyle("-fx-background-color: #1c1c1c; -fx-padding: 10; -fx-background-radius: 6;");
                Label pName = screen.limeLabel(pr[0] + " (x" + pr[1] + " Multiplier)", 13);
                Label pDates = screen.textLabel("Active: " + pr[2] + " to " + pr[3]);
                row.getChildren().addAll(pName, pDates);
                container.getChildren().add(row);
            }
        }

        // Form to Add Promo
        VBox addPromoCard = new VBox(8);
        addPromoCard.setStyle("-fx-background-color: #1c1c1c; -fx-padding: 14; -fx-background-radius: 6;");
        Label addTitle = screen.limeLabel("Create New Promotion", 14);
        TextField nameField = screen.createField("Promo Name, e.g. Weekend Bonus");
        TextField multField = screen.createField("Multiplier, e.g. 2.0");
        TextField startField = screen.createField("Start Date (YYYY-MM-DD), e.g. " + LocalDate.now());
        TextField endField = screen.createField("End Date (YYYY-MM-DD), e.g. " + LocalDate.now().plusDays(7));

        Button btnAddPromo = screen.primaryButton("Activate Promotion", () -> {
            String nm = nameField.getText().trim();
            double mult = Double.parseDouble(multField.getText().trim());
            String st = startField.getText().trim();
            String en = endField.getText().trim();
            Api.adminAddPromo(nm, mult, st, en);
            notice.setText("Activated promo: " + nm);
            onRefresh.run();
        });

        addPromoCard.getChildren().addAll(addTitle, nameField, multField, startField, endField, btnAddPromo);
        container.getChildren().add(addPromoCard);
    }
}
