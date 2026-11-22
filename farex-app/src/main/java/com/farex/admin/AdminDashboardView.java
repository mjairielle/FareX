package com.farex.admin;

import com.farex.AdminScreen;
import com.farex.Api;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;

import java.util.function.Consumer;

/**
 * Builds the Admin Dashboard tab view with summary metrics and quick links.
 * Single Responsibility: Dashboard metrics and quick navigation presentation.
 */
public class AdminDashboardView {
    private final AdminScreen screen;
    private final VBox container;
    private final Consumer<AdminScreen.AdminTab> tabSwitcher;

    public AdminDashboardView(AdminScreen screen, VBox container, Consumer<AdminScreen.AdminTab> tabSwitcher) {
        this.screen = screen;
        this.container = container;
        this.tabSwitcher = tabSwitcher;
    }

    public void build() {
        VBox card = new VBox(8);
        card.setStyle("-fx-background-color: #1c1c1c; -fx-padding: 16; -fx-background-radius: 8;");
        Label title = screen.limeLabel("System Summary Metrics", 16);

        String summaryText;
        try {
            summaryText = Api.summary();
        } catch (Exception e) {
            summaryText = "Server summary unavailable: " + e.getMessage();
        }
        Label content = screen.textLabel(summaryText);
        content.setStyle("-fx-font-family: 'Courier New', monospace; -fx-font-size: 13; -fx-text-fill: #dddddd;");
        card.getChildren().addAll(title, content);

        VBox quickLinks = new VBox(8);
        quickLinks.getChildren().addAll(
                screen.primaryButton("Manage User Accounts & Discounts", () -> tabSwitcher.accept(AdminScreen.AdminTab.USERS)),
                screen.secondaryButton("Fleet & Vehicle Assignment", () -> tabSwitcher.accept(AdminScreen.AdminTab.VEHICLES)),
                screen.secondaryButton("Transit Routes & Stops", () -> tabSwitcher.accept(AdminScreen.AdminTab.ROUTES)),
                screen.secondaryButton("Publish Fare Tables", () -> tabSwitcher.accept(AdminScreen.AdminTab.FARES)),
                screen.secondaryButton("Reward Point Promotions", () -> tabSwitcher.accept(AdminScreen.AdminTab.PROMOTIONS))
        );

        container.getChildren().addAll(card, quickLinks);
    }
}
