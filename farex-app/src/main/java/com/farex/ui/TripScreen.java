package com.farex.ui;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.layout.Border;
import javafx.scene.layout.BorderStroke;
import javafx.scene.layout.BorderStrokeStyle;
import javafx.scene.layout.BorderWidths;
import javafx.scene.layout.CornerRadii;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

// Trip Planner, all in one screen (opened from the Routes tab):
//   planner (01/05) -> pick start (03) -> pick destination (04) -> planner
//   -> searching (06) -> choose your route (07) -> route details (08-10) -> route map (19)
//   -> trip in progress (15)
// Warnings and results (02, 11-14, 16-18) are pop-ups in TripDialogs. Back arrow = previous step;
// during a trip it asks "End this trip?" first.
public class TripScreen extends StepScreen {

    private boolean tracking = false;

    public TripScreen() {
        super("Trip Planner", "routes");
        restartAt(this::plannerStep);
    }

    @Override
    protected void back() {
        if (tracking) {
            TripDialogs.endTrip();
        } else {
            super.back();
        }
    }

    @Override
    protected void onExit() {
        TripState.clear();
    }

    // ----- 01 (nothing chosen) / 05 (both chosen): Point A, Point B, Search routes -----

    private void plannerStep() {
        tracking = false;
        TripPlace start = TripState.getStart();
        TripPlace destination = TripState.getDestination();

        Label title = new Label("Trip planner");
        title.setTextFill(Theme.WHITE);
        title.setFont(Theme.bold(22));
        VBox heading = new VBox(6,
                TripUi.eyebrow("PLAN YOUR JOURNEY"),
                title,
                FetchUi.subtitle("Choose where to start and where you want to go."));
        VBox.setMargin(heading, new Insets(-12, 0, 4, 0));

        VBox info = FetchUi.panel(
                TripUi.bold("Find your way, your way", 13),
                FetchUi.subtitle("Compare distance, transfers, and estimated fare. "
                        + "You can track your trip after choosing a route."));
        info.setSpacing(6);

        body.getChildren().addAll(
                heading,
                pointPanel("POINT A · FROM", (start == null) ? "Choose starting point" : start.getFullName(),
                        () -> showStep(this::pickStartStep)),
                // Point B needs Point A first ("Starting at Quiapo Church" is shown there)
                pointPanel("POINT B · TO", (destination == null) ? "Choose destination" : destination.getFullName(),
                        () -> showStep(start == null ? this::pickStartStep : this::pickDestinationStep)),
                info,
                FetchUi.small("Preview journey · sample stops and estimates", Theme.WHITE),
                FetchUi.spacer(),
                FetchUi.limeButton("Search routes", this::search),
                FetchUi.link("Cancel", this::back));
    }

    private Node pointPanel(String caption, String value, Runnable onClick) {
        VBox panel = FetchUi.panel(TripUi.eyebrow(caption), TripUi.bold(value, 14));
        panel.setSpacing(4);
        panel.setCursor(Cursor.HAND);
        panel.setOnMouseClicked(e -> onClick.run());
        return panel;
    }

    // Both chosen -> search; otherwise the Warning pop-up (02)
    private void search() {
        if (TripState.isReady()) {
            showStep(this::searchingStep);
        } else {
            TripDialogs.missingLocation(() -> showStep(
                    TripState.getStart() == null ? this::pickStartStep : this::pickDestinationStep));
        }
    }

    // ----- 03 / 04: pick a stop, cards floating over the map -----

    private void pickStartStep() {
        pickStep(true);
    }

    private void pickDestinationStep() {
        pickStep(false);
    }

    private void pickStep(boolean pickingStart) {
        TripPlace place = pickingStart ? TripState.QUIAPO : TripState.RIZAL_PARK;
        TripPlace start = TripState.getStart();

        String hint = pickingStart
                ? "Select a suggested stop to plan this preview trip."
                : "Starting at " + (start == null ? "your starting point" : start.getFullName()) + ".";
        VBox topCard = TripUi.floatingCard(
                TripUi.eyebrow(pickingStart ? "STEP 1 OF 2" : "STEP 2 OF 2"),
                TripUi.bold(pickingStart ? "Starting point" : "Destination", 22),
                FetchUi.small(hint, Theme.WHITE));

        VBox placeCard = TripUi.floatingCard(
                TripUi.eyebrow("SEARCH LOCATION"),
                TripUi.bold(place.getName(), 15),
                FetchUi.small(place.getAddress(), Theme.WHITE));

        VBox tipCard = pickingStart
                ? tip("Prefer your current location?",
                        "You can enable location when you start tracking. Planning a trip does not require GPS.")
                : tip("Stop-based route planning",
                        "Routes are matched to boarding and alighting stops. A short walk may be needed.");

        Node choose = FetchUi.limeButton((pickingStart ? "Use " : "Choose ") + place.getName(), () -> {
            if (pickingStart) {
                TripState.setStart(place);
                showStep(this::pickDestinationStep);
            } else {
                TripState.setDestination(place);
                restartAt(this::plannerStep);   // 05: both filled in
            }
        });
        Node second = TripUi.darkButton(pickingStart ? "Cancel" : "Back to starting point", this::back);

        Region push = new Region();
        VBox.setVgrow(push, Priority.ALWAYS);
        VBox overlay = new VBox(10, topCard, push, placeCard, tipCard, choose, second);
        overlay.setPadding(new Insets(10, 12, 14, 12));
        overlay.setPickOnBounds(false);

        // The map fills the whole area under the header
        StackPane area = new StackPane(new MapView(), overlay);
        VBox.setVgrow(area, Priority.ALWAYS);
        body.setPadding(Insets.EMPTY);
        body.getChildren().add(area);
    }

    // Small dark card: bold line + short text
    private VBox tip(String heading, String text) {
        Label small = FetchUi.small(text, Theme.WHITE);
        small.setWrapText(true);
        VBox card = TripUi.floatingCard(TripUi.bold(heading, 11), small);
        card.setPadding(new Insets(8, 12, 8, 12));
        return card;
    }

    // ----- 06: fake route search (N = no route, Esc = connection lost) -----

    private void searchingStep() {
        body.getChildren().add(new TripSearchStep(
                () -> replaceStep(this::routesStep),   // back from the results skips the search
                () -> TripDialogs.connectionLost(this::redraw, this::back),
                () -> TripDialogs.noRoute(this::redraw, this::back),
                this::back));
    }

    // ----- 07: the three routes -----

    private void routesStep() {
        VBox heading = new VBox(6,
                FetchUi.title("Choose your route"),
                FetchUi.subtitle(TripState.summary() + "\nExample routes · fares and times are estimates"));
        VBox.setMargin(heading, new Insets(0, 0, 6, 0));

        body.getChildren().add(heading);
        for (int i = 0; i < TripRoute.SAMPLES.size(); i++) {
            body.getChildren().add(routeCard(i));
        }
        body.getChildren().addAll(
                FetchUi.spacer(),
                FetchUi.limeButton("Edit locations", this::back),
                FetchUi.link("Back to Routes", () -> leaveTo("routes")));
    }

    // "01  Shortest distance ›" / "4.2 km · 25 min · ₱26" / "1 transfer · Walk 300 m"
    private Node routeCard(int index) {
        TripRoute route = TripRoute.SAMPLES.get(index);
        boolean selected = (index == TripState.getRouteIndex());

        Label name = new Label(TripRoute.number(index) + "  " + route.getTitle() + " ›");
        name.setTextFill(selected ? Theme.LIME : Theme.WHITE);
        name.setFont(Theme.bold(12));

        VBox text = new VBox(3, name, TripUi.bold(route.getStats(), 14),
                FetchUi.small(route.getSummary(), Theme.WHITE));
        HBox.setHgrow(text, Priority.ALWAYS);

        Label chevron = new Label("›");
        chevron.setTextFill(Theme.WHITE);
        chevron.setFont(Theme.bold(22));

        HBox card = new HBox(8, text, chevron);
        card.setAlignment(Pos.CENTER_LEFT);
        card.setPadding(new Insets(12, 16, 12, 16));
        card.setBackground(Theme.rounded(FetchUi.PANEL, 8));
        if (selected) {
            card.setBorder(new Border(new BorderStroke(Theme.LIME,
                    BorderStrokeStyle.SOLID, new CornerRadii(8), new BorderWidths(1.2))));
        }
        card.setCursor(Cursor.HAND);
        card.setOnMouseClicked(e -> {
            TripState.setRouteIndex(index);
            showStep(this::routeStep);
        });
        return card;
    }

    // ----- 08 / 09 / 10: the picked route's details -----

    private void routeStep() {
        TripRoute route = TripState.getRoute();

        VBox heading = new VBox(6, FetchUi.title(route.getTitle()), FetchUi.subtitle(TripState.summary()));
        VBox.setMargin(heading, new Insets(0, 0, 6, 0));

        // "4.2 km  ·  25 min  ·  ₱26"
        Label stats = new Label(route.getStats().replace(" · ", "  ·  "));
        stats.setTextFill(Theme.LIME);
        stats.setFont(Theme.bold(16));
        VBox total = FetchUi.panel(stats, FetchUi.small("Estimated total · standard fare", Theme.WHITE));
        total.setSpacing(4);

        // "01  Board at Quiapo Church" / "Quiapo–Kalaw jeepney · estimated ₱13"
        VBox legs = FetchUi.panel();
        legs.setSpacing(8);
        for (int i = 0; i < route.getLegs().size(); i++) {
            TripRoute.Leg leg = route.getLegs().get(i);
            legs.getChildren().add(new VBox(2,
                    TripUi.bold(TripRoute.number(i) + "  " + leg.getTitle(), 12),
                    FetchUi.small(leg.getDetail(), Theme.WHITE)));
        }

        body.getChildren().addAll(
                heading,
                total,
                legs,
                FetchUi.subtitle("Estimates only. Confirm the jeepney signboard, fare, "
                        + "and alighting stop with the driver."),
                FetchUi.spacer(),
                FetchUi.limeButton("Start trip tracking", this::askLocation),
                FetchUi.link("View route map", () -> showStep(this::routeMapStep)));
    }

    // 13 / 14 (pop-ups): explain, then the phone's permission prompt
    private void askLocation() {
        if (TripState.isLocationAllowed()) {
            startTracking();
        } else {
            TripDialogs.enableLocation(this::requestPermission);
        }
    }

    private void requestPermission() {
        LocationPermissionDialog.ask(this::startTracking,
                () -> TripDialogs.trackingUnavailable(this::requestPermission));
    }

    // ----- 19: the chosen route on the map -----

    private void routeMapStep() {
        body.getChildren().addAll(
                new VBox(6, FetchUi.title("Your route at a glance"), FetchUi.subtitle(TripState.summary())),
                TripUi.mapCard(190),
                TripUi.routeCard(),
                FetchUi.subtitle("This map provides area context only. Follow the stop-by-stop directions "
                        + "in route details and confirm boarding with the driver."),
                FetchUi.spacer(),
                FetchUi.limeButton("Back to route details", this::back),
                FetchUi.link("Choose another route", () -> {
                    back();     // route details
                    back();     // route list
                }));
    }

    // ----- 15: the (simulated) trip in progress -----

    // History is forgotten: from here the back arrow asks "End this trip?"
    private void startTracking() {
        restartAt(this::trackingStep);
    }

    private void trackingStep() {
        tracking = true;
        TripRoute route = TripState.getRoute();

        // Next step: the first leg, e.g. "Board at Quiapo Church"
        VBox nextStep = FetchUi.panel(
                TripUi.bold(route.getLegs().get(0).getTitle(), 14),
                FetchUi.subtitle("Check the signboard and confirm your destination with the driver before boarding."));
        nextStep.setSpacing(6);

        Label offRoute = FetchUi.small("Preview an off-route warning ›", Theme.LIME);
        offRoute.setCursor(Cursor.HAND);
        offRoute.setOnMouseClicked(e -> TripDialogs.offRoute(() -> restartAt(this::plannerStep)));

        body.getChildren().addAll(
                new VBox(6, FetchUi.title("Your trip is underway"),
                        FetchUi.subtitle(TripState.summary() + "\nLocation and progress are simulated.")),
                TripUi.mapCard(120),
                TripUi.routeCard(),
                nextStep,
                offRoute,
                FetchUi.spacer(),
                FetchUi.limeButton("I’ve arrived", () -> TripDialogs.arrived(() -> {
                    TripState.clear();
                    restartAt(this::plannerStep);
                })),
                FetchUi.link("End trip", TripDialogs::endTrip));
    }

    // ----- Helpers -----

    // Leave the planner for another page, forgetting the trip
    static void leaveTo(String route) {
        TripState.clear();
        Navigator.go(route);
    }
}
