package com.farex.ui.trip;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import javafx.animation.PauseTransition;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressIndicator;
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
import javafx.scene.paint.Color;
import javafx.util.Duration;

import com.farex.Api;
import com.farex.ui.common.CommonUi;
import com.farex.ui.common.LeafletMap;
import com.farex.ui.common.Theme;
import farex.shared.data.RouteStop;
import farex.shared.plan.TripProgress;

// Routes tab: the whole trip planner on one map, like a maps app.
//   Top:    From / To boxes. Typing lists matching stops; ⌖ uses your location.
//   Map:    drag and zoom freely. A tap drops a pin: "Start here" / "Go here", and the jeeps passing there.
//   Bottom: one sheet that changes with the step: finding routes -> up to 3 results (tap one to see its
//           steps on the map) -> Start trip -> live tracking. No page changes; the plan survives tab switches.
public class RoutesView extends StackPane {

    private static final int TIMEOUT_SECONDS = 20;
    // Laptop has no GPS: "My location" is this demo spot (UP Diliman), clearly labelled
    private static final double DEMO_LAT = 14.6537, DEMO_LNG = 121.0687;

    private final LeafletMap map = new LeafletMap();
    private final TextField fromField = field("From: a stop, or tap the map");
    private final TextField toField = field("To: where are you going?");
    private final VBox bar;
    private final VBox suggestions = new VBox(4);
    private final VBox suggestionsCard;
    private final VBox sheet = new VBox(8);
    private TextField editing;
    private boolean filling = false;             // text set by code, not typed
    private int asked = 0;                       // a newer request wins over a slower older answer

    public RoutesView() {
        Button locate = TripUi.darkButton("⌖", this::useMyLocation);
        locate.setMinWidth(40);
        HBox fromRow = new HBox(6, fromField, locate);
        HBox.setHgrow(fromField, Priority.ALWAYS);
        bar = TripUi.floatingCard(fromRow, toField);
        bar.setSpacing(6);
        bar.setPadding(new Insets(10, 10, 10, 10));

        suggestionsCard = TripUi.floatingCard(suggestions);
        suggestionsCard.setPadding(new Insets(6));
        hide(suggestionsCard);
        hide(sheet);
        sheet.setPadding(new Insets(12, 14, 12, 14));
        sheet.setBackground(Theme.rounded(Color.rgb(28, 28, 28, 0.97), 12));
        sheet.heightProperty().addListener((o, a, h) -> map.setBottomInset(sheet.isVisible() ? h.doubleValue() + 16 : 0));
        bar.heightProperty().addListener((o, a, h) -> map.setTopInset(bar.isVisible() ? h.doubleValue() + 12 : 0));

        Region push = new Region();
        VBox.setVgrow(push, Priority.ALWAYS);
        push.setMouseTransparent(true);              // an empty Region still catches clicks: the map must get them
        VBox overlay = new VBox(8, bar, suggestionsCard, push, sheet);
        overlay.setPadding(new Insets(10, 12, 12, 12));
        overlay.setPickOnBounds(false);              // drags and taps between the cards reach the map

        wire(fromField, true);
        wire(toField, false);
        map.withFarexRoutes();
        map.onPick(this::tapped);
        getChildren().addAll(map, overlay);

        TripState.loadPlaces(places -> { });         // warm the stop list for typing
        restore();
    }

    // Coming back to the tab: the trip in progress, the results, or the points chosen so far
    private void restore() {
        fill(fromField, name(TripState.getStart()));
        fill(toField, name(TripState.getDestination()));
        TripTracker tracker = TripTracker.active();
        if (tracker != null) {
            tracking(tracker);
        } else if (!TripState.getRoutes().isEmpty()) {
            results();
        }
    }

    // ----- From / To -----

    private static TextField field(String prompt) {
        TextField f = new TextField();
        f.setPromptText(prompt);
        f.setStyle("-fx-background-color: #2A2A2A; -fx-text-fill: white; -fx-prompt-text-fill: #9A9A9A;"
                + " -fx-background-radius: 6; -fx-padding: 9 10 9 10; -fx-font-size: 13px;");
        return f;
    }

    // Typing clears that end and lists matching stops; leaving the box hides the list
    private void wire(TextField f, boolean isStart) {
        f.textProperty().addListener((o, a, text) -> {
            if (filling || !f.isFocused()) return;
            setPlace(isStart, null);
            editing = f;
            showSuggestions(text, isStart);
        });
        f.focusedProperty().addListener((o, a, focused) -> {
            if (!focused) {
                PauseTransition later = new PauseTransition(Duration.millis(150));   // let a click on a row land first
                later.setOnFinished(e -> { if (editing == f && !f.isFocused()) hide(suggestionsCard); });
                later.play();
            }
        });
    }

    private void showSuggestions(String text, boolean isStart) {
        suggestions.getChildren().clear();
        if (text.isBlank()) {
            hide(suggestionsCard);
            return;
        }
        if (!TripState.isLive()) {
            suggestions.getChildren().add(TripUi.wrapped("Can’t reach FareX, so stops can’t be searched. Check your connection."));
        } else {
            List<TripPlace> found = TripState.search(text, 6);
            if (found.isEmpty()) suggestions.getChildren().add(TripUi.wrapped("No stop matches. Try another word, or tap the map."));
            for (TripPlace p : found) {
                VBox row = new VBox(1, TripUi.bold(p.getName(), 13), CommonUi.small(p.getAddress(), Theme.GRAY));
                row.setPadding(new Insets(6, 8, 6, 8));
                row.setBackground(Theme.rounded(CommonUi.PANEL, 6));
                row.setCursor(Cursor.HAND);
                row.setOnMouseClicked(e -> choose(isStart, p));
                suggestions.getChildren().add(row);
            }
        }
        show(suggestionsCard);
    }

    // A stop, a map pin or your location becomes one end; with both ends, routes are found at once
    private void choose(boolean isStart, TripPlace place) {
        setPlace(isStart, place);
        fill(isStart ? fromField : toField, name(place));
        hide(suggestionsCard);
        map.showPin(null);
        requestFocus();                                  // out of the text box
        if (TripState.getStart() != null && TripState.getDestination() != null) {
            findRoutes();
        } else {
            hide(sheet);
            if (isStart) toField.requestFocus();
        }
    }

    private void setPlace(boolean isStart, TripPlace place) {
        if (TripTracker.active() != null) return;
        if (isStart) TripState.setStart(place); else TripState.setDestination(place);
        TripState.setRoutes(List.of());                  // old results no longer match
        map.showTrip(List.of());
    }

    private void fill(TextField f, String text) {
        filling = true;
        f.setText(text);
        filling = false;
    }

    private static String name(TripPlace p) {
        return p == null ? "" : p.getName();
    }

    // ⌖: the phone's GPS (asks permission first), or the demo spot on a laptop
    private void useMyLocation() {
        java.util.Optional<LocationSource> gps = LocationSource.phone();
        if (gps.isEmpty()) {
            choose(true, TripPlace.pinned("My location (demo)", DEMO_LAT, DEMO_LNG));
            return;
        }
        Runnable locate = () -> {
            message("Finding your location…", true);
            LocationSource source = gps.get();
            boolean[] got = {false};
            source.start((lat, lng) -> {
                if (got[0]) return;
                got[0] = true;
                source.stop();
                choose(true, TripPlace.pinned("My location", lat, lng));
            });
            PauseTransition giveUp = new PauseTransition(Duration.seconds(15));
            giveUp.setOnFinished(e -> {
                if (got[0]) return;
                source.stop();
                message("Couldn’t get a GPS fix. Type your stop or tap the map instead.", false);
            });
            giveUp.play();
        };
        if (TripState.isLocationAllowed()) locate.run();
        else LocationPermissionDialog.ask(locate, () -> message("Location is off. Type your stop or tap the map instead.", false));
    }

    // ----- Map tap: a pin, Start here / Go here, and the jeeps passing there -----

    private void tapped(double lat, double lng) {
        if (TripTracker.active() != null) return;
        int ask = ++asked;
        map.focusRoute(null);
        map.showPin(new RouteStop("Here", lat, lng));
        HBox actions = new HBox(8,
                CommonUi.limeButton("Start here", () -> choose(true, TripPlace.pinned("Pinned start", lat, lng))),
                TripUi.darkButton("Go here", () -> choose(false, TripPlace.pinned("Pinned destination", lat, lng))));
        actions.getChildren().forEach(b -> HBox.setHgrow(b, Priority.ALWAYS));
        actions.getChildren().forEach(b -> ((Region) b).setMaxWidth(Double.MAX_VALUE));
        VBox passing = new VBox(6, CommonUi.small("Looking for jeeps…", Theme.WHITE));
        fillSheet("DROPPED PIN", this::closeTap, actions, TripUi.eyebrow("JEEPS PASSING HERE"), passing);

        Thread t = new Thread(() -> {
            List<Api.Passing> found;
            try {
                found = Api.passing(lat, lng);
            } catch (RuntimeException offline) {
                Platform.runLater(() -> {
                    if (ask == asked) passing.getChildren().setAll(TripUi.wrapped("Can’t reach FareX right now. Check your connection."));
                });
                return;
            }
            Platform.runLater(() -> {
                if (ask != asked) return;
                if (found.isEmpty()) {
                    passing.getChildren().setAll(TripUi.wrapped("No jeepney route passes within 150 m of here. Try a main road."));
                    return;
                }
                VBox rows = new VBox(6);
                for (Api.Passing p : found) rows.getChildren().add(passingRow(p));
                passing.getChildren().setAll(scroll(rows, 170));
            });
        }, "farex-passing");
        t.setDaemon(true);
        t.start();
    }

    private void closeTap() {
        asked++;
        map.showPin(null);
        map.focusRoute(null);
        if (!TripState.getRoutes().isEmpty()) results(); else hide(sheet);
    }

    // "2120-B  Rodriguez Suburban–SM North EDSA" / "44 m away · Pinatubo St → North Ave near EDSA"; tap = draw it
    private Node passingRow(Api.Passing p) {
        Label id = CommonUi.smallBold(p.routeId(), Theme.LIME);
        id.setMinWidth(Region.USE_PREF_SIZE);
        Label name = TripUi.wrapped(p.name());
        name.setMinHeight(Region.USE_PREF_SIZE);
        HBox top = new HBox(8, id, name);
        HBox.setHgrow(name, Priority.ALWAYS);
        String ends = p.oneway() ? p.firstStop() + " → " + p.lastStop() : p.firstStop() + " ↔ " + p.lastStop();
        Label where = TripUi.wrapped(p.meters() + " m away · " + ends);
        where.setMinHeight(Region.USE_PREF_SIZE);
        where.setTextFill(Color.web("#BDBDBD"));
        VBox row = new VBox(2, top, where);
        row.setPadding(new Insets(6, 8, 6, 8));
        row.setBackground(Theme.rounded(Color.web("#262626"), 6));
        row.setCursor(Cursor.HAND);
        row.setOnMouseClicked(e -> map.focusRoute(p.routeId()));
        return row;
    }

    // ----- Finding routes: an inline spinner, then the results -----

    private void findRoutes() {
        TripPlace from = TripState.getStart(), to = TripState.getDestination();
        if (!TripState.isReady()) {
            message("Start and destination are the same place. Pick another destination.", false);
            return;
        }
        int ask = ++asked;
        ProgressIndicator spin = new ProgressIndicator();
        spin.setMaxSize(28, 28);
        HBox finding = new HBox(10, spin, TripUi.bold("Finding routes…", 14));
        finding.setAlignment(Pos.CENTER_LEFT);
        fillSheet(null, null, finding, CommonUi.small(TripState.summary(), Theme.GRAY),
                CommonUi.link("Cancel", () -> { asked++; hide(sheet); }));

        // A pin or your location at either end: plan between the two points (walks at the ends)
        CompletableFuture.supplyAsync(() -> from.isPin() || to.isPin()
                        ? Api.plan(from.getLat(), from.getLng(), from.getName(), to.getLat(), to.getLng(), to.getName())
                        : Api.plan(from.getName(), to.getName()))
                .orTimeout(TIMEOUT_SECONDS, TimeUnit.SECONDS)
                .whenComplete((plans, error) -> Platform.runLater(() -> {
                    if (ask != asked) return;                 // cancelled, or a newer search
                    if (error != null) {
                        fillSheet(null, () -> hide(sheet), TripUi.bold("Can’t reach FareX", 14),
                                TripUi.wrapped("Check your connection, then try again."),
                                CommonUi.limeButton("Try again", this::findRoutes));
                    } else if (plans.isEmpty()) {
                        fillSheet(null, () -> hide(sheet), TripUi.bold("No jeepney route found", 14),
                                TripUi.wrapped("No route connects these two places within a short walk. "
                                        + "Try a point closer to a main road."));
                    } else {
                        List<TripRoute> found = new ArrayList<>();
                        for (Api.PlanCard p : plans) found.add(TripRoute.fromPlan(p));
                        TripState.setRoutes(found);
                        results();
                    }
                }));
    }

    // Up to three cards; the picked one is drawn on the map and shows its steps and "Start trip"
    private void results() {
        TripRoute picked = TripState.getRoute();
        map.showPin(null);
        map.withFarexTrip(picked.getRides(), TripUi.pinnedPoints());
        List<Node> cards = new ArrayList<>();
        for (int i = 0; i < TripState.getRoutes().size(); i++) cards.add(resultCard(i));
        VBox list = new VBox(8);
        list.getChildren().addAll(cards);
        fillSheet(TripState.summary(), this::clearPlan, scroll(list, 330),
                CommonUi.small("Fares and times are estimates. Confirm the signboard with the driver.", Theme.GRAY));
    }

    private Node resultCard(int index) {
        TripRoute route = TripState.getRoutes().get(index);
        boolean picked = index == TripState.getRouteIndex();
        Label title = CommonUi.smallBold(route.getTitle(), picked ? Theme.LIME : Theme.WHITE);
        VBox card = new VBox(3, title, TripUi.bold(route.getStats(), 14), CommonUi.small(route.getSummary(), Theme.GRAY));
        card.setPadding(new Insets(10, 12, 10, 12));
        card.setBackground(Theme.rounded(CommonUi.PANEL, 8));
        if (picked) {
            card.setBorder(new Border(new BorderStroke(Theme.LIME, BorderStrokeStyle.SOLID, new CornerRadii(8), new BorderWidths(1.2))));
            VBox steps = new VBox(6);
            for (int i = 0; i < route.getLegs().size(); i++) {
                TripRoute.Leg leg = route.getLegs().get(i);
                steps.getChildren().add(new VBox(1, TripUi.bold(TripRoute.number(i) + "  " + leg.getTitle(), 12),
                        TripUi.wrapped(leg.getDetail())));
            }
            VBox.setMargin(steps, new Insets(6, 0, 4, 0));
            card.getChildren().addAll(steps, CommonUi.limeButton("Start trip", this::askLocation));
        } else {
            card.setCursor(Cursor.HAND);
            card.setOnMouseClicked(e -> {
                TripState.setRouteIndex(index);
                results();
            });
        }
        return card;
    }

    private void clearPlan() {
        asked++;
        TripState.clear();
        fill(fromField, "");
        fill(toField, "");
        map.showTrip(List.of());
        hide(sheet);
    }

    // ----- Live tracking, in the same sheet -----

    private void askLocation() {
        if (TripState.isLocationAllowed()) startTrip();
        else TripDialogs.enableLocation(() -> LocationPermissionDialog.ask(this::startTrip,
                () -> TripDialogs.trackingUnavailable(this::askLocation)));
    }

    private void startTrip() {
        message("Getting your route ready…", true);
        TripTracker.prepare(TripState.getRoute().getRides(), TripUi.pinnedPoints(), tracker -> {
            tracker.start((at, s) -> { });
            tracking(tracker);
        }, problem -> message(problem, false));
    }

    // Progress bar, "NEXT" step and minutes left follow each position fix; the search bar hides meanwhile
    private void tracking(TripTracker tracker) {
        hide(bar);
        map.setTopInset(0);
        hide(suggestionsCard);
        map.withFarexTrip(TripState.getRoute().getRides(), TripUi.pinnedPoints());

        Region fill = new Region();
        fill.setBackground(Theme.rounded(Theme.LIME, 3));
        fill.setMaxWidth(0);
        StackPane track = new StackPane(fill);
        track.setAlignment(Pos.CENTER_LEFT);
        track.setMinHeight(6);
        track.setMaxHeight(6);
        track.setBackground(Theme.rounded(Color.web("#3A3A3A"), 3));
        Label done = TripUi.bold("Starting…", 14);
        Label left = CommonUi.small("", Theme.WHITE);
        Label nextTitle = TripUi.bold("", 14);
        Label nextDetail = TripUi.wrapped("");
        Label riding = TripUi.wrapped("");
        riding.setTextFill(Color.web("#BDBDBD"));
        Label offRoute = CommonUi.smallBold("", Theme.LIME);
        offRoute.setWrapText(true);
        Label note = CommonUi.small(tracker.simulated()
                ? "No GPS on this device: a simulated ride at " + LocationSource.SimulatedRide.SPEEDUP + "× speed."
                : "Live location from your phone’s GPS. It stops when the trip ends.", Theme.GRAY);
        note.setWrapText(true);
        Label detour = CommonUi.small("Simulate a detour ›", Theme.LIME);
        detour.setCursor(Cursor.HAND);
        detour.setOnMouseClicked(e -> tracker.simulateDetour());
        if (!tracker.simulated()) hide(detour);

        HBox buttons = new HBox(8, CommonUi.limeButton("I’ve arrived", this::finishTrip),
                TripUi.darkButton("End trip", () -> TripDialogs.endTrip(this::endTrip)));
        buttons.getChildren().forEach(b -> { HBox.setHgrow(b, Priority.ALWAYS); ((Region) b).setMaxWidth(Double.MAX_VALUE); });
        fillSheet(TripState.summary(), null, done, track, left, TripUi.eyebrow("NEXT"), nextTitle, nextDetail,
                riding, offRoute, note, detour, buttons);

        boolean[] warned = {false};
        tracker.listen((at, s) -> {
            map.showRiding(s.arrived() ? null : tracker.jeepAt(s));
            map.showMe(at, s.offRoute());
            String jeep = s.arrived() ? null : tracker.jeepAt(s);
            String headed = jeep == null ? null : tracker.headedTo(jeep);
            riding.setText(jeep == null || headed == null ? ""
                    : (tracker.progress().legAt(s.doneKm()).walk() ? "Your jeep, " : "You’re on ") + jeep + ", headed to " + headed + ".");
            done.setText(String.format("%.1f of %.1f km", s.doneKm(), s.totalKm()));
            fill.setMaxWidth(track.getWidth() * s.fraction());
            fill.setPrefWidth(track.getWidth() * s.fraction());
            left.setText(s.arrived() ? "You’re there" : "About " + s.minutesLeft() + " min left · estimate");
            nextTitle.setText(checkpointTitle(tracker, s.next()));
            nextDetail.setText(checkpointDetail(s, tracker.progress().legAt(s.doneKm()).walk()));
            offRoute.setText(s.offRoute() ? String.format("You’re about %,d m from the route.", Math.round(s.offRouteMeters())) : "");
            if (s.offRoute() && !warned[0]) {                   // once per detour, not on every fix
                warned[0] = true;
                TripDialogs.offRoute(this::endTrip);
            } else if (!s.offRoute()) {
                warned[0] = false;
            }
            if (s.arrived()) finishTrip();
        });
    }

    private void finishTrip() {
        TripTracker.stopCurrent();
        TripDialogs.arrived(this::endTrip);
    }

    // Tracking off, back to the bar with the same From/To, ready to plan again
    private void endTrip() {
        TripTracker.stopCurrent();
        map.showMe(null, false);
        map.showRiding(null);
        show(bar);
        map.setTopInset(bar.getHeight() + 12);
        clearPlan();
    }

    // "Walk to Philcoa" / "Board 102-B at Philcoa" / "Transfer to 111-B at …" / "Get off at …"
    private static String checkpointTitle(TripTracker tracker, TripProgress.Checkpoint c) {
        return switch (c.kind()) {
            case WALK -> "Walk to " + c.stop();
            case BOARD -> "Board " + c.route() + " at " + c.stop();
            case TRANSFER -> "Transfer to " + c.route() + " at " + c.stop();
            case ALIGHT -> {
                List<TripProgress.Ride> legs = tracker.progress().legs();
                yield (legs.get(legs.size() - 1).walk() ? "Arrive at " : "Get off at ") + c.stop();
            }
        };
    }

    // walkingNow: on a walk leg (the last one: arriving on foot, nobody to say "para" to)
    private static String checkpointDetail(TripProgress.Status s, boolean walkingNow) {
        TripProgress.Checkpoint c = s.next();
        String in = s.toNextKm() < 0.05 ? "Now" : String.format("In %.1f km", s.toNextKm());
        return switch (c.kind()) {
            case BOARD, TRANSFER -> in + " · " + TripState.routeName(c.route())
                    + (TripRoute.flagged(c.stop()) ? ". No stop here: wait by the road and wave the jeep down."
                                                   : ". Check the signboard before you get on.");
            case WALK -> in + " · on foot";
            case ALIGHT -> in + (walkingNow ? " · on foot" : " · tell the driver “para” before your stop");
        };
    }

    // ----- Sheet helpers -----

    // title (or null) with a close × (when onClose is set), then the content
    private void fillSheet(String title, Runnable onClose, Node... content) {
        sheet.getChildren().clear();
        if (title != null || onClose != null) {
            Label head = TripUi.eyebrow(title == null ? "" : title);
            head.setWrapText(true);
            Region gap = new Region();
            HBox.setHgrow(gap, Priority.ALWAYS);
            HBox row = new HBox(head, gap);
            row.setAlignment(Pos.CENTER_LEFT);
            if (onClose != null) {
                Label close = new Label("×");
                close.setTextFill(Theme.WHITE);
                close.setFont(Theme.bold(18));
                close.setCursor(Cursor.HAND);
                close.setOnMouseClicked(e -> onClose.run());
                row.getChildren().add(close);
            }
            sheet.getChildren().add(row);
        }
        sheet.getChildren().addAll(content);
        show(sheet);
    }

    private void message(String text, boolean busy) {
        Label label = TripUi.wrapped(text);
        if (busy) {
            ProgressIndicator spin = new ProgressIndicator();
            spin.setMaxSize(24, 24);
            HBox row = new HBox(10, spin, label);
            row.setAlignment(Pos.CENTER_LEFT);
            fillSheet(null, null, row);
        } else {
            fillSheet(null, () -> hide(sheet), label);
        }
    }

    private static ScrollPane scroll(Node content, double maxHeight) {
        ScrollPane scroll = new ScrollPane(content);
        scroll.setFitToWidth(true);
        scroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scroll.setMaxHeight(maxHeight);
        scroll.setStyle("-fx-background: transparent; -fx-background-color: transparent;");
        return scroll;
    }

    private static void show(Node n) {
        n.setVisible(true);
        n.setManaged(true);
    }

    private static void hide(Node n) {
        n.setVisible(false);
        n.setManaged(false);
    }
}
