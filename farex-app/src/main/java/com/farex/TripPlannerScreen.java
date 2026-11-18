package com.farex;

import com.farex.ui.common.LeafletMap;
import farex.shared.data.RouteStop;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import javafx.scene.Cursor;
import javafx.scene.control.*;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

public class TripPlannerScreen extends Screen {
    private static final String CARD = "-fx-background-color: #1c1c1c; -fx-padding: 12;";
    private static final String CARD_PICKED = CARD + " -fx-border-color: #CAFE06; -fx-border-width: 1.2;";

    private final VBox results = new VBox(8);
    private final LeafletMap map = new LeafletMap(0, 230, 8);   // OpenStreetMap; picture when offline
    private Map<String, List<RouteStop>> routeStops = new LinkedHashMap<>();

    public TripPlannerScreen() {
        ComboBox<String> from = combo();
        ComboBox<String> to = combo();
        from.getItems().addAll(Api.places());
        to.getItems().addAll(Api.places());

        map.setMaxWidth(Double.MAX_VALUE);
        map.setMinWidth(0);
        try {
            routeStops = Api.routeStops();
            map.showRoutes(routeStops, Api.routeShapes());   // lines follow the roads
        } catch (Exception e) {
            // no route list: the map still shows the streets
        }

        ScrollPane scroll = new ScrollPane(results);
        scroll.setFitToWidth(true);
        scroll.setStyle("-fx-background: #111111; -fx-background-color: #111111;");
        setVgrow(scroll, Priority.ALWAYS);

        getChildren().addAll(back(() -> Nav.show(new PassengerHome())), heading("Trip Planner"),
                text("From"), from, text("To"), to,
                primary("Find routes", () -> {
                    if (from.getValue() == null || to.getValue() == null) throw new IllegalStateException("Pick both places");
                    show(Api.plan(from.getValue(), to.getValue()));
                }),
                error, map, scroll);
    }

    private void show(List<Api.PlanCard> plans) {
        results.getChildren().clear();
        if (plans.isEmpty()) {
            results.getChildren().add(text("No route found between these places."));
            map.showTrip(List.of());
            return;
        }
        List<VBox> cards = new ArrayList<>();
        for (Api.PlanCard p : plans) {
            VBox card = new VBox(4, lime(p.label(), 14),
                    text(peso(p.fareCents()) + "  |  " + p.km() + " km  |  " + p.transfers() + " transfers  |  about " + p.minutes() + " min"));
            p.legs().forEach(l -> card.getChildren().add(text("Ride " + l)));
            card.setStyle(CARD);
            card.setCursor(Cursor.HAND);
            card.setOnMouseClicked(e -> pick(p, card, cards));   // tap a plan to see it on the map
            cards.add(card);
            results.getChildren().add(card);
        }
        pick(plans.get(0), cards.get(0), cards);
    }

    private void pick(Api.PlanCard plan, VBox card, List<VBox> cards) {
        cards.forEach(c -> c.setStyle(CARD));
        card.setStyle(CARD_PICKED);
        map.showTrip(LeafletMap.legsFor(routeStops, plan.rides()));
    }
}
