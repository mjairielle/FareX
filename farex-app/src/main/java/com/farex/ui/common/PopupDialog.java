package com.farex.ui.common;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.TextAlignment;

import com.farex.ui.trip.TripUi;

// A pop-up shown over the current screen instead of a new page.
// Result look: icon, centered title and sentence. Form look (no icon): left-aligned title and
// sentence, e.g. "Location access" with its options. Then any panels/notes, a lime button, links.
// Every button closes the pop-up first, then runs its action.
// Example:
//   new PopupDialog(CommonUi.doubleCheck(), "Load fetched successfully", "Your card is updated.")
//       .add(panel).button("Back to Home", FetchLoadState::goHome).show();
public class PopupDialog extends StackPane {

    private final VBox box = new VBox(12);

    // Result look: icon on top, everything centered
    public PopupDialog(Node icon, String title, String text) {
        this(title, text);
        VBox top = (VBox) box.getChildren().get(0);
        top.getChildren().add(0, icon);
        top.setAlignment(Pos.TOP_CENTER);
        for (Node node : top.getChildren()) {
            if (node instanceof Label label) {
                label.setTextAlignment(TextAlignment.CENTER);
            }
        }
    }

    // Form look: no icon, left-aligned
    public PopupDialog(String title, String text) {
        // Dim layer over the blurred screen; it also stops clicks reaching the screen below
        setBackground(Theme.solid(Color.rgb(0, 0, 0, 0.45)));

        VBox top = new VBox(8, TripUi.bold(title, 19), CommonUi.subtitle(text));
        VBox.setMargin(top, new Insets(0, 0, 4, 0));

        box.getChildren().add(top);
        box.setPadding(new Insets(22, 18, 18, 18));
        box.setMaxSize(316, VBox.USE_PREF_SIZE);
        box.setBackground(Theme.rounded(Color.web("#1E1E1E"), 14));
        getChildren().add(box);
    }

    // Panels, notes, anything between the sentence and the buttons
    public PopupDialog add(Node... nodes) {
        box.getChildren().addAll(nodes);
        return this;
    }

    public PopupDialog button(String text, Runnable action) {
        box.getChildren().add(CommonUi.limeButton(text, closeThen(action)));
        return this;
    }

    public PopupDialog link(String text, Runnable action) {
        box.getChildren().add(CommonUi.link(text, closeThen(action)));
        return this;
    }

    public void show() {
        Navigator.showOverlay(this);
    }

    private Runnable closeThen(Runnable action) {
        return () -> {
            Navigator.closeOverlay();
            action.run();
        };
    }
}
