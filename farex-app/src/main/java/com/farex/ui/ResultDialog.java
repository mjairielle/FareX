package com.farex.ui;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.TextAlignment;

// A result pop-up shown over the current screen instead of a new page:
// icon, title, sentence, then any panels/notes, a lime button and links.
// Every button closes the pop-up first, then runs its action.
// Example:
//   new ResultDialog(FetchUi.doubleCheck(), "Load fetched successfully", "Your card is updated.")
//       .add(panel).button("Back to Home", FetchLoadState::goHome).show();
public class ResultDialog extends StackPane {

    private final VBox box = new VBox(12);

    public ResultDialog(Node icon, String title, String text) {
        // Dim layer over the blurred screen; it also stops clicks reaching the screen below
        setBackground(Theme.solid(Color.rgb(0, 0, 0, 0.45)));

        Label titleLabel = TripUi.bold(title, 19);
        titleLabel.setTextAlignment(TextAlignment.CENTER);
        Label sentence = FetchUi.subtitle(text);
        sentence.setTextAlignment(TextAlignment.CENTER);

        VBox top = new VBox(10, icon, titleLabel, sentence);
        top.setAlignment(Pos.TOP_CENTER);
        VBox.setMargin(top, new Insets(0, 0, 4, 0));

        box.getChildren().add(top);
        box.setPadding(new Insets(22, 18, 18, 18));
        box.setMaxSize(316, VBox.USE_PREF_SIZE);
        box.setBackground(Theme.rounded(Color.web("#1E1E1E"), 14));
        getChildren().add(box);
    }

    // Panels, notes, anything between the sentence and the buttons
    public ResultDialog add(Node... nodes) {
        box.getChildren().addAll(nodes);
        return this;
    }

    public ResultDialog button(String text, Runnable action) {
        box.getChildren().add(FetchUi.limeButton(text, closeThen(action)));
        return this;
    }

    public ResultDialog link(String text, Runnable action) {
        box.getChildren().add(FetchUi.link(text, closeThen(action)));
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
