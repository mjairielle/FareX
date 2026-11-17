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
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;

// Profile / 04: privacy and security settings.
// Location access (07) and Notification preferences (08) open as pop-ups on this screen;
// Change password, Terms and Delete account open their own pages.
public class PrivacyView extends SubScreen {

    private static final String[][] LOCATION_OPTIONS = {
        {ProfileState.WHILE_USING, "Find nearby stops and improve your pickup location."},
        {ProfileState.ASK, "Choose whether to share your location each time."},
        {ProfileState.NEVER, "Enter your pickup location and search for stops manually."}};

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
                row("Change password", Theme.WHITE, () -> Navigator.go("changePassword")),
                row("Location access", Theme.WHITE, this::showLocationAccess),
                row("Notification preferences", Theme.WHITE, this::showNotificationPrefs),
                row("Terms & Privacy Policy", Theme.WHITE, () -> Navigator.go("terms")),
                row("Delete account", Theme.LIME, () -> Navigator.go("deleteAccount")),
                FetchUi.spacer(),
                footer);
    }

    private Node row(String text, Color color, Runnable onClick) {
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
        row.setOnMouseClicked(e -> onClick.run());
        return row;
    }

    // ----- Location access (pop-up): one of three options, then Save -----

    private void showLocationAccess() {
        LocationOptions options = new LocationOptions(ProfileState.getLocation());

        new PopupDialog("Location access", "Choose when FareX can use your location.")
                .add(options, wrapped("Your location helps FareX find nearby stops. "
                        + "You can also change access in your device settings."))
                .button("Save preferences", () -> ProfileState.setLocation(options.choice))
                .link("Cancel", () -> { })
                .show();
    }

    // The three options; clicking one gives it the lime outline and filled radio
    private class LocationOptions extends VBox {
        private String choice;

        LocationOptions(String start) {
            super(8);
            choice = start;
            draw();
        }

        private void draw() {
            getChildren().clear();
            for (String[] option : LOCATION_OPTIONS) {
                boolean selected = option[0].equals(choice);
                HBox row = settingRow(option[0], option[1], radio(selected));
                row.setBorder(new Border(new BorderStroke(selected ? Theme.LIME : Color.TRANSPARENT,
                        BorderStrokeStyle.SOLID, new CornerRadii(8), new BorderWidths(1.2))));
                row.setCursor(Cursor.HAND);
                row.setOnMouseClicked(e -> {
                    choice = option[0];
                    draw();
                });
                getChildren().add(row);
            }
        }
    }

    // Radio circle: lime ring with a dot when chosen
    private Node radio(boolean selected) {
        Circle ring = new Circle(7);
        ring.setFill(Color.TRANSPARENT);
        ring.setStroke(selected ? Theme.LIME : Theme.WHITE);
        ring.setStrokeWidth(1.5);
        StackPane radio = new StackPane(ring);
        if (selected) {
            radio.getChildren().add(new Circle(3.5, Theme.LIME));
        }
        return radio;
    }

    // ----- Notification preferences (pop-up): three switches, Account & security always on -----

    private void showNotificationPrefs() {
        ToggleSwitch trip = new ToggleSwitch(ProfileState.isTripUpdates());
        ToggleSwitch card = new ToggleSwitch(ProfileState.isCardAlerts());
        ToggleSwitch offers = new ToggleSwitch(ProfileState.isOffers());

        VBox rows = new VBox(8,
                settingRow("Trip updates", "Arrival times, delays and trip changes.", trip),
                settingRow("Card & balance alerts", "Payments, top-ups and low balance.", card),
                settingRow("Offers & promotions", "FareX deals, news and rewards.", offers),
                settingRow("Account & security", "Security and account messages stay on.",
                        FetchUi.smallBold("Always on", Theme.LIME)));

        new PopupDialog("Notification preferences", "Choose the updates you want to receive.")
                .add(rows, wrapped("Push notifications also need to be enabled in your device settings."))
                .button("Save preferences", () -> ProfileState.setNotifications(trip.isOn(), card.isOn(), offers.isOn()))
                .link("Cancel", () -> { })
                .show();
    }

    // ----- Shared by both pop-ups -----

    // Title + description on the left, a control (radio, switch, "Always on") on the right
    private HBox settingRow(String title, String description, Node control) {
        VBox text = new VBox(3, TripUi.bold(title, 12), wrapped(description));
        HBox.setHgrow(text, Priority.ALWAYS);
        if (control instanceof Region region) {
            region.setMinWidth(Region.USE_PREF_SIZE);   // "Always on" never gets cut to "Alw…"
        }
        HBox row = new HBox(10, text, control);
        row.setAlignment(Pos.CENTER_LEFT);
        row.setPadding(new Insets(10, 12, 10, 12));
        row.setBackground(Theme.rounded(Color.web("#141414"), 8));
        return row;
    }

    private Label wrapped(String text) {
        Label label = FetchUi.small(text, Theme.WHITE);
        label.setWrapText(true);
        return label;
    }
}
