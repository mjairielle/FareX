package com.farex.ui;

import java.util.ArrayList;
import java.util.List;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

// Operator Mode main screen: "FareX  OPERATOR" header, the tab content, 3 bottom tabs.
// Tabs: 0 Dashboard, 1 Ready Mode, 2 History
public class OperatorShell extends BorderPane {

    private static final String[] TITLES = {"OPERATOR", "READY MODE", "HISTORY"};
    private static final String[] ICON_NAMES = {"icon_op_home", "icon_op_qr", "icon_op_history"};

    private final StackPane contentArea = new StackPane();
    private final Label titleLabel = new Label();
    private final List<StackPane> tabs = new ArrayList<>();

    public OperatorShell(int startTab) {
        setBackground(Theme.solid(Theme.DARK));
        setTop(buildHeader());
        setCenter(contentArea);
        setBottom(buildNav());
        showTab(startTab);
    }

    public void showTab(int index) {
        OperatorState.setLastTab(index);
        titleLabel.setText(TITLES[index]);
        for (int i = 0; i < tabs.size(); i++) {
            int size = (i == index) ? 28 : 26;
            String name = ICON_NAMES[i] + (i == index ? "_on" : "");
            tabs.get(i).getChildren().setAll(Icons.view(name, size));
        }

        Node content;
        if (index == 0) {
            content = new OperatorDashboardView(this::showTab);
        } else if (index == 1) {
            content = new ReadyModeView(this::showTab);
        } else {
            content = placeholder("History");
        }
        contentArea.getChildren().setAll(content);
    }

    // ----- Header: logo + "FareX" on the left, lime title in the middle -----

    private Node buildHeader() {
        Label name = new Label("FareX");
        name.setTextFill(Theme.WHITE);
        name.setFont(Theme.bold(16));
        HBox brand = new HBox(8, Assets.logo(26, 7), name);
        brand.setAlignment(Pos.CENTER_LEFT);

        titleLabel.setTextFill(Theme.LIME);
        titleLabel.setFont(Theme.bold(14));

        HBox header = new HBox(28, brand, titleLabel);
        header.setAlignment(Pos.CENTER_LEFT);
        header.setPadding(new Insets(18, 20, 12, 20));
        return header;
    }

    // ----- Bottom tabs (same bar as the passenger side) -----

    private Node buildNav() {
        HBox nav = new HBox();
        MainShell.styleNavBar(nav);
        for (int i = 0; i < ICON_NAMES.length; i++) {
            int index = i;
            StackPane tab = new StackPane();
            tab.setMinHeight(42);
            HBox.setHgrow(tab, Priority.ALWAYS);
            tab.setMaxWidth(Double.MAX_VALUE);
            tab.setCursor(Cursor.HAND);
            tab.setOnMouseClicked(e -> showTab(index));
            tabs.add(tab);
            nav.getChildren().add(tab);
        }
        return nav;
    }

    // ----- Tabs built in later parts -----

    private Node placeholder(String title) {
        Label message = Theme.bodyText(title + " isn't built yet.", Theme.GRAY);
        VBox box = new VBox(message);
        box.setAlignment(Pos.CENTER);
        return box;
    }
}
