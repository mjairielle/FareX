package com.farex.ui.common;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Polygon;

import com.farex.Api;

// The route drop-down for a new operator (sign-up and "Become an operator"): which route their jeepney runs.
// A field that opens a searchable list right under it, in place, so it also works inside a pop-up
// (only one overlay shows at a time). Routes come from the server, off the UI thread.
public class RoutePicker extends VBox {

    private final Label value = new Label("Loading routes…");
    private final VBox list = new VBox(4);
    private final TextField search = new TextField();
    private final VBox dropDown;
    private List<String[]> routes = new ArrayList<>();   // {id, name}
    private String[] chosen;

    public RoutePicker() {
        super(6);
        value.setTextFill(Theme.GRAY);
        value.setFont(Theme.bold(15));
        value.setWrapText(true);
        value.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(value, Priority.ALWAYS);
        Polygon chevron = new Polygon(0, 0, 14, 0, 7, 8);   // drawn: the phone font has no ▾
        chevron.setFill(Theme.WHITE);

        HBox field = new HBox(10, value, chevron);
        field.setAlignment(Pos.CENTER_LEFT);
        field.setPadding(new Insets(16));
        field.setMinHeight(56);                                   // a big target: this is the whole tap area
        field.setBackground(Theme.rounded(Theme.FIELD, 10));
        field.setCursor(Cursor.HAND);
        field.setOnMouseClicked(e -> toggle());

        Theme.inputField(search, "Search a route or place");
        search.textProperty().addListener((obs, was, now) -> fill());
        ScrollPane scroll = new ScrollPane(list);
        scroll.setFitToWidth(true);
        // At least four rows show (a squeezed list was one row high and hard to hit), up to about six
        scroll.setPrefViewportHeight(340);
        scroll.setMinHeight(240);
        scroll.setMaxHeight(360);
        scroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scroll.setStyle("-fx-background: #1C1C1C; -fx-background-color: transparent;");
        dropDown = new VBox(6, search, scroll);
        dropDown.setVisible(false);
        dropDown.managedProperty().bind(dropDown.visibleProperty());

        getChildren().addAll(field, dropDown);
        load();
    }

    /** True while the list is open: a screen can make room for it (it is tall). */
    public javafx.beans.value.ObservableBooleanValue openProperty() {
        return dropDown.visibleProperty();
    }

    /** The chosen route id, or "" when none was picked. */
    public String routeId() {
        return chosen == null ? "" : chosen[0];
    }

    /** True when routes loaded, so a route must be picked (offline demo sign-up has none to pick). */
    public boolean hasRoutes() {
        return !routes.isEmpty();
    }

    private void load() {
        Thread t = new Thread(() -> {
            List<String[]> loaded;
            try {
                loaded = Api.routeChoices();
            } catch (Api.ApiException e) {
                loaded = List.of();
            }
            List<String[]> result = loaded;
            Platform.runLater(() -> {
                routes = result;
                value.setText(routes.isEmpty() ? "Routes unavailable (no server)" : "Choose your route");
                fill();
            });
        }, "farex-route-list");
        t.setDaemon(true);
        t.start();
    }

    private void toggle() {
        if (routes.isEmpty()) {
            return;
        }
        dropDown.setVisible(!dropDown.isVisible());
        if (dropDown.isVisible()) {
            search.requestFocus();
        }
    }

    // The rows matching the search, e.g. "Commonwealth–SM Fairview via Regalado (to SM City Fairview)"
    private void fill() {
        String term = search.getText().trim().toLowerCase(Locale.ROOT);
        list.getChildren().clear();
        for (String[] r : routes) {
            if (!term.isEmpty() && !(r[1] + " " + r[0]).toLowerCase(Locale.ROOT).contains(term)) {
                continue;
            }
            Label row = CommonUi.small(r[1], Theme.WHITE);
            row.setFont(javafx.scene.text.Font.font(Theme.FONT, 14));
            row.setWrapText(true);
            row.setMaxWidth(Double.MAX_VALUE);
            row.setMinHeight(52);                                 // a finger-sized row (Android asks for 48)
            row.setPadding(new Insets(14, 14, 14, 14));
            row.setBackground(Theme.rounded(r == chosen ? Color.web("#2E3A10") : Color.web("#262626"), 8));
            row.setCursor(Cursor.HAND);
            row.setOnMouseClicked(e -> {
                chosen = r;
                value.setText(r[1]);
                value.setTextFill(Theme.WHITE);
                dropDown.setVisible(false);
                fill();
            });
            list.getChildren().add(row);
        }
        if (list.getChildren().isEmpty()) {
            list.getChildren().add(CommonUi.small("No route matches.", Theme.GRAY));
        }
    }
}
