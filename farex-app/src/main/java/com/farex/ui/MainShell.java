package com.farex.ui;

import java.util.ArrayList;
import java.util.List;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.control.Label;
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
import javafx.scene.shape.Arc;
import javafx.scene.shape.ArcType;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Line;
import javafx.scene.shape.Polyline;
import javafx.scene.shape.Shape;
import javafx.scene.shape.StrokeLineCap;
import javafx.scene.shape.StrokeLineJoin;

/**
 * The passenger app frame: fixed header (logo, title, bell) on top, fixed bottom tabs,
 * and the tab's content in between. Registered as "dashboard".
 * Tab 0 = Home, 1 = Routes, 2 = Transactions, 3 = Profile.
 */
public class MainShell extends BorderPane {

    private static final String[] TITLES = {"HOME", "ROUTES", "TRANSACTIONS", "PROFILE"};
    // When you export these as PNGs, the tabs switch to them automatically:
    // icon_tab_home.png (+ icon_tab_home_active.png), icon_tab_routes.png, icon_tab_transactions.png, icon_tab_profile.png
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

    // ---------- Header ----------

    private Node buildHeader() {
        Label name = new Label("FareX");
        name.setTextFill(Theme.WHITE);
        name.setFont(Theme.bold(16));
        HBox brand = new HBox(8, Assets.logo(26, 7), name);
        brand.setAlignment(Pos.CENTER_LEFT);
        // Prevent the brand from expanding across the entire header width
        brand.setMaxWidth(Double.NEGATIVE_INFINITY); 

        titleLabel.setTextFill(Theme.LIME);
        titleLabel.setFont(Theme.bold(14));

        StackPane bell = new StackPane(Icons.view("icon_bell", 20));
        bell.setCursor(Cursor.HAND);
        bell.setOnMouseClicked(e -> Navigator.go("notifications"));
        // Prevent the bell container from expanding and forcing itself into the center
        bell.setMaxWidth(Double.NEGATIVE_INFINITY); 

        // Use a StackPane to handle perfect absolute centering layers
        StackPane header = new StackPane();
        header.getChildren().addAll(brand, titleLabel, bell);
        
        // Lock their individual alignments securely
        StackPane.setAlignment(brand, Pos.CENTER_LEFT);
        StackPane.setAlignment(titleLabel, Pos.CENTER);
        StackPane.setAlignment(bell, Pos.CENTER_RIGHT);
        
        header.setPadding(new Insets(14, 20, 10, 20));
        return header;
    }

    // ---------- Bottom tabs ----------

    private Node buildNav() {
        HBox nav = new HBox();
        nav.setAlignment(Pos.CENTER);
        nav.setPadding(new Insets(8, 10, 8, 10));
        nav.setBackground(Theme.rounded(Color.web("#1C1C1C"), 26));
        nav.setBorder(new Border(new BorderStroke(Color.web("#2E2E2E"),
                BorderStrokeStyle.SOLID, new CornerRadii(26), new BorderWidths(1))));
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
            pill.setBackground(active ? Theme.rounded(Color.rgb(202, 254, 6, 0.16), 14) : null);
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
            return placeholderIcon(index, active ? Theme.LIME : Color.web("#8A8A8A"));
        }
    }

    // ---------- Placeholder tab icons (simple drawn shapes until the real icons arrive) ----------

    private static Node placeholderIcon(int index, Color color) {
        Group icon = new Group();
        switch (index) {
            case 0:   // home: roof + walls
                icon.getChildren().addAll(
                        line(color, 3, 12, 12, 3, 21, 12),
                        line(color, 6, 10, 6, 21, 18, 21, 18, 10));
                break;
            case 1:   // routes: zigzag with a dot at each end
                icon.getChildren().addAll(
                        line(color, 7, 17, 10, 8, 14, 16, 17, 7),
                        dot(color, 7, 17), dot(color, 17, 7));
                break;
            case 2:   // transactions: receipt
                icon.getChildren().addAll(
                        line(color, 6, 3, 18, 3, 18, 21, 15, 19, 12, 21, 9, 19, 6, 21, 6, 3),
                        line(color, 9, 9, 15, 9),
                        line(color, 9, 13, 15, 13));
                break;
            default:  // profile: head + shoulders
                Circle head = new Circle(12, 8, 4);
                style(head, color);
                Arc shoulders = new Arc(12, 21, 8, 7, 0, 180);
                shoulders.setType(ArcType.OPEN);
                style(shoulders, color);
                icon.getChildren().addAll(head, shoulders);
                break;
        }
        // Fixed 24 x 24 area so every tab icon sits at the same place
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

    private static Circle dot(Color color, double x, double y) {
        Circle circle = new Circle(x, y, 2);
        circle.setFill(color);
        circle.setStroke(null);
        return circle;
    }

    private static void style(Shape shape, Color color) {
        shape.setFill(null);
        shape.setStroke(color);
        shape.setStrokeWidth(2);
        shape.setStrokeLineCap(StrokeLineCap.ROUND);
        shape.setStrokeLineJoin(StrokeLineJoin.ROUND);
    }

    // ---------- Tabs without a design yet ----------

    private Node placeholderContent(String title) {
        Label message = Theme.bodyText(title + " isn't designed yet.", Theme.GRAY);
        VBox box = new VBox(message);
        box.setAlignment(Pos.CENTER);
        return box;
    }
}
