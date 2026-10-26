package com.farex.ui;

import java.util.ArrayList;
import java.util.List;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.layout.Background;
import javafx.scene.layout.BackgroundFill;
import javafx.scene.layout.Border;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.BorderStroke;
import javafx.scene.layout.BorderStrokeStyle;
import javafx.scene.layout.BorderWidths;
import javafx.scene.layout.CornerRadii;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.paint.CycleMethod;
import javafx.scene.paint.LinearGradient;
import javafx.scene.paint.Stop;
import javafx.scene.shape.Arc;
import javafx.scene.shape.ArcType;
import javafx.scene.shape.Circle;
import javafx.scene.shape.ClosePath;
import javafx.scene.shape.Line;
import javafx.scene.shape.LineTo;
import javafx.scene.shape.MoveTo;
import javafx.scene.shape.Path;
import javafx.scene.shape.Polygon;
import javafx.scene.shape.Polyline;
import javafx.scene.shape.QuadCurveTo;
import javafx.scene.shape.Shape;
import javafx.scene.shape.StrokeLineCap;
import javafx.scene.shape.StrokeLineJoin;

public class MainShell extends BorderPane {

    private static final String[] TITLES = {"HOME", "ROUTES", "TRANSACTIONS", "PROFILE"};
    private static final String[] ICON_NAMES = {
        "icon_tab_home", "icon_tab_routes", "icon_tab_transactions", "icon_tab_profile"};

    private final StackPane contentArea = new StackPane();
    private final Label titleLabel = new Label();
    private final List<NavTab> tabs = new ArrayList<>();

    public MainShell() {
        setBackground(Theme.solid(Theme.DARK));
        setTop(buildHeader());
        setCenter(contentArea);
        setBottom(buildNav());
        showTab(0);
    }

    public void showTab(int index) {
        titleLabel.setText(TITLES[index]);
        for (int i = 0; i < tabs.size(); i++) {
            tabs.get(i).setActive(i == index);
        }
        Node content = (index == 0) ? new HomeView(this::showTab) : placeholderContent(TITLES[index]);
        contentArea.getChildren().setAll(content);
    }

    // Header 

    private Node buildHeader() {
        Label name = new Label("FareX");
        name.setTextFill(Theme.WHITE);
        name.setFont(Theme.bold(16));
        HBox brand = new HBox(8, Assets.logo(26, 7), name);
        brand.setAlignment(Pos.CENTER_LEFT);
        brand.setMaxWidth(Double.NEGATIVE_INFINITY); 

        titleLabel.setTextFill(Theme.LIME);
        titleLabel.setFont(Theme.bold(14));

        StackPane bell = new StackPane(Icons.view("icon_bell", 20));
        bell.setCursor(Cursor.HAND);
        bell.setOnMouseClicked(e -> Navigator.go("notifications"));
        bell.setMaxWidth(Double.NEGATIVE_INFINITY); 

        StackPane header = new StackPane();
        header.getChildren().addAll(brand, titleLabel, bell);

        StackPane.setAlignment(brand, Pos.CENTER_LEFT);
        StackPane.setAlignment(titleLabel, Pos.CENTER);
        StackPane.setAlignment(bell, Pos.CENTER_RIGHT);
        
        header.setPadding(new Insets(14, 20, 10, 20));
        return header;
    }

    // Bottom tabs

    private Node buildNav() {
        HBox nav = new HBox();
        nav.setAlignment(Pos.CENTER);
        nav.setPadding(new Insets(8, 10, 8, 10));
        // Figma: gray bar, light top edge, soft glow along the bottom
        LinearGradient barFill = new LinearGradient(0, 0, 0, 1, true, CycleMethod.NO_CYCLE,
                new Stop(0, Color.web("#3B3B3B")), new Stop(0.55, Color.web("#373737")),
                new Stop(0.85, Color.web("#4A4A4A")), new Stop(1, Color.web("#626262")));
        nav.setBackground(new Background(new BackgroundFill(barFill, new CornerRadii(24), Insets.EMPTY)));
        nav.setBorder(new Border(new BorderStroke(Color.web("#5E5E5E"),
                BorderStrokeStyle.SOLID, new CornerRadii(24), new BorderWidths(1))));
        BorderPane.setMargin(nav, new Insets(6, 14, 14, 14));

        for (int i = 0; i < ICON_NAMES.length; i++) {
            NavTab tab = new NavTab(i);
            tabs.add(tab);
            nav.getChildren().add(tab);
        }
        return nav;
    }

    private class NavTab extends StackPane {
        private final int index;
        private final StackPane pill = new StackPane();

        NavTab(int index) {
            this.index = index;
            pill.setMinSize(52, 42);
            pill.setPrefSize(52, 42);
            pill.setMaxSize(52, 42);
            getChildren().add(pill);

            HBox.setHgrow(this, Priority.ALWAYS);
            setMaxWidth(Double.MAX_VALUE);
            setCursor(Cursor.HAND);
            setOnMouseClicked(e -> showTab(index));
        }

        void setActive(boolean active) {
            pill.getChildren().setAll(iconFor(active));
        }

        private Node iconFor(boolean active) {
            String name = ICON_NAMES[index];
            if (active && Icons.exists(name + "_active")) {
                return Icons.view(name + "_active", 24);
            }
            if (Icons.exists(name)) {
                Node icon = Icons.view(name, 24);
                icon.setOpacity(active ? 1.0 : 0.7);
                return icon;
            }
            return placeholderIcon(index, active);
        }
    }

    // Placeholder tab icons

    private static Node placeholderIcon(int index, boolean active) {
        Color color = active ? Theme.LIME : Color.web("#D9D9D9");
        Group icon = new Group();
        switch (index) {
            case 0:   // home: house with a door, filled olive when active
                Color outline = active ? Color.web("#F2F2F2") : color;
                Polygon house = new Polygon(12, 2.5, 21, 9.5, 21, 21, 3, 21, 3, 9.5);
                style(house, outline);
                if (active) {
                    house.setFill(Color.web("#90AE1A"));
                }
                icon.getChildren().addAll(house, line(outline, 12, 16, 12, 18.5));
                break;
            case 1:   // routes: folded map
                icon.getChildren().addAll(
                        panel(color, 2.5, 5.5, 8, 3.5, 8, 18.5, 2.5, 20.5),
                        panel(color, 9.5, 3.5, 14.5, 5.5, 14.5, 20.5, 9.5, 18.5),
                        panel(color, 16, 5.5, 21.5, 3.5, 21.5, 18.5, 16, 20.5));
                break;
            case 2:   // transactions: receipt with a wavy bottom
                Path receipt = new Path(new MoveTo(5, 21), new LineTo(5, 6),
                        new QuadCurveTo(5, 3, 8, 3), new LineTo(16, 3),
                        new QuadCurveTo(19, 3, 19, 6), new LineTo(19, 21),
                        new LineTo(15.5, 18.5), new LineTo(12, 21), new LineTo(8.5, 18.5),
                        new ClosePath());
                style(receipt, color);
                icon.getChildren().addAll(receipt, line(color, 9.5, 11, 14.5, 11));
                break;
            default:  // profile
                Circle head = new Circle(12, 7.5, 4.5);
                style(head, color);
                Arc shoulders = new Arc(12, 22, 8.5, 7.5, 0, 180);
                shoulders.setType(ArcType.OPEN);
                style(shoulders, color);
                icon.getChildren().addAll(head, shoulders);
                break;
        }
        Line pad = new Line(0, 0, 24, 24);
        pad.setStroke(Color.TRANSPARENT);
        icon.getChildren().add(0, pad);
        return icon;
    }

    private static Polyline line(Color color, double... points) {
        Polyline polyline = new Polyline(points);
        style(polyline, color);
        return polyline;
    }

    private static Polygon panel(Color color, double... points) {
        Polygon polygon = new Polygon(points);
        style(polygon, color);
        return polygon;
    }

    private static void style(Shape shape, Color color) {
        shape.setFill(null);
        shape.setStroke(color);
        shape.setStrokeWidth(1.6);
        shape.setStrokeLineCap(StrokeLineCap.ROUND);
        shape.setStrokeLineJoin(StrokeLineJoin.ROUND);
    }

    // Tabs with no design

    private Node placeholderContent(String title) {
        Label message = Theme.bodyText(title + " isn't designed yet.", Theme.GRAY);
        VBox box = new VBox(message);
        box.setAlignment(Pos.CENTER);
        return box;
    }
}
