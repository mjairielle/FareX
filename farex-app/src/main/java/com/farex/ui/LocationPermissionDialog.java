package com.farex.ui;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.Border;
import javafx.scene.layout.BorderStroke;
import javafx.scene.layout.BorderStrokeStyle;
import javafx.scene.layout.BorderWidths;
import javafx.scene.layout.CornerRadii;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.TextAlignment;

// Pretend phone permission prompt. Allow -> onAllowed (trip in progress),
// Don't allow -> onDenied ("Tracking unavailable")
public class LocationPermissionDialog extends StackPane {

    private final Runnable onAllowed;
    private final Runnable onDenied;

    public LocationPermissionDialog(Runnable onAllowed, Runnable onDenied) {
        this.onAllowed = onAllowed;
        this.onDenied = onDenied;
        setBackground(Theme.solid(Color.rgb(0, 0, 0, 0.45)));

        Label title = new Label("Allow FareX to use your location?");
        title.setTextFill(Theme.WHITE);
        title.setFont(Theme.bold(15));
        title.setWrapText(true);
        title.setTextAlignment(TextAlignment.CENTER);

        Label text = FetchUi.small("Used only while a trip is being tracked.", Theme.WHITE);

        Button allow = new Button("Allow");
        allow.setFont(Theme.bold(12));
        allow.setPadding(new Insets(8));
        allow.setMaxWidth(Double.MAX_VALUE);
        Theme.paintButton(allow, "#CAFE06", "#D8FF3A", "#A9D400", 6, "black");
        allow.setOnAction(e -> answer(true));

        Button deny = new Button("Don’t allow");
        deny.setFont(Theme.bold(12));
        deny.setPadding(new Insets(8));
        deny.setMaxWidth(Double.MAX_VALUE);
        Theme.paintButton(deny, "#141414", "#1E1E1E", "#0E0E0E", 6, "white");
        deny.setBorder(new Border(new BorderStroke(Color.web("#4A4A4A"),
                BorderStrokeStyle.SOLID, new CornerRadii(6), new BorderWidths(1))));
        deny.setOnAction(e -> answer(false));

        VBox box = new VBox(10, title, text, allow, deny);
        box.setAlignment(Pos.CENTER);
        box.setPadding(new Insets(22, 18, 18, 18));
        box.setMaxSize(260, VBox.USE_PREF_SIZE);
        box.setBackground(Theme.rounded(Color.web("#1C1C1C"), 10));
        box.setBorder(new Border(new BorderStroke(Theme.LIME,
                BorderStrokeStyle.SOLID, new CornerRadii(10), new BorderWidths(1.5))));
        VBox.setMargin(allow, new Insets(6, 0, 0, 0));

        getChildren().add(box);
    }

    private void answer(boolean allowed) {
        TripState.setLocationAllowed(allowed);
        Navigator.closeOverlay();
        if (allowed) {
            onAllowed.run();
        } else {
            onDenied.run();
        }
    }

    // "Allow location" / "Enable location": ask, unless already allowed earlier
    public static void ask(Runnable onAllowed, Runnable onDenied) {
        if (TripState.isLocationAllowed()) {
            onAllowed.run();
        } else {
            Navigator.showOverlay(new LocationPermissionDialog(onAllowed, onDenied));
        }
    }
}
