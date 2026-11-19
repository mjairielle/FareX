package com.farex.ui.home;

import java.util.ArrayList;
import java.util.List;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;

import com.farex.ui.common.Icons;
import com.farex.ui.common.Navigator;
import com.farex.ui.common.SubScreen;
import com.farex.ui.common.Theme;
import com.farex.ui.data.DemoAccount;
import com.farex.ui.data.DemoNotification;
import com.farex.ui.data.MockData;

// Notification: unread messages under "Latest", read ones under "Other Messages".
// Tapping an unread message marks it read and moves it down.
public class NotificationsView extends SubScreen {

    private static final Color PANEL = Color.web("#1E1E1E");
    private static final Color LINE = Color.web("#2A2A2A");

    public NotificationsView() {
        super("Notification", "dashboard");

        DemoAccount account = MockData.getCurrentAccount();
        List<DemoNotification> all = (account == null) ? new ArrayList<>() : account.getNotifications();

        if (all.isEmpty()) {
            showEmpty();
        } else {
            showList(all);
        }
    }

    // ----- Empty state -----

    private void showEmpty() {
        Label message = new Label("No notification yet!");
        message.setTextFill(Color.web("#6B6B6B"));
        message.setFont(Theme.bold(14));

        VBox box = new VBox(14, Icons.view("icon_bell_off", 84), message);
        box.setAlignment(Pos.CENTER);
        box.setTranslateY(-30);   // Figma: a little above the middle

        body.setAlignment(Pos.CENTER);
        body.getChildren().add(box);
    }

    // ----- List state -----

    private void showList(List<DemoNotification> all) {
        List<DemoNotification> latest = new ArrayList<>();
        List<DemoNotification> other = new ArrayList<>();
        for (DemoNotification n : all) {
            (n.isRead() ? other : latest).add(n);
        }

        VBox list = new VBox(12);
        list.setPadding(new Insets(0, 0, 24, 0));
        if (!latest.isEmpty()) {
            list.getChildren().addAll(sectionTitle("Latest"), panel(latest));
        }
        if (!other.isEmpty()) {
            Label title = sectionTitle("Other Messages");
            if (!latest.isEmpty()) {
                VBox.setMargin(title, new Insets(14, 0, 0, 0));
            }
            list.getChildren().addAll(title, panel(other));
        }

        ScrollPane scroll = new ScrollPane(list);
        scroll.setFitToWidth(true);
        scroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scroll.setVbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scroll.setStyle("-fx-background: transparent; -fx-background-color: transparent;");
        VBox.setVgrow(scroll, Priority.ALWAYS);

        body.setPadding(new Insets(22, 20, 0, 20));
        body.getChildren().add(scroll);
    }

    private Label sectionTitle(String text) {
        Label label = new Label(text);
        label.setTextFill(Theme.WHITE);
        label.setFont(Theme.bold(16));
        return label;
    }

    // One rounded panel with a thin line between messages
    private Node panel(List<DemoNotification> items) {
        VBox panel = new VBox();
        panel.setBackground(Theme.rounded(PANEL, 10));
        for (int i = 0; i < items.size(); i++) {
            if (i > 0) {
                panel.getChildren().add(separator());
            }
            panel.getChildren().add(row(items.get(i)));
        }
        return panel;
    }

    private Node row(DemoNotification n) {
        boolean unread = !n.isRead();

        Label title = new Label(n.getTitle());
        title.setTextFill(unread ? Theme.LIME : Theme.WHITE);
        title.setFont(Font.font("System", 14));

        VBox text = new VBox(2, title, message(n.getMessage()));
        HBox.setHgrow(text, Priority.ALWAYS);

        Node icon = Icons.view(unread ? "icon_mail_unread" : "icon_mail", 28);

        HBox row = new HBox(14, icon, text);
        row.setAlignment(Pos.CENTER_LEFT);
        row.setPadding(new Insets(12, 14, 12, 14));

        if (unread) {
            row.setCursor(Cursor.HAND);
            row.setOnMouseClicked(e -> {
                n.markRead();
                Navigator.go("notifications");   // rebuild: the message moves to "Other Messages"
            });
        }
        return row;
    }

    // "You have paid **₱300.00** using GCash." -> normal, bold, normal
    private TextFlow message(String raw) {
        TextFlow flow = new TextFlow();
        String[] parts = raw.split("\\*\\*");
        for (int i = 0; i < parts.length; i++) {
            Text piece = new Text(parts[i]);
            piece.setFill(Theme.WHITE);
            piece.setFont(i % 2 == 1 ? Theme.bold(11) : Font.font("System", 11));   // odd parts were inside **
            flow.getChildren().add(piece);
        }
        return flow;
    }

    private Region separator() {
        Region line = new Region();
        line.setMinHeight(1);
        line.setPrefHeight(1);
        line.setMaxHeight(1);
        line.setBackground(Theme.solid(LINE));
        VBox.setMargin(line, new Insets(0, 14, 0, 14));
        return line;
    }
}
