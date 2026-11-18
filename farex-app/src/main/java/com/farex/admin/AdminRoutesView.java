package com.farex.admin;

import com.farex.AdminScreen;
import com.farex.Api;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;

import java.util.Collections;
import java.util.List;

/**
 * Builds the Admin Transit Routes management tab view.
 * Single Responsibility: Route and stop sequence administration.
 */
public class AdminRoutesView {
    private final AdminScreen screen;
    private final VBox container;
    private final Label notice;
    private final Runnable onRefresh;

    public AdminRoutesView(AdminScreen screen, VBox container, Label notice, Runnable onRefresh) {
        this.screen = screen;
        this.container = container;
        this.notice = notice;
        this.onRefresh = onRefresh;
    }

    public void build() {
        container.getChildren().addAll(
                screen.limeLabel("Transit Routes & Station Network", 16),
                screen.textLabel("Jeepney routes currently recognized by the Fare Engine and Trip Planner.")
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
            Label rId = screen.limeLabel("Route " + r[0], 14);
            Label rStops = screen.textLabel(r[1]);
            row.getChildren().addAll(rId, rStops);
            container.getChildren().add(row);
        }

        // Form to Add New Route
        VBox addCard = new VBox(8);
        addCard.setStyle("-fx-background-color: #1c1c1c; -fx-padding: 14; -fx-background-radius: 6;");
        Label addTitle = screen.limeLabel("Add New Jeepney Route", 14);
        TextField routeIdField = screen.createField("Route ID, e.g. R4");
        TextArea stopsArea = new TextArea();
        stopsArea.setPromptText("One stop per line: Name, Lat, Lng\ne.g.\nPhilcoa, 14.65400, 121.05377\nTandang Sora, 14.66223, 121.06619");
        stopsArea.setPrefRowCount(4);
        stopsArea.setStyle("-fx-control-inner-background: #222222; -fx-text-fill: white; -fx-prompt-text-fill: #888888;");

        Button btnAdd = screen.primaryButton("Add Route", () -> {
            String id = routeIdField.getText().trim();
            String rawStops = stopsArea.getText().trim().replace("\r\n", "\n");
            String stopsParam = rawStops.replace("\n", ";");
            Api.adminAddRoute(id, stopsParam);
            notice.setText("Added route " + id + " successfully!");
            onRefresh.run();
        });

        addCard.getChildren().addAll(addTitle, routeIdField, stopsArea, btnAdd);
        container.getChildren().add(addCard);
    }
}
