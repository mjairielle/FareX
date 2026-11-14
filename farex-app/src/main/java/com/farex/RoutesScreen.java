package com.farex;

import farex.shared.data.RouteStop;
import farex.shared.fare.*;
import farex.shared.user.Passenger;
import java.util.*;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;

/**
 * Passenger Routes Screen - Browse and search jeepney routes, view stops,
 * fare table, and active vehicles on each route.
 */
public class RoutesScreen extends Screen {
    private final ComboBox<String> routePicker = combo();
    private final VBox routeContent = new VBox(10);
    private List<Api.RouteDetail> routeList = new ArrayList<>();
    private List<Api.VehicleInfo> vehicleList = new ArrayList<>();
    private Api.FareRuleInfo currentFare;

    public RoutesScreen() {
        getChildren().addAll(
                back(() -> Nav.show(new PassengerHome())),
                heading("Jeepney Routes"),
                text("Browse Quezon City transit routes, stops, and fare tables.")
        );

        try {
            routeList = Api.routes();
            vehicleList = Api.vehicles();
            currentFare = Api.currentFares();
        } catch (Exception e) {
            error.setText("Error loading routes: " + e.getMessage());
        }

        if (routeList.isEmpty()) {
            getChildren().add(text("No routes found."));
            return;
        }

        for (Api.RouteDetail rd : routeList) {
            String label = rd.id() + ": " + rd.stops().get(0).name() + " ➔ " + rd.stops().get(rd.stops().size() - 1).name();
            routePicker.getItems().add(label);
        }

        routePicker.valueProperty().addListener((obs, oldVal, newVal) -> renderRouteDetail());
        routePicker.setValue(routePicker.getItems().get(0));

        ScrollPane scroll = new ScrollPane(routeContent);
        scroll.setFitToWidth(true);
        scroll.setStyle("-fx-background: #111111; -fx-background-color: #111111; -fx-border-color: transparent;");
        setVgrow(scroll, Priority.ALWAYS);

        getChildren().addAll(text("Select Route:"), routePicker, scroll, error);
    }

    private void renderRouteDetail() {
        routeContent.getChildren().clear();
        int selIdx = routePicker.getSelectionModel().getSelectedIndex();
        if (selIdx < 0 || selIdx >= routeList.size()) return;

        Api.RouteDetail rd = routeList.get(selIdx);
        List<RouteStop> stops = rd.stops();

        // 1. Route Overview Card
        VBox overviewCard = new VBox(6);
        overviewCard.setStyle("-fx-background-color: #1c1c1c; -fx-padding: 14; -fx-background-radius: 8;");
        Label rId = lime("Route " + rd.id(), 18);
        double totalKm = Fares.pathKm(stops, 0, stops.size() - 1);
        Label rSummary = text(stops.size() + " stops  |  " + String.format(Locale.ROOT, "%.1f km total route length", totalKm));
        overviewCard.getChildren().addAll(rId, rSummary);

        // 2. Active Jeepneys on this Route
        VBox vehicleCard = new VBox(6);
        vehicleCard.setStyle("-fx-background-color: #1c1c1c; -fx-padding: 14; -fx-background-radius: 8;");
        Label vTitle = lime("Active Jeepneys on Route", 14);
        vehicleCard.getChildren().add(vTitle);

        if (vehicleList.isEmpty()) {
            vehicleCard.getChildren().add(text("No vehicles currently logged on this route."));
        } else {
            for (Api.VehicleInfo vi : vehicleList) {
                HBox vRow = new HBox(10);
                vRow.setAlignment(Pos.CENTER_LEFT);
                Label plate = new Label("Jeepney " + vi.plate());
                plate.setStyle("-fx-text-fill: white; -fx-font-weight: bold;");
                Label driver = text("Driver: " + vi.driverName());
                Region spacer = new Region();
                HBox.setHgrow(spacer, Priority.ALWAYS);
                Label status = lime("● In Service", 11);
                vRow.getChildren().addAll(plate, driver, spacer, status);
                vehicleCard.getChildren().add(vRow);
            }
        }

        // 3. Stops in Sequence
        VBox stopsCard = new VBox(6);
        stopsCard.setStyle("-fx-background-color: #1c1c1c; -fx-padding: 14; -fx-background-radius: 8;");
        Label sTitle = lime("Route Stops & Distance", 14);
        stopsCard.getChildren().add(sTitle);

        for (int i = 0; i < stops.size(); i++) {
            RouteStop s = stops.get(i);
            double legKm = i > 0 ? Fares.pathKm(stops, i - 1, i) : 0;
            double accKm = i > 0 ? Fares.pathKm(stops, 0, i) : 0;

            HBox sRow = new HBox(8);
            sRow.setAlignment(Pos.CENTER_LEFT);
            Label dot = lime("●", 12);
            Label name = new Label((i + 1) + ". " + s.name());
            name.setStyle("-fx-text-fill: white; -fx-font-size: 13;");
            Region sp = new Region();
            HBox.setHgrow(sp, Priority.ALWAYS);
            Label km = text(i == 0 ? "Start" : String.format(Locale.ROOT, "+%.1f km (%.1f km)", legKm, accKm));
            sRow.getChildren().addAll(dot, name, sp, km);
            stopsCard.getChildren().add(sRow);
        }

        // 4. Fare Table Card
        VBox fareCard = new VBox(6);
        fareCard.setStyle("-fx-background-color: #1c1c1c; -fx-padding: 14; -fx-background-radius: 8;");
        Label fTitle = lime("Fare Matrix (Base ₱" + (currentFare.base() / 100.0) + " + ₱" + (currentFare.rate() / 100.0) + "/km)", 14);
        fareCard.getChildren().add(fTitle);

        Passenger passenger = Session.user instanceof Passenger p ? p : null;
        String userDiscountType = passenger != null ? passenger.getPassengerType() : "REGULAR";

        for (int i = 1; i < stops.size(); i++) {
            double km = Fares.pathKm(stops, 0, i);
            long regFare = Fares.fare(km, new RegularFare());
            long discFare = Fares.fare(km, new DiscountedFare());

            HBox fRow = new HBox(8);
            fRow.setAlignment(Pos.CENTER_LEFT);
            Label dest = text("To " + stops.get(i).name() + " (" + String.format(Locale.ROOT, "%.1f km", km) + "):");
            Region sp = new Region();
            HBox.setHgrow(sp, Priority.ALWAYS);

            Label reg = text("Regular: " + peso(regFare));
            Label disc = lime("20% Off: " + peso(discFare), 12);
            fRow.getChildren().addAll(dest, sp, reg, disc);
            fareCard.getChildren().add(fRow);
        }

        if (!userDiscountType.equals("REGULAR")) {
            fareCard.getChildren().add(lime("Your account has " + userDiscountType + " 20% discount applied automatically.", 12));
        }

        routeContent.getChildren().addAll(overviewCard, vehicleCard, stopsCard, fareCard);
    }
<<<<<<< Updated upstream
}
=======
}
>>>>>>> Stashed changes
