package com.farex.admin;

import com.farex.AdminScreen;
import com.farex.Api;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;

import java.util.Collections;
import java.util.List;

/**
 * Builds the Admin Jeepney Vehicle Fleet management tab view.
 * Single Responsibility: Fleet and vehicle assignment administration.
 */
public class AdminVehiclesView {
    private final AdminScreen screen;
    private final VBox container;
    private final Label notice;
    private final Runnable onRefresh;

    public AdminVehiclesView(AdminScreen screen, VBox container, Label notice, Runnable onRefresh) {
        this.screen = screen;
        this.container = container;
        this.notice = notice;
        this.onRefresh = onRefresh;
    }

    public void build() {
        container.getChildren().addAll(
                screen.limeLabel("Jeepney Vehicle Fleet", 16),
                screen.textLabel("Active vehicles assigned to operator accounts. Retire a vehicle by toggling its operator's status.")
        );

        List<String[]> users;
        try {
            users = Api.adminUsers();
        } catch (Exception e) {
            users = Collections.emptyList();
        }

        for (String[] u : users) {
            if (u.length >= 7 && "OPERATOR".equals(u[3])) {
                String id = u[0], name = u[1], email = u[2], plate = u[5], status = u[6];

                VBox row = new VBox(6);
                row.setStyle("-fx-background-color: #1c1c1c; -fx-padding: 12; -fx-background-radius: 6;");

                Label plateLbl = screen.limeLabel("Jeepney Plate: " + (plate.isEmpty() ? "Unassigned" : plate), 14);
                Label opLbl = screen.textLabel("Operator: " + name + " (" + email + ")");
                Label stLbl = screen.textLabel("Status: " + status.toUpperCase());

                boolean isSuspended = "suspended".equalsIgnoreCase(status);
                Button btnToggle = screen.secondaryButton(isSuspended ? "Reinstate Vehicle" : "Retire Vehicle", () -> {
                    Api.adminSetUserStatus(id, isSuspended ? "REINSTATE" : "SUSPEND");
                    notice.setText((isSuspended ? "Reinstated vehicle " : "Retired vehicle ") + plate);
                    onRefresh.run();
                });
                btnToggle.setStyle(isSuspended ? "-fx-background-color: #1e331e; -fx-text-fill: #c8ff00; -fx-font-size: 11; -fx-padding: 5 10;"
                        : "-fx-background-color: #331e1e; -fx-text-fill: #ff6b6b; -fx-font-size: 11; -fx-padding: 5 10;");

                row.getChildren().addAll(plateLbl, opLbl, stLbl, btnToggle);
                container.getChildren().add(row);
            }
        }
    }
}
