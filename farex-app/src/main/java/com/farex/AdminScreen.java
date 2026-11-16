package com.farex;

import com.farex.admin.*;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;

/**
 * Admin Screen - Host screen and navigation controller for FareX administration.
 * Single Responsibility: Admin tab coordination and high-level layout.
 */
public class AdminScreen extends Screen {
    public enum AdminTab { DASHBOARD, USERS, VEHICLES, ROUTES, FARES, PROMOTIONS }
    private AdminTab currentTab = AdminTab.DASHBOARD;

    private final VBox tabContent = new VBox(12);
    private final Label notice = lime("", 13);

    public AdminScreen() {
        render();
    }

    public void switchTab(AdminTab t) {
        this.currentTab = t;
        notice.setText("");
        error.setText("");
        render();
    }

    public void render() {
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
            b.setOnAction(e -> switchTab(t));
            nav.getChildren().add(b);
        }

        getChildren().addAll(heading("FareX Admin Panel"), nav);

        switch (currentTab) {
            case DASHBOARD -> new AdminDashboardView(this, tabContent, this::switchTab).build();
            case USERS -> new AdminUsersView(this, tabContent, notice, error, this::render).build();
            case VEHICLES -> new AdminVehiclesView(this, tabContent, notice, this::render).build();
            case ROUTES -> new AdminRoutesView(this, tabContent, notice, this::render).build();
            case FARES -> new AdminFaresView(this, tabContent, notice, this::render).build();
            case PROMOTIONS -> new AdminPromotionsView(this, tabContent, notice, this::render).build();
        }

        ScrollPane scroll = new ScrollPane(tabContent);
        scroll.setFitToWidth(true);
        scroll.setStyle("-fx-background: #111111; -fx-background-color: #111111; -fx-border-color: transparent;");
        setVgrow(scroll, Priority.ALWAYS);

        getChildren().addAll(scroll, notice, error, secondary("Log Out", this::logout));
    }

    // Bridge helpers for tab views
    public Label limeLabel(String t, int size) { return lime(t, size); }
    public Label textLabel(String t) { return text(t); }
    public Button primaryButton(String t, Runnable a) { return primary(t, a); }
    public Button secondaryButton(String t, Runnable a) { return secondary(t, a); }
    public TextField createField(String prompt) { return field(prompt); }
    public <T> ComboBox<T> createCombo() { return combo(); }
    public String formatPeso(long cents) { return peso(cents); }

}
