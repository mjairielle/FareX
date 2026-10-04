package com.farex.ui;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.Node;
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
import javafx.scene.paint.Color;
import javafx.scene.text.Font;

/**
 * Dark input box with an icon on the left (Figma Login / Register fields).
 * IconField.text(...)     -> user icon
 * IconField.password(...) -> lock icon, plus an eye icon that shows/hides the password
 */
public class IconField extends HBox {

    private static final Border IDLE_BORDER = new Border(new BorderStroke(
            Color.web("#333333"), BorderStrokeStyle.SOLID, new CornerRadii(10), new BorderWidths(1.5)));
    private static final Border FOCUS_BORDER = new Border(new BorderStroke(
            Theme.LIME, BorderStrokeStyle.SOLID, new CornerRadii(10), new BorderWidths(1.5)));

    private final TextField field;      // the real field: getText() always works on this one
    private TextField shownField;       // password only: plain-text twin shown when the eye is on
    private StackPane eyeHolder;
    private boolean showing = false;

    public static IconField text(String prompt) {
        return new IconField(prompt, "icon_user", false);
    }

    public static IconField password(String prompt) {
        return new IconField(prompt, "icon_lock", true);
    }

    private IconField(String prompt, String iconName, boolean isPassword) {
        setAlignment(Pos.CENTER_LEFT);
        setSpacing(12);
        setPadding(new Insets(0, 14, 0, 14));
        setBackground(Theme.rounded(Theme.FIELD, 10));
        setBorder(IDLE_BORDER);

        field = isPassword ? new PasswordField() : new TextField();
        styleInner(field, prompt);
        getChildren().addAll(Icons.view(iconName, 20), field);

        if (isPassword) {
            addEyeToggle(prompt);
        }

        // Lime outline around the whole box while typing in it
        field.focusedProperty().addListener((obs, was, now) -> updateBorder());
        if (shownField != null) {
            shownField.focusedProperty().addListener((obs, was, now) -> updateBorder());
        }
    }

    /** The field to read with getText() and to attach setOnAction(...) to. */
    public TextField getField() {
        return field;
    }

    /** Puts the cursor in this box (works whether the password is shown or hidden). */
    public void focusField() {
        if (showing) {
            shownField.requestFocus();
        } else {
            field.requestFocus();
        }
    }

    // Transparent text field inside the dark box, white text (same inline-style reason as Theme.inputField)
    private void styleInner(TextField inner, String prompt) {
        inner.setPromptText(prompt);
        inner.setBackground(Background.EMPTY);
        inner.setBorder(null);
        inner.setPadding(new Insets(14, 0, 14, 0));
        inner.setFont(Font.font("System", 14));
        inner.setStyle("-fx-text-fill: white; -fx-prompt-text-fill: #9A9A9A;");
        HBox.setHgrow(inner, Priority.ALWAYS);
    }

    private void addEyeToggle(String prompt) {
        shownField = new TextField();
        styleInner(shownField, prompt);
        shownField.textProperty().bindBidirectional(field.textProperty());   // same text in both
        shownField.setVisible(false);
        shownField.managedProperty().bind(shownField.visibleProperty());
        field.managedProperty().bind(field.visibleProperty());

        // Pressing Enter in the plain-text twin does the same as Enter in the password field
        shownField.setOnAction(e -> {
            if (field.getOnAction() != null) {
                field.getOnAction().handle(e);
            }
        });

        eyeHolder = new StackPane(Icons.view("icon_eye_off", 20));
        eyeHolder.setCursor(Cursor.HAND);
        eyeHolder.setOnMouseClicked(e -> toggleEye());

        getChildren().addAll(shownField, eyeHolder);
    }

    private void toggleEye() {
        showing = !showing;
        shownField.setVisible(showing);
        field.setVisible(!showing);

        // Crossed-out eye = hidden. When shown, use icon_eye if you have it, else a dimmed crossed-out eye.
        Node eye;
        if (!showing) {
            eye = Icons.view("icon_eye_off", 20);
        } else if (Icons.exists("icon_eye")) {
            eye = Icons.view("icon_eye", 20);
        } else {
            eye = Icons.view("icon_eye_off", 20);
            eye.setOpacity(0.5);
        }
        eyeHolder.getChildren().setAll(eye);

        TextField active = showing ? shownField : field;
        active.requestFocus();
        active.positionCaret(active.getLength());
    }

    private void updateBorder() {
        boolean focused = field.isFocused() || (shownField != null && shownField.isFocused());
        setBorder(focused ? FOCUS_BORDER : IDLE_BORDER);
    }
}
