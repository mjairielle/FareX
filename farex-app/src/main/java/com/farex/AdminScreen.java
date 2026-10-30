package com.farex;

import java.time.LocalDate;
import java.util.*;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;

/**
 * Admin Panel Screen - Comprehensive administrative management panel.
 * Follows flowchart 09-admin-panel.md.
 * Implements Dashboard, User Management (discounts, suspensions),
 * Vehicle Fleet, Route Management, Fare Publishing, and Rewards Promotions.
 */
public class AdminScreen extends Screen {
    private enum AdminTab { DASHBOARD, USERS, VEHICLES, ROUTES, FARES, PROMOTIONS }
    private AdminTab currentTab = AdminTab.DASHBOARD;

    private final VBox tabContent = new VBox(12);
    private final Label notice = lime("", 13);

    public AdminScreen() {
        render();
    }

    private void render() {
        getChildren().clear();
        tabContent.getChildren().clear();

        // Top Navigation Bar
        HBox nav = new HBox(4);
        nav.setAlignment(Pos.CENTER);
        for (AdminTab t : AdminTab.values()) {
            Button b = new Button(t.name().substring(0, 1) + t.name().substring(1).toLowerCase());
            b.setMaxWidth(Double.MAX_VALUE);
            HBox.setHgrow(b, Priority.ALWAYS);
            if (t == currentTab) {
                b.setStyle("-fx-background-color: #c8ff00; -fx-text-fill: black; -fx-font-weight: bold; -fx-padding: 6 8; -fx-font-size: 11;");
            } else {
                b.setStyle("-fx-background-color: #222222; -fx-text-fill: #aaaaaa; -fx-font-weight: bold; -fx-padding: 6 8; -fx-font-size: 11;");
            }
            b.setOnAction(e -> {
                currentTab = t;
                notice.setText("");
                error.setText("");
                render();
            });
            nav.getChildren().add(b);
        }

        getChildren().addAll(heading("FareX Admin Panel"), nav);

        switch (currentTab) {
            case DASHBOARD -> buildDashboard();
            case USERS -> buildUsers();
            case VEHICLES -> buildVehicles();
            case ROUTES -> buildRoutes();
            case FARES -> buildFares();
            case PROMOTIONS -> buildPromotions();
        }

        ScrollPane scroll = new ScrollPane(tabContent);
        scroll.setFitToWidth(true);
        scroll.setStyle("-fx-background: #111111; -fx-background-color: #111111; -fx-border-color: transparent;");
        setVgrow(scroll, Priority.ALWAYS);

        getChildren().addAll(scroll, notice, error, secondary("Log Out", this::logout));
    }

    // ==========================================
    // 1. DASHBOARD
    // ==========================================
    private void buildDashboard() {
        VBox card = new VBox(8);
        card.setStyle("-fx-background-color: #1c1c1c; -fx-padding: 16; -fx-background-radius: 8;");
        Label title = lime("System Summary Metrics", 16);

        String summaryText;
        try {
            summaryText = Api.summary();
        } catch (Exception e) {
            summaryText = "Server summary unavailable: " + e.getMessage();
        }
        Label content = text(summaryText);
        content.setStyle("-fx-font-family: 'Courier New', monospace; -fx-font-size: 13; -fx-text-fill: #dddddd;");
        card.getChildren().addAll(title, content);

        VBox quickLinks = new VBox(8);
        quickLinks.getChildren().addAll(
                primary("Manage User Accounts & Discounts", () -> { currentTab = AdminTab.USERS; render(); }),
                secondary("Fleet & Vehicle Assignment", () -> { currentTab = AdminTab.VEHICLES; render(); }),
                secondary("Transit Routes & Stops", () -> { currentTab = AdminTab.ROUTES; render(); }),
                secondary("Publish Fare Tables", () -> { currentTab = AdminTab.FARES; render(); }),
                secondary("Reward Point Promotions", () -> { currentTab = AdminTab.PROMOTIONS; render(); })
        );

        tabContent.getChildren().addAll(card, quickLinks);
    }

    // ==========================================
    // 2. USERS MANAGEMENT
    // ==========================================
    private void buildUsers() {
        tabContent.getChildren().addAll(
                lime("User Accounts Management", 16),
                text("Verify passenger discount status (Student, Senior, PWD) and manage account suspensions.")
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
            Label roleLbl = text("Role: " + role + "  |  Status: " + status.toUpperCase());

            HBox actions = new HBox(8);
            actions.setAlignment(Pos.CENTER_LEFT);

            if ("PASSENGER".equals(role)) {
                ComboBox<String> typeSel = combo();
                typeSel.getItems().addAll("REGULAR", "STUDENT", "SENIOR", "PWD");
                typeSel.setValue(type.isEmpty() ? "REGULAR" : type);
                typeSel.setMaxWidth(130);

                Button btnSet = primary("Set Type", () -> {
                    Api.adminSetUserType(id, typeSel.getValue());
                    notice.setText("Updated " + name + " to " + typeSel.getValue());
                    render();
                });
                btnSet.setStyle("-fx-background-color: #c8ff00; -fx-text-fill: black; -fx-font-size: 11; -fx-padding: 5 10;");

                actions.getChildren().addAll(typeSel, btnSet);
            }

            if (!"ADMIN".equals(role)) {
                boolean isSuspended = "suspended".equalsIgnoreCase(status);
                Button btnStatus = secondary(isSuspended ? "Reinstate" : "Suspend", () -> {
                    Api.adminSetUserStatus(id, isSuspended ? "REINSTATE" : "SUSPEND");
                    notice.setText((isSuspended ? "Reinstated " : "Suspended ") + name);
                    render();
                });
                btnStatus.setStyle(isSuspended ? "-fx-background-color: #1e331e; -fx-text-fill: #c8ff00; -fx-font-size: 11; -fx-padding: 5 10;"
                        : "-fx-background-color: #331e1e; -fx-text-fill: #ff6b6b; -fx-font-size: 11; -fx-padding: 5 10;");
                actions.getChildren().add(btnStatus);
            }

            row.getChildren().addAll(userHdr, roleLbl, actions);
            tabContent.getChildren().add(row);
        }
    }

    // ==========================================
    // 3. VEHICLE FLEET MANAGEMENT
    // ==========================================
    private void buildVehicles() {
        tabContent.getChildren().addAll(
                lime("Jeepney Vehicle Fleet", 16),
                text("Active vehicles assigned to operator accounts. Retire a vehicle by toggling its operator's status.")
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

                Label plateLbl = lime("Jeepney Plate: " + (plate.isEmpty() ? "Unassigned" : plate), 14);
                Label opLbl = text("Operator: " + name + " (" + email + ")");
                Label stLbl = text("Status: " + status.toUpperCase());

                boolean isSuspended = "suspended".equalsIgnoreCase(status);
                Button btnToggle = secondary(isSuspended ? "Reinstate Vehicle" : "Retire Vehicle", () -> {
                    Api.adminSetUserStatus(id, isSuspended ? "REINSTATE" : "SUSPEND");
                    notice.setText((isSuspended ? "Reinstated vehicle " : "Retired vehicle ") + plate);
                    render();
                });
                btnToggle.setStyle(isSuspended ? "-fx-background-color: #1e331e; -fx-text-fill: #c8ff00; -fx-font-size: 11; -fx-padding: 5 10;"
                        : "-fx-background-color: #331e1e; -fx-text-fill: #ff6b6b; -fx-font-size: 11; -fx-padding: 5 10;");

                row.getChildren().addAll(plateLbl, opLbl, stLbl, btnToggle);
                tabContent.getChildren().add(row);
            }
        }
    }

    // ==========================================
    // 4. ROUTE MANAGEMENT
    // ==========================================
    private void buildRoutes() {
        tabContent.getChildren().addAll(
                lime("Transit Routes & Station Network", 16),
                text("Jeepney routes currently recognized by the Fare Engine and Trip Planner.")
        );

        List<String[]> routes;
        try {
            routes = Api.adminRoutes();
        } catch (Exception e) {
            routes = Collections.emptyList();
        }

        for (String[] r : routes) {
            if (r.length < 2) continue;
            VBox row = new VBox(4);
            row.setStyle("-fx-background-color: #1c1c1c; -fx-padding: 12; -fx-background-radius: 6;");
            Label rId = lime("Route " + r[0], 14);
            Label rStops = text(r[1]);
            row.getChildren().addAll(rId, rStops);
            tabContent.getChildren().add(row);
        }

        // Form to Add New Route
        VBox addCard = new VBox(8);
        addCard.setStyle("-fx-background-color: #1c1c1c; -fx-padding: 14; -fx-background-radius: 6;");
        Label addTitle = lime("Add New Jeepney Route", 14);
        TextField routeIdField = field("Route ID, e.g. R4");
        TextArea stopsArea = new TextArea();
        stopsArea.setPromptText("One stop per line: Name, Lat, Lng\ne.g.\nPhilcoa, 14.6537, 121.0594\nTandang Sora, 14.6700, 121.0540");
        stopsArea.setPrefRowCount(4);
        stopsArea.setStyle("-fx-control-inner-background: #222222; -fx-text-fill: white; -fx-prompt-text-fill: #888888;");

        Button btnAdd = primary("Add Route", () -> {
            String id = routeIdField.getText().trim();
            String rawStops = stopsArea.getText().trim().replace("\r\n", "\n");
            String stopsParam = rawStops.replace("\n", ";");
            Api.adminAddRoute(id, stopsParam);
            notice.setText("Added route " + id + " successfully!");
            render();
        });

        addCard.getChildren().addAll(addTitle, routeIdField, stopsArea, btnAdd);
        tabContent.getChildren().add(addCard);
    }

    // ==========================================
    // 5. FARE TABLE MANAGEMENT
    // ==========================================
    private void buildFares() {
        tabContent.getChildren().addAll(
                lime("Fare Schedules & Rates", 16),
                text("Publish updated base fares and kilometer rates with future effective dates.")
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
            Label title = lime(f[0] + " Fare Schedule" + (eff.equals("-") ? " (Active)" : " (Effective: " + eff + ")"), 13);
            Label desc = text("Base Fare: " + peso(base) + "  |  Rate per KM: " + peso(rate));
            row.getChildren().addAll(title, desc);
            tabContent.getChildren().add(row);
        }

        // Form to Publish New Fare
        VBox pubCard = new VBox(8);
        pubCard.setStyle("-fx-background-color: #1c1c1c; -fx-padding: 14; -fx-background-radius: 6;");
        Label pubTitle = lime("Publish New Fare Schedule", 14);
        TextField effDateField = field("Effective Date (YYYY-MM-DD), e.g. " + LocalDate.now().plusMonths(1));
        TextField baseField = field("Base Fare in centavos (e.g. 1400 for P14.00)");
        TextField rateField = field("Rate per KM in centavos (e.g. 190 for P1.90)");

        Button btnPublish = primary("Publish Fare Schedule", () -> {
            String eff = effDateField.getText().trim();
            long b = Long.parseLong(baseField.getText().trim());
            long r = Long.parseLong(rateField.getText().trim());
            Api.adminPublishFare(eff, b, r);
            notice.setText("Published new fare schedule effective " + eff);
            render();
        });

        pubCard.getChildren().addAll(pubTitle, effDateField, baseField, rateField, btnPublish);
        tabContent.getChildren().add(pubCard);
    }

    // ==========================================
    // 6. PROMOTIONS & REWARDS
    // ==========================================
    private void buildPromotions() {
        tabContent.getChildren().addAll(
                lime("Rewards Promotions", 16),
                text("Active point multiplier promotions for passenger transit rewards.")
        );

        List<String[]> promos;
        try {
            promos = Api.adminPromos();
        } catch (Exception e) {
            promos = Collections.emptyList();
        }

        if (promos.isEmpty()) {
            tabContent.getChildren().add(text("No promotions configured."));
        } else {
            for (String[] pr : promos) {
                if (pr.length < 4) continue;
                VBox row = new VBox(4);
                row.setStyle("-fx-background-color: #1c1c1c; -fx-padding: 10; -fx-background-radius: 6;");
                Label pName = lime(pr[0] + " (x" + pr[1] + " Multiplier)", 13);
                Label pDates = text("Active: " + pr[2] + " to " + pr[3]);
                row.getChildren().addAll(pName, pDates);
                tabContent.getChildren().add(row);
            }
        }

        // Form to Add Promo
        VBox addPromoCard = new VBox(8);
        addPromoCard.setStyle("-fx-background-color: #1c1c1c; -fx-padding: 14; -fx-background-radius: 6;");
        Label addTitle = lime("Create New Promotion", 14);
        TextField nameField = field("Promo Name, e.g. Weekend Bonus");
        TextField multField = field("Multiplier, e.g. 2.0");
        TextField startField = field("Start Date (YYYY-MM-DD), e.g. " + LocalDate.now());
        TextField endField = field("End Date (YYYY-MM-DD), e.g. " + LocalDate.now().plusDays(7));

        Button btnAddPromo = primary("Activate Promotion", () -> {
            String nm = nameField.getText().trim();
            double mult = Double.parseDouble(multField.getText().trim());
            String st = startField.getText().trim();
            String en = endField.getText().trim();
            Api.adminAddPromo(nm, mult, st, en);
            notice.setText("Activated promo: " + nm);
            render();
        });

        addPromoCard.getChildren().addAll(addTitle, nameField, multField, startField, endField, btnAddPromo);
        tabContent.getChildren().add(addPromoCard);
    }
}
