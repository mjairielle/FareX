package com.farex.ui.trip;

import java.util.List;

import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;

import com.farex.Api;
import com.farex.ui.common.CommonUi;
import com.farex.ui.common.Icons;
import com.farex.ui.common.LeafletMap;
import com.farex.ui.common.Navigator;
import com.farex.ui.common.Theme;
import farex.shared.data.RouteStop;

// Routes tab (inside MainShell): the map with VIEW MAP and the Trip Planner tile on top.
// Tap anywhere on the map: "Jeeps passing here" lists every route whose road runs right there,
// where each one goes, and tapping one shows that route on the map.
public class RoutesView extends StackPane {

    private final LeafletMap map = new LeafletMap();
    private final VBox passingCard = new VBox(8);
    private int asked = 0;                       // a newer tap wins over a slower older answer

    public RoutesView() {
        Region push = new Region();
        VBox.setVgrow(push, Priority.ALWAYS);

        passingCard.setPadding(new Insets(12, 14, 12, 14));
        passingCard.setBackground(Theme.rounded(Color.rgb(28, 28, 28, 0.96), 10));
        passingCard.setVisible(false);
        passingCard.setManaged(false);

        Label hint = CommonUi.small("Tap the map to see the jeeps that pass there", Theme.WHITE);
        hint.setPadding(new Insets(5, 10, 5, 10));
        hint.setBackground(Theme.rounded(Color.rgb(14, 14, 14, 0.85), 6));
        HBox hintRow = new HBox(hint);
        hintRow.setAlignment(Pos.CENTER);
        hintRow.setPickOnBounds(false);

        VBox overlay = new VBox(14, hintRow, push, passingCard, viewMapButton(), tripPlannerTile());
        overlay.setPadding(new Insets(14, 18, 14, 18));
        overlay.setPickOnBounds(false);

        map.withFarexRoutes();
        map.onPick(this::showPassing);
        getChildren().addAll(map, overlay);
    }

    // ----- Jeeps passing here -----

    private void showPassing(double lat, double lng) {
        int ask = ++asked;
        map.focusRoute(null);
        map.showPin(new RouteStop("Here", lat, lng));
        fillCard(List.of(CommonUi.small("Looking for jeeps…", Theme.WHITE)), 0);
        Thread t = new Thread(() -> {
            List<Api.Passing> found;
            try {
                found = Api.passing(lat, lng);
            } catch (RuntimeException offline) {
                Platform.runLater(() -> {
                    if (ask == asked) fillCard(List.of(TripUi.wrapped("Can’t reach FareX right now. Check your connection.")), 0);
                });
                return;
            }
            Platform.runLater(() -> {
                if (ask != asked) return;
                if (found.isEmpty()) {
                    fillCard(List.of(TripUi.wrapped("No jeepney route passes within 150 m of here. Try a main road.")), 0);
                    return;
                }
                VBox rows = new VBox(6);
                for (Api.Passing p : found) rows.getChildren().add(row(p));
                ScrollPane scroll = new ScrollPane(rows);
                scroll.setFitToWidth(true);
                scroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
                scroll.setMaxHeight(190);
                scroll.setStyle("-fx-background: transparent; -fx-background-color: transparent;");
                fillCard(List.of(scroll), found.size());
            });
        }, "farex-passing");
        t.setDaemon(true);
        t.start();
    }

    // "JEEPS PASSING HERE · 10" with a close ×, then the content
    private void fillCard(List<Node> content, int count) {
        Label title = TripUi.eyebrow(count > 0 ? "JEEPS PASSING HERE · " + count : "JEEPS PASSING HERE");
        Region gap = new Region();
        HBox.setHgrow(gap, Priority.ALWAYS);
        Label close = new Label("×");
        close.setTextFill(Theme.WHITE);
        close.setFont(Theme.bold(18));
        close.setCursor(Cursor.HAND);
        close.setOnMouseClicked(e -> hideCard());
        HBox head = new HBox(title, gap, close);
        head.setAlignment(Pos.CENTER_LEFT);

        passingCard.getChildren().setAll(head);
        passingCard.getChildren().addAll(content);
        passingCard.setVisible(true);
        passingCard.setManaged(true);
    }

    private void hideCard() {
        asked++;
        passingCard.setVisible(false);
        passingCard.setManaged(false);
        map.showPin(null);
        map.setBottomInset(0);
        map.focusRoute(null);
    }

    // "2120-B  Rodriguez Suburban–SM North EDSA" / "44 m away · Pinatubo St → North Ave near EDSA"
    private Node row(Api.Passing p) {
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
        row.setOnMouseClicked(e -> {                                  // show this route above the card
            map.setBottomInset(getHeight() - passingCard.localToScene(0, 0).getY() + 10);
            map.focusRoute(p.routeId());
        });
        return row;
    }

    // ----- the original tiles -----

    private Node viewMapButton() {
        Button view = new Button("VIEW MAP");
        Theme.limeButton(view);
        view.setFont(Theme.bold(15));
        view.setPadding(new Insets(16, 20, 16, 20));
        view.setMaxWidth(Double.MAX_VALUE);
        view.setOnAction(e -> Navigator.go("tripMap"));
        return view;
    }

    // Dark see-through tile: jeepney icon, "Trip Planner", chevron
    private Node tripPlannerTile() {
        StackPane iconTile = new StackPane(Icons.view("icon_trip_planner", 26));
        iconTile.setMinSize(42, 42);
        iconTile.setPrefSize(42, 42);
        iconTile.setMaxSize(42, 42);
        iconTile.setBackground(Theme.rounded(Color.web("#2A2A2A"), 10));

        Label title = new Label("Trip Planner");
        title.setTextFill(Theme.WHITE);
        title.setFont(Theme.bold(17));
        Label sub = new Label("Create Your Own Trip Planner");
        sub.setTextFill(Color.web("#D0D0D0"));
        sub.setFont(Font.font("System", 10));
        VBox text = new VBox(2, title, sub);
        text.setAlignment(Pos.CENTER);
        HBox.setHgrow(text, Priority.ALWAYS);
        text.setMaxWidth(Double.MAX_VALUE);

        Label chevron = new Label("›");
        chevron.setTextFill(Theme.GRAY);
        chevron.setFont(Theme.bold(24));

        HBox tile = new HBox(12, iconTile, text, chevron);
        tile.setAlignment(Pos.CENTER_LEFT);
        tile.setPadding(new Insets(10, 16, 10, 12));
        tile.setBackground(Theme.rounded(Color.rgb(60, 60, 60, 0.92), 6));
        tile.setCursor(Cursor.HAND);
        tile.setOnMouseClicked(e -> Navigator.go("tripNew"));
        return tile;
    }
}
