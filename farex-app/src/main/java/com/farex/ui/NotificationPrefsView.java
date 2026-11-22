package com.farex.ui;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

// Profile / 08: three on/off switches plus "Account & security" (always on)
public class NotificationPrefsView extends SubScreen {

    private final ToggleSwitch trip = new ToggleSwitch(ProfileState.isTripUpdates());
    private final ToggleSwitch card = new ToggleSwitch(ProfileState.isCardAlerts());
    private final ToggleSwitch offers = new ToggleSwitch(ProfileState.isOffers());

    public NotificationPrefsView() {
        super("Notifications", "privacy");

        VBox heading = new VBox(6,
                FetchUi.title("Notification preferences"),
                FetchUi.subtitle("Choose the updates you want to receive."));
        VBox.setMargin(heading, new Insets(0, 0, 6, 0));

        body.setSpacing(10);
        body.getChildren().addAll(
                heading,
                row("Trip updates", "Arrival times, delays and trip changes.", trip),
                row("Card & balance alerts", "Payments, top-ups and low balance.", card),
                row("Offers & promotions", "FareX deals, news and rewards.", offers),
                row("Account & security",
                        "Important security and account messages stay on to help protect your account.",
                        FetchUi.smallBold("Always on", Theme.LIME)),
                FetchUi.small("Push notifications also need to be enabled in your device settings.", Theme.WHITE),
                FetchUi.spacer(),
                FetchUi.limeButton("Save preferences", () -> {
                    ProfileState.setNotifications(trip.isOn(), card.isOn(), offers.isOn());
                    Navigator.go("privacy");
                }),
                FetchUi.link("Cancel", () -> Navigator.go("privacy")));
    }

    private Node row(String title, String description, Node control) {
        VBox text = new VBox(4, TripUi.bold(title, 12), FetchUi.subtitle(description));
        HBox.setHgrow(text, Priority.ALWAYS);
        HBox row = new HBox(12, text, control);
        row.setAlignment(Pos.CENTER_LEFT);
        row.setPadding(new Insets(14));
        row.setBackground(Theme.rounded(FetchUi.PANEL, 8));
        return row;
    }
}
