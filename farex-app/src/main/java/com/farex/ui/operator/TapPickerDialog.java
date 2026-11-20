package com.farex.ui.operator;

import java.util.function.Consumer;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
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

import com.farex.ui.common.CommonUi;
import com.farex.ui.common.Navigator;
import com.farex.ui.common.Theme;
import com.farex.ui.trip.TripUi;

// "Simulate a tap": the list behind the Ready Mode drop-down. Each row is a card or app user;
// the right side says what tapping does now ("Tap in" or "Tap out" if already on board).
public class TapPickerDialog extends StackPane {

    public TapPickerDialog(Consumer<TapRider> onPick) {
        setBackground(Theme.solid(Color.rgb(0, 0, 0, 0.45)));

        VBox rows = new VBox(8);
        for (TapRider rider : TapTerminal.riders()) {
            rows.getChildren().add(row(rider, onPick));
        }

        Button cancel = OperatorUi.outlineButton("Cancel", Navigator::closeOverlay);
        VBox.setMargin(cancel, new Insets(4, 0, 0, 0));

        VBox box = new VBox(10,
                TripUi.bold("Simulate a tap", 18),
                wrapped("Pick who taps the reader. Their second tap is the tap out."),
                rows, cancel);
        box.setPadding(new Insets(20, 18, 18, 18));
        box.setMaxSize(316, VBox.USE_PREF_SIZE);
        box.setBackground(Theme.rounded(Color.web("#1C1C1C"), 10));
        box.setBorder(new Border(new BorderStroke(Theme.LIME,
                BorderStrokeStyle.SOLID, new CornerRadii(10), new BorderWidths(1.5))));
        getChildren().add(box);
    }

    private Label wrapped(String text) {
        Label label = CommonUi.small(text, Theme.WHITE);
        label.setWrapText(true);
        return label;
    }

    // "Aron (Regular)" / "00 6767 6767 · ₱676.50"  ............  "Tap in"
    private Node row(TapRider rider, Consumer<TapRider> onPick) {
        Label name = CommonUi.smallBold(rider.getPickerName(), Theme.WHITE);
        name.setFont(Theme.bold(12));
        String detail = rider.getDisplayId() + " · " + CommonUi.peso(rider.getBalance());
        if (rider.isFrozen()) {
            detail += " · Frozen";
        }
        VBox left = new VBox(2, name, CommonUi.small(detail, Theme.GRAY));
        HBox.setHgrow(left, Priority.ALWAYS);

        String boardedAt = TapTerminal.boardedAt(rider);
        Label action = (boardedAt == null)
                ? CommonUi.smallBold("Tap in", Theme.WHITE)
                : CommonUi.smallBold("Tap out", Theme.LIME);
        action.setMinWidth(Label.USE_PREF_SIZE);
        VBox right = new VBox(2, action);
        right.setMinWidth(VBox.USE_PREF_SIZE);
        right.setAlignment(Pos.CENTER_RIGHT);
        if (boardedAt != null) {
            right.getChildren().add(CommonUi.small("from " + boardedAt, Theme.GRAY));
        }

        HBox row = new HBox(8, left, right);
        row.setAlignment(Pos.CENTER_LEFT);
        row.setPadding(new Insets(10, 12, 10, 12));
        row.setBackground(Theme.rounded(Color.web("#262626"), 8));
        row.setCursor(Cursor.HAND);
        row.setOnMouseClicked(e -> {
            Navigator.closeOverlay();
            onPick.accept(rider);
        });
        return row;
    }
}
