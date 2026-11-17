package com.farex.ui.trip;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;

import com.farex.ui.common.Icons;
import com.farex.ui.common.MapView;
import com.farex.ui.common.Navigator;
import com.farex.ui.common.Theme;

// Routes tab (inside MainShell): the map with VIEW MAP and the Trip Planner tile on top
public class RoutesView extends StackPane {

    public RoutesView() {
        Region push = new Region();
        VBox.setVgrow(push, Priority.ALWAYS);

        VBox overlay = new VBox(14, push, viewMapButton(), tripPlannerTile());
        overlay.setPadding(new Insets(0, 18, 14, 18));
        overlay.setPickOnBounds(false);

        getChildren().addAll(new MapView(), overlay);
    }

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
