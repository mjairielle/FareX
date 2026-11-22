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

// Trip Planner / 02: "Search routes" tapped with a location missing
public class TripWarningDialog extends StackPane {

    public TripWarningDialog() {
        // Dim layer over the blurred screen; it also stops clicks reaching the screen below
        setBackground(Theme.solid(Color.rgb(0, 0, 0, 0.45)));

        Label title = new Label("Warning");
        title.setTextFill(Theme.WHITE);
        title.setFont(Theme.bold(18));

        Label message = FetchUi.small("Choose a starting point and destination before searching. "
                + "They must be different places.", Theme.WHITE);
        message.setWrapText(true);
        message.setTextAlignment(TextAlignment.CENTER);

        Button add = new Button("Add both locations");
        add.setFont(Theme.bold(11));
        add.setPadding(new Insets(7));
        add.setMaxWidth(Double.MAX_VALUE);
        Theme.paintButton(add, "#CAFE06", "#D8FF3A", "#A9D400", 4, "black");
        // Goes to whichever location is still missing
        add.setOnAction(e -> {
            Navigator.closeOverlay();
            Navigator.go(TripState.getStart() == null ? "tripStart" : "tripDestination");
        });
        VBox.setMargin(add, new Insets(8, 0, 0, 0));

        VBox box = new VBox(10, WarningDialog.warningIcon(), title, message, add);
        box.setAlignment(Pos.CENTER);
        box.setPadding(new Insets(24, 16, 18, 16));
        box.setMaxSize(220, VBox.USE_PREF_SIZE);
        box.setBackground(Theme.rounded(Color.web("#1C1C1C"), 6));
        box.setBorder(new Border(new BorderStroke(Theme.LIME,
                BorderStrokeStyle.SOLID, new CornerRadii(6), new BorderWidths(1))));

        getChildren().add(box);

        // Tapping the dim area outside the box closes it
        setOnMouseClicked(e -> {
            if (e.getTarget() == this) {
                Navigator.closeOverlay();
            }
        });
    }
}
