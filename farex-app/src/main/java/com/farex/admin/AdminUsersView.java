package com.farex.admin;

import com.farex.AdminScreen;
import com.farex.Api;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

import java.util.Collections;
import java.util.List;

/**
 * Builds the Admin User Management tab view (discounts, suspensions).
 * Single Responsibility: User account and discount status controls.
 */
public class AdminUsersView {
    private final AdminScreen screen;
    private final VBox container;
    private final Label notice;
    private final Label error;
    private final Runnable onRefresh;

    public AdminUsersView(AdminScreen screen, VBox container, Label notice, Label error, Runnable onRefresh) {
        this.screen = screen;
        this.container = container;
        this.notice = notice;
        this.error = error;
        this.onRefresh = onRefresh;
    }

    public void build() {
        container.getChildren().addAll(
                screen.limeLabel("User Accounts Management", 16),
                screen.textLabel("Verify passenger discount status (Student, Senior, PWD) and manage account suspensions.")
        );

        List<String[]> users;
        try {
            users = Api.adminUsers();
        } catch (Exception e) {
            users = Collections.emptyList();
            error.setText("Error loading users: " + e.getMessage());
        }

        for (String[] u : users) {
            if (u.length < 7) continue;
            String id = u[0], name = u[1], email = u[2], role = u[3], type = u[4], status = u[6];

            VBox row = new VBox(6);
            row.setStyle("-fx-background-color: #1c1c1c; -fx-padding: 12; -fx-background-radius: 6;");

            Label userHdr = new Label(name + "  (" + email + ")");
            userHdr.setStyle("-fx-text-fill: white; -fx-font-weight: bold;");
            Label roleLbl = screen.textLabel("Role: " + role + "  |  Status: " + status.toUpperCase());

            HBox actions = new HBox(8);
            actions.setAlignment(Pos.CENTER_LEFT);

            if ("PASSENGER".equals(role)) {
                ComboBox<String> typeSel = screen.createCombo();
                typeSel.getItems().addAll("REGULAR", "STUDENT", "SENIOR", "PWD");
                typeSel.setValue(type.isEmpty() ? "REGULAR" : type);
                typeSel.setMaxWidth(130);

                Button btnSet = screen.primaryButton("Set Type", () -> {
                    Api.adminSetUserType(id, typeSel.getValue());
                    notice.setText("Updated " + name + " to " + typeSel.getValue());
                    onRefresh.run();
                });
                btnSet.setStyle("-fx-background-color: #c8ff00; -fx-text-fill: black; -fx-font-size: 11; -fx-padding: 5 10;");

                actions.getChildren().addAll(typeSel, btnSet);
            }

            if (!"ADMIN".equals(role)) {
                boolean isSuspended = "suspended".equalsIgnoreCase(status);
                Button btnStatus = screen.secondaryButton(isSuspended ? "Reinstate" : "Suspend", () -> {
                    Api.adminSetUserStatus(id, isSuspended ? "REINSTATE" : "SUSPEND");
                    notice.setText((isSuspended ? "Reinstated " : "Suspended ") + name);
                    onRefresh.run();
                });
                btnStatus.setStyle(isSuspended ? "-fx-background-color: #1e331e; -fx-text-fill: #c8ff00; -fx-font-size: 11; -fx-padding: 5 10;"
                        : "-fx-background-color: #331e1e; -fx-text-fill: #ff6b6b; -fx-font-size: 11; -fx-padding: 5 10;");
                actions.getChildren().add(btnStatus);
            }

            row.getChildren().addAll(userHdr, roleLbl, actions);
            container.getChildren().add(row);
        }
    }
}
