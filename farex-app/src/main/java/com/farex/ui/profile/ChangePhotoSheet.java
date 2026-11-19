package com.farex.ui.profile;

import java.io.File;

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
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.stage.FileChooser;
import javafx.stage.Window;

import com.farex.ui.common.CommonUi;
import com.farex.ui.common.Navigator;
import com.farex.ui.common.Theme;
import com.farex.ui.data.MockData;
import com.farex.ui.trip.TripUi;

// Profile / 03: bottom sheet over the edit screen.
// Choose from gallery opens a real file picker; a laptop has no camera, so Take photo says so.
public class ChangePhotoSheet extends StackPane {

    private final Window window;
    private final Runnable onDone;
    private final Label message = CommonUi.small("", Theme.ERROR);

    // onDone runs after the sheet closes, e.g. the edit screen redraws with the new photo
    public ChangePhotoSheet(Window window, Runnable onDone) {
        this.window = window;
        this.onDone = onDone;
        setBackground(Theme.solid(Color.rgb(0, 0, 0, 0.55)));

        Region handle = new Region();
        handle.setMinSize(36, 4);
        handle.setMaxSize(36, 4);
        handle.setBackground(Theme.rounded(Theme.WHITE, 2));
        StackPane handleRow = new StackPane(handle);

        message.setVisible(false);
        message.managedProperty().bind(message.visibleProperty());

        Button cancel = new Button("Cancel");
        cancel.setFont(Theme.bold(13));
        cancel.setPadding(new Insets(10));
        cancel.setMaxWidth(Double.MAX_VALUE);
        Theme.paintButton(cancel, "#1E1E1E", "#262626", "#161616", 10, "white");
        cancel.setBorder(new Border(new BorderStroke(Color.web("#6A6A6A"),
                BorderStrokeStyle.SOLID, new CornerRadii(10), new BorderWidths(1))));
        cancel.setOnAction(e -> back());
        VBox.setMargin(cancel, new Insets(14, 0, 0, 0));

        VBox sheet = new VBox(6,
                handleRow,
                TripUi.bold("Change photo", 16),
                CommonUi.small("Choose how to update your profile photo.", Theme.WHITE),
                option("Take photo", Theme.WHITE, this::takePhoto),
                option("Choose from gallery", Theme.WHITE, this::chooseFromGallery),
                option("Remove photo", Theme.LIME, this::removePhoto),
                message,
                cancel);
        sheet.setPadding(new Insets(10, 18, 22, 18));
        sheet.setBackground(Theme.roundedTop(Color.web("#1E1E1E"), 20));
        sheet.setMaxHeight(VBox.USE_PREF_SIZE);
        StackPane.setAlignment(sheet, Pos.BOTTOM_CENTER);

        getChildren().add(sheet);
    }

    // "Take photo ›"
    private Node option(String text, Color color, Runnable onClick) {
        Label label = new Label(text);
        label.setTextFill(color);
        label.setFont(Theme.bold(12));
        Region fill = new Region();
        HBox.setHgrow(fill, Priority.ALWAYS);
        Label chevron = new Label("›");
        chevron.setTextFill(Theme.WHITE);
        chevron.setFont(Theme.bold(18));

        HBox row = new HBox(8, label, fill, chevron);
        row.setAlignment(Pos.CENTER_LEFT);
        row.setPadding(new Insets(10, 6, 10, 6));
        row.setCursor(Cursor.HAND);
        row.setOnMouseClicked(e -> onClick.run());
        return row;
    }

    private void takePhoto() {
        message.setText("No camera is available on this computer. Choose from gallery instead.");
        message.setVisible(true);
    }

    private void chooseFromGallery() {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Choose a profile photo");
        chooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("Images", "*.png", "*.jpg", "*.jpeg"));
        File file = chooser.showOpenDialog(window);
        if (file != null) {
            MockData.getCurrentAccount().setPhotoUrl(file.toURI().toString());
            back();
        }
    }

    private void removePhoto() {
        MockData.getCurrentAccount().setPhotoUrl(null);
        back();
    }

    // Close the sheet; the edit screen redraws so the new photo shows (typed text is kept as a draft)
    private void back() {
        Navigator.closeOverlay();
        onDone.run();
    }
}
