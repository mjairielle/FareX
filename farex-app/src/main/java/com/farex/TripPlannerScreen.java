package com.farex;

import javafx.scene.control.*;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

public class TripPlannerScreen extends Screen {
    private final VBox results = new VBox(8);

    public TripPlannerScreen() {
        ComboBox<String> from = combo();
        ComboBox<String> to = combo();
        from.getItems().addAll(Api.places());
        to.getItems().addAll(Api.places());

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
                error, scroll);
    }

    private void show(java.util.List<Api.PlanCard> plans) {
        results.getChildren().clear();
        if (plans.isEmpty()) results.getChildren().add(text("No route found between these places."));
        for (Api.PlanCard p : plans) {
            VBox card = new VBox(4, lime(p.label(), 14),
                    text(peso(p.fareCents()) + "  |  " + p.km() + " km  |  " + p.transfers() + " transfers  |  about " + p.minutes() + " min"));
            p.legs().forEach(l -> card.getChildren().add(text("Ride " + l)));
            card.setStyle("-fx-background-color: #1c1c1c; -fx-padding: 12;");
            results.getChildren().add(card);
        }
    }
}
