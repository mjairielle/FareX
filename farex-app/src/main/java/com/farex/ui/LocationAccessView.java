package com.farex.ui;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.Node;
import javafx.scene.layout.Border;
import javafx.scene.layout.BorderStroke;
import javafx.scene.layout.BorderStrokeStyle;
import javafx.scene.layout.BorderWidths;
import javafx.scene.layout.CornerRadii;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;

// Profile / 07: pick one of three location options (radio buttons), then save
public class LocationAccessView extends SubScreen {

    private static final String[][] OPTIONS = {
        {ProfileState.WHILE_USING, "Find nearby stops and improve your pickup location."},
        {ProfileState.ASK, "Choose whether to share your location each time."},
        {ProfileState.NEVER, "Enter your pickup location and search for stops manually."}};

    private final VBox optionBox = new VBox(10);
    private String choice = ProfileState.getLocation();

    public LocationAccessView() {
        super("Location access", "privacy");

        VBox heading = new VBox(6,
                FetchUi.title("Location access"),
                FetchUi.subtitle("Choose when FareX can use your location."));
        VBox.setMargin(heading, new Insets(0, 0, 6, 0));

        drawOptions();

        body.setSpacing(12);
        body.getChildren().addAll(
                heading,
                optionBox,
                FetchUi.subtitle("Your location helps FareX find nearby stops. "
                        + "You can also change access in your device settings."),
                FetchUi.spacer(),
                FetchUi.limeButton("Save preferences", () -> {
                    ProfileState.setLocation(choice);
                    Navigator.go("privacy");
                }),
                FetchUi.link("Cancel", () -> Navigator.go("privacy")));
    }

    private void drawOptions() {
        optionBox.getChildren().clear();
        for (String[] option : OPTIONS) {
            optionBox.getChildren().add(option(option[0], option[1]));
        }
    }

    // Title + description on the left, radio circle on the right; the chosen one has a lime outline
    private Node option(String title, String description) {
        boolean selected = title.equals(choice);

        VBox text = new VBox(4, TripUi.bold(title, 12), FetchUi.subtitle(description));
        HBox.setHgrow(text, Priority.ALWAYS);

        HBox row = new HBox(12, text, radio(selected));
        row.setAlignment(Pos.CENTER_LEFT);
        row.setPadding(new Insets(14));
        row.setBackground(Theme.rounded(FetchUi.PANEL, 8));
        row.setBorder(new Border(new BorderStroke(selected ? Theme.LIME : Color.TRANSPARENT,
                BorderStrokeStyle.SOLID, new CornerRadii(8), new BorderWidths(1.2))));
        row.setCursor(Cursor.HAND);
        row.setOnMouseClicked(e -> {
            choice = title;
            drawOptions();
        });
        return row;
    }

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
}
