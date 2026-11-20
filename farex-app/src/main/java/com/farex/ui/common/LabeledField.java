package com.farex.ui.common;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.layout.Background;
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

// Gray box with a small caption on top and the value under it (Profile screens).
// Read-only:  new LabeledField("Full name", "Aron Ferrancullo")
// Editable:   LabeledField.editable("Full name", "Aron Ferrancullo")
// Password:   LabeledField.password("Current password")  (with the eye toggle)
public class LabeledField extends VBox {

    private static final Border IDLE = new Border(new BorderStroke(
            Color.TRANSPARENT, BorderStrokeStyle.SOLID, new CornerRadii(8), new BorderWidths(1.2)));
    private static final Border FOCUSED = new Border(new BorderStroke(
            Theme.LIME, BorderStrokeStyle.SOLID, new CornerRadii(8), new BorderWidths(1.2)));

    private final Label caption;
    private TextField field;
    private TextField shownField;      // plain copy of a password, shown when the eye is on
    private StackPane eyeHolder;
    private boolean showing = false;

    // ----- Read-only -----

    public LabeledField(String captionText, String value) {
        caption = CommonUi.small(captionText, Theme.WHITE);
        Label valueLabel = new Label(value);
        valueLabel.setTextFill(Theme.WHITE);
        valueLabel.setFont(javafx.scene.text.Font.font(Theme.FONT, 13));
        setUp(valueLabel);
    }

    // ----- Editable -----

    public static LabeledField editable(String captionText, String value) {
        LabeledField box = new LabeledField(captionText, new TextField(value));
        return box;
    }

    public static LabeledField password(String captionText) {
        LabeledField box = new LabeledField(captionText, new PasswordField());
        box.addEye();
        return box;
    }

    private LabeledField(String captionText, TextField input) {
        caption = CommonUi.small(captionText, Theme.LIME);
        field = input;
        style(field);
        HBox line = new HBox(8, field);
        line.setAlignment(Pos.CENTER_LEFT);
        setUp(line);
        field.focusedProperty().addListener((obs, was, now) -> updateBorder());
    }

    private void setUp(Node value) {
        getChildren().addAll(caption, value);
        setSpacing(6);
        setPadding(new Insets(12, 14, 14, 14));
        setBackground(Theme.rounded(CommonUi.PANEL, 8));
        setBorder(IDLE);
    }

    private void style(TextField input) {
        input.setBackground(Background.EMPTY);
        input.setBorder(null);
        input.setPadding(Insets.EMPTY);
        input.setFont(Theme.bold(13));
        input.setStyle("-fx-text-fill: white; -fx-prompt-text-fill: #8A8A8A;");
        HBox.setHgrow(input, Priority.ALWAYS);
    }

    public String getText() {
        return field.getText().trim();
    }

    public TextField getField() {
        return field;
    }

    // ----- Eye toggle (password) -----

    private void addEye() {
        HBox line = (HBox) getChildren().get(1);

        shownField = new TextField();
        style(shownField);
        shownField.textProperty().bindBidirectional(field.textProperty());
        shownField.setVisible(false);
        shownField.managedProperty().bind(shownField.visibleProperty());
        field.managedProperty().bind(field.visibleProperty());
        shownField.focusedProperty().addListener((obs, was, now) -> updateBorder());

        eyeHolder = new StackPane(Icons.view("icon_eye_off", 18));
        eyeHolder.setCursor(Cursor.HAND);
        eyeHolder.setOnMouseClicked(e -> toggleEye());

        line.getChildren().addAll(shownField, eyeHolder);
    }

    private void toggleEye() {
        showing = !showing;
        shownField.setVisible(showing);
        field.setVisible(!showing);
        eyeHolder.getChildren().setAll(Icons.view(showing ? "icon_eye" : "icon_eye_off", 18));
        TextField active = showing ? shownField : field;
        active.requestFocus();
        active.positionCaret(active.getLength());
    }

    // Lime outline while typing
    private void updateBorder() {
        boolean focused = field.isFocused() || (shownField != null && shownField.isFocused());
        setBorder(focused ? FOCUSED : IDLE);
    }
}
