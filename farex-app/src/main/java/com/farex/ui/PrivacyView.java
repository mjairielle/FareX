package com.farex.ui;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;

// Profile / 04: list of privacy and security settings
public class PrivacyView extends SubScreen {

    public PrivacyView() {
        super("Privacy & Security", "profile");

        VBox heading = new VBox(6,
                FetchUi.title("Privacy & Security"),
                FetchUi.subtitle("Manage your privacy and account security."));
        VBox.setMargin(heading, new Insets(0, 0, 6, 0));

        Label footer = FetchUi.small("You’re in control of what you share.", Theme.WHITE);
        footer.setMaxWidth(Double.MAX_VALUE);
        footer.setAlignment(Pos.CENTER);

        body.setSpacing(10);
        body.getChildren().addAll(
                heading,
                row("Change password", Theme.WHITE, "changePassword"),
                row("Location access", Theme.WHITE, "locationAccess"),
                row("Notification preferences", Theme.WHITE, "notificationPrefs"),
                row("Terms & Privacy Policy", Theme.WHITE, "terms"),
                row("Delete account", Theme.LIME, "deleteAccount"),
                FetchUi.spacer(),
                footer);
    }

    private Node row(String text, Color color, String target) {
        Label label = new Label(text);
        label.setTextFill(color);
        label.setFont(Theme.bold(12));
        Region fill = new Region();
        HBox.setHgrow(fill, Priority.ALWAYS);
        Label chevron = new Label("›");
        chevron.setTextFill(Theme.WHITE);
        chevron.setFont(Theme.bold(20));

        HBox row = new HBox(8, label, fill, chevron);
        row.setAlignment(Pos.CENTER_LEFT);
        row.setPadding(new Insets(14, 16, 14, 16));
        row.setBackground(Theme.rounded(FetchUi.PANEL, 8));
        row.setCursor(Cursor.HAND);
        row.setOnMouseClicked(e -> Navigator.go(target));
        return row;
    }
}
