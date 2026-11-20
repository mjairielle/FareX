package com.farex.ui.trip;

import java.util.ArrayList;
import java.util.List;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
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

import com.farex.ui.common.CommonUi;
import com.farex.ui.common.LeafletMap;
import com.farex.ui.common.Navigator;
import com.farex.ui.common.StepScreen;
import com.farex.ui.common.Theme;
import farex.shared.data.RouteStop;

// Trip Planner, all in one screen (opened from the Routes tab). Stops and route plans come from
// the FareX server; offline it falls back to the Figma preview (see TripState).
//   planner (01/05) -> pick start (03) -> pick destination (04) -> planner
//   -> searching (06) -> choose your route (07) -> route details (08-10) -> route map (19)
//   -> trip in progress (15)
// Warnings and results (02, 11-14, 16-18) are pop-ups in TripDialogs. Back arrow = previous step;
// during a trip it asks "End this trip?" first.
public class TripScreen extends StepScreen {

    private boolean tracking = false;

    public TripScreen() {
        super("Trip Planner", "routes");
        TripState.loadPlaces(places -> { });   // fetch the stops early; the pick step uses them
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
                CommonUi.subtitle("Choose where to start and where you want to go."));
        VBox.setMargin(heading, new Insets(-12, 0, 4, 0));

        VBox info = CommonUi.panel(
                TripUi.bold("Find your way, your way", 13),
                CommonUi.subtitle("Compare distance, transfers, and estimated fare. "
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
                CommonUi.small(TripState.isLive()
                        ? "FareX jeepney stops · fares are estimates"
                        : "Preview journey · sample stops and estimates", Theme.WHITE),
                CommonUi.spacer(),
                CommonUi.limeButton("Search routes", this::search),
                CommonUi.link("Cancel", this::back));
    }

    private Node pointPanel(String caption, String value, Runnable onClick) {
        VBox panel = CommonUi.panel(TripUi.eyebrow(caption), TripUi.bold(value, 14));
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
        TripPlace start = TripState.getStart();

        String hint = pickingStart
                ? "Search a stop or street, or tap the map where you are."
                : "Starting at " + (start == null ? "your starting point" : start.getFullName()) + ".";
        VBox topCard = TripUi.floatingCard(
                TripUi.eyebrow(pickingStart ? "STEP 1 OF 2" : "STEP 2 OF 2"),
                TripUi.bold(pickingStart ? "Starting point" : "Destination", 22),
                CommonUi.small(hint, Theme.WHITE));

        // "SEARCH LOCATION" card: a search box over the stops found, filled in once they are loaded
        TextField search = new TextField();
        search.setPromptText("Search a stop or street, e.g. Commonwealth");
        search.setStyle("-fx-background-color: #2A2A2A; -fx-text-fill: white; -fx-prompt-text-fill: #9A9A9A;"
                + " -fx-background-radius: 6; -fx-padding: 8 10 8 10;");
        VBox choices = new VBox(6, CommonUi.small("Loading stops…", Theme.WHITE));
        ScrollPane scroll = new ScrollPane(choices);
        scroll.setFitToWidth(true);
        scroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scroll.setStyle("-fx-background: transparent; -fx-background-color: transparent;");
        scroll.setMaxHeight(150);
        VBox placeCard = TripUi.floatingCard(TripUi.eyebrow("SEARCH LOCATION"), search, scroll);
        placeCard.setSpacing(8);

        VBox tipCard = pickingStart
                ? tip("Not at a stop?",
                        "Tap the map to drop a pin. The trip starts with a short walk to the nearest jeepney.")
                : tip("Stop-based route planning",
                        "Search a stop or tap the map. A short walk may be needed at either end or between jeepneys.");

        Button choose = CommonUi.limeButton(pickingStart ? "Use this stop" : "Choose this stop", () -> { });
        choose.setDisable(true);
        Node second = TripUi.darkButton(pickingStart ? "Cancel" : "Back to starting point", this::back);

        // The map fills the whole area under the header, with the FareX routes and their stops
        LeafletMap map = new LeafletMap().withFarexRoutes();
        Picker picker = new Picker(choices, choose, map, pickingStart);

        // Cached stops arrive at once (before this step is on screen); loading ones arrive later.
        // If the step was left meanwhile, filling its detached list is harmless.
        TripState.loadPlaces(places -> {
            if (!TripState.isLive()) {
                search.setVisible(false);
                search.setManaged(false);
                picker.show(places);                  // offline preview: the two sample stops
                return;
            }
            picker.show(List.of());
            search.textProperty().addListener((o, a, text) -> picker.show(text.isBlank() ? List.of() : TripState.search(text, 30)));
            map.onPick((lat, lng) -> picker.select(TripPlace.pinned(pickingStart ? "Pinned start" : "Pinned destination", lat, lng)));
        });

        Region push = new Region();
        VBox.setVgrow(push, Priority.ALWAYS);
        VBox overlay = new VBox(10, topCard, push, placeCard, tipCard, choose, second);
        overlay.setPadding(new Insets(10, 12, 14, 12));
        overlay.setPickOnBounds(false);

        StackPane area = new StackPane(map, overlay);
        VBox.setVgrow(area, Priority.ALWAYS);
        body.setPadding(Insets.EMPTY);
        body.getChildren().add(area);
    }

    // The stop list and the lime button of one pick step: tap a row (or the map) to choose it
    private final class Picker {
        private final VBox choices;
        private final Button choose;
        private final LeafletMap map;
        private final boolean pickingStart;
        private final List<VBox> rows = new ArrayList<>();

        Picker(VBox choices, Button choose, LeafletMap map, boolean pickingStart) {
            this.choices = choices;
            this.choose = choose;
            this.map = map;
            this.pickingStart = pickingStart;
        }

        // One tappable row per stop; the picked one is outlined lime and named on the button
        void show(List<TripPlace> places) {
            choices.getChildren().clear();
            rows.clear();
            TripPlace start = TripState.getStart();
            for (TripPlace place : places) {
                if (!pickingStart && start != null && place.getName().equals(start.getName())) {
                    continue;   // cannot travel to where you start
                }
                VBox row = new VBox(2, TripUi.bold(place.getName(), 14), CommonUi.small(place.getAddress(), Theme.GRAY));
                row.setPadding(new Insets(6, 10, 6, 10));
                row.setBackground(Theme.rounded(CommonUi.PANEL, 6));
                row.setCursor(Cursor.HAND);
                row.setOnMouseClicked(e -> {
                    rows.forEach(r -> r.setBorder(null));
                    row.setBorder(new Border(new BorderStroke(Theme.LIME,
                            BorderStrokeStyle.SOLID, new CornerRadii(6), new BorderWidths(1.2))));
                    select(place);
                });
                rows.add(row);
                choices.getChildren().add(row);
            }
            String note = !TripState.isLive()
                    ? "Offline preview · sample stops. Connect to the FareX server for real routes."
                    : places.isEmpty() ? "Type to search " + "the FareX stops, or tap the map to drop a pin." : null;
            if (note != null) {
                Label label = CommonUi.small(note, TripState.isLive() ? Theme.GRAY : Theme.LIME);
                label.setWrapText(true);
                choices.getChildren().add(label);
            }
        }

        // Make `place` the choice: name it on the button, and pin it on the map
        void select(TripPlace place) {
            if (place.isPin()) rows.forEach(r -> r.setBorder(null));
            if (place.isPin() || !Double.isNaN(place.getLat())) {
                map.showPin(new RouteStop(place.getName(), place.getLat(), place.getLng()));
            }
            choose.setText((pickingStart ? "Use " : "Choose ") + place.getName());
            choose.setDisable(false);
            choose.setOnAction(a -> {
                if (pickingStart) {
                    TripState.setStart(place);
                    showStep(TripScreen.this::pickDestinationStep);
                } else {
                    TripState.setDestination(place);
                    restartAt(TripScreen.this::plannerStep);   // 05: both filled in
                }
            });
        }
    }

    // Small dark card: bold line + short text
    private VBox tip(String heading, String text) {
        Label small = CommonUi.small(text, Theme.WHITE);
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
                CommonUi.title("Choose your route"),
                CommonUi.subtitle(TripState.summary() + (TripState.isLive()
                        ? "\nStandard fares · times are estimates"
                        : "\nExample routes · fares and times are estimates")));
        VBox.setMargin(heading, new Insets(0, 0, 6, 0));

        body.getChildren().add(heading);
        for (int i = 0; i < TripState.getRoutes().size(); i++) {
            body.getChildren().add(routeCard(i));
        }
        body.getChildren().addAll(
                CommonUi.spacer(),
                CommonUi.limeButton("Edit locations", this::back),
                CommonUi.link("Back to Routes", () -> leaveTo("routes")));
    }

    // "01  Shortest distance ›" / "4.2 km · 25 min · ₱26" / "1 transfer · Walk 300 m"
    private Node routeCard(int index) {
        TripRoute route = TripState.getRoutes().get(index);
        boolean selected = (index == TripState.getRouteIndex());

        Label name = new Label(TripRoute.number(index) + "  " + route.getTitle() + " ›");
        name.setTextFill(selected ? Theme.LIME : Theme.WHITE);
        name.setFont(Theme.bold(12));

        VBox text = new VBox(3, name, TripUi.bold(route.getStats(), 14),
                CommonUi.small(route.getSummary(), Theme.WHITE));
        HBox.setHgrow(text, Priority.ALWAYS);

        Label chevron = new Label("›");
        chevron.setTextFill(Theme.WHITE);
        chevron.setFont(Theme.bold(22));

        HBox card = new HBox(8, text, chevron);
        card.setAlignment(Pos.CENTER_LEFT);
        card.setPadding(new Insets(12, 16, 12, 16));
        card.setBackground(Theme.rounded(CommonUi.PANEL, 8));
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

        VBox heading = new VBox(6, CommonUi.title(route.getTitle()), CommonUi.subtitle(TripState.summary()));
        VBox.setMargin(heading, new Insets(0, 0, 6, 0));

        // "4.2 km  ·  25 min  ·  ₱26"
        Label stats = new Label(route.getStats().replace(" · ", "  ·  "));
        stats.setTextFill(Theme.LIME);
        stats.setFont(Theme.bold(16));
        VBox total = CommonUi.panel(stats, CommonUi.small("Estimated total · standard fare", Theme.WHITE));
        total.setSpacing(4);

        // "01  Board at Quiapo Church" / "Quiapo–Kalaw jeepney · estimated ₱13"
        VBox legs = CommonUi.panel();
        legs.setSpacing(8);
        for (int i = 0; i < route.getLegs().size(); i++) {
            TripRoute.Leg leg = route.getLegs().get(i);
            legs.getChildren().add(new VBox(2,
                    TripUi.bold(TripRoute.number(i) + "  " + leg.getTitle(), 12),
                    TripUi.wrapped(leg.getDetail())));
        }

        body.getChildren().addAll(
                heading,
                total,
                legs,
                CommonUi.subtitle("Estimates only. Confirm the jeepney signboard, fare, "
                        + "and alighting stop with the driver."),
                CommonUi.spacer(),
                CommonUi.limeButton("Start trip tracking", this::askLocation),
                CommonUi.link("View route map", () -> showStep(this::routeMapStep)));
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
                new VBox(6, CommonUi.title("Your route at a glance"), CommonUi.subtitle(TripState.summary())),
                TripUi.mapCard(190),
                TripUi.routeCard(),
                CommonUi.subtitle(TripState.getRoute().getRides().isEmpty()
                        ? "This map provides area context only. Follow the stop-by-stop directions "
                                + "in route details and confirm boarding with the driver."
                        : "Your route is the lime line. Follow the stop-by-stop directions "
                                + "in route details and confirm boarding with the driver."),
                CommonUi.spacer(),
                CommonUi.limeButton("Back to route details", this::back),
                CommonUi.link("Choose another route", () -> {
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
        VBox nextStep = CommonUi.panel(
                TripUi.bold(route.getLegs().get(0).getTitle(), 14),
                CommonUi.subtitle("Check the signboard and confirm your destination with the driver before boarding."));
        nextStep.setSpacing(6);

        Label offRoute = CommonUi.small("Preview an off-route warning ›", Theme.LIME);
        offRoute.setCursor(Cursor.HAND);
        offRoute.setOnMouseClicked(e -> TripDialogs.offRoute(() -> restartAt(this::plannerStep)));

        body.getChildren().addAll(
                new VBox(6, CommonUi.title("Your trip is underway"),
                        CommonUi.subtitle(TripState.summary() + "\nLocation and progress are simulated.")),
                TripUi.mapCard(120),
                TripUi.routeCard(),
                nextStep,
                offRoute,
                CommonUi.spacer(),
                CommonUi.limeButton("I’ve arrived", () -> TripDialogs.arrived(() -> {
                    TripState.clear();
                    restartAt(this::plannerStep);
                })),
                CommonUi.link("End trip", TripDialogs::endTrip));
    }

    // ----- Helpers -----

    // Leave the planner for another page, forgetting the trip
    static void leaveTo(String route) {
        TripState.clear();
        Navigator.go(route);
    }
}
