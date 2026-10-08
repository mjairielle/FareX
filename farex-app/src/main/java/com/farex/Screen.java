package com.farex;

import farex.shared.txn.Txn;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;

/** Parent of every screen. Shared look and helpers live here, children only add content. */
public abstract class Screen extends VBox {
    protected final Label error = new Label();

    protected Screen() {
        super(12);
        setPadding(new Insets(24));
        setStyle("-fx-background-color: #111111;");
        error.setStyle("-fx-text-fill: #ff6b6b;");
        error.setWrapText(true);
    }

    protected static String peso(long centavos) {
        return String.format("P%.2f", centavos / 100.0);
    }

    protected Label heading(String t) {
        Label l = new Label(t);
        l.setStyle("-fx-text-fill: white; -fx-font-size: 22; -fx-font-weight: bold;");
        l.setWrapText(true);
        return l;
    }

    protected Label text(String t) {
        Label l = new Label(t);
        l.setStyle("-fx-text-fill: #cccccc; -fx-font-size: 13;");
        l.setWrapText(true);
        return l;
    }

    protected Label lime(String t, int size) {
        Label l = new Label(t);
        l.setStyle("-fx-text-fill: #c8ff00; -fx-font-weight: bold; -fx-font-size: " + size + ";");
        l.setWrapText(true);
        return l;
    }

    protected TextField field(String prompt) {
        TextField f = new TextField();
        f.setPromptText(prompt);
        f.setStyle("-fx-control-inner-background: #222222; -fx-text-fill: white; -fx-prompt-text-fill: #888888; -fx-padding: 10;");
        return f;
    }

    protected PasswordField password(String prompt) {
        PasswordField f = new PasswordField();
        f.setPromptText(prompt);
        f.setStyle("-fx-control-inner-background: #222222; -fx-text-fill: white; -fx-prompt-text-fill: #888888; -fx-padding: 10;");
        return f;
    }

    protected <T> ComboBox<T> combo() {
        ComboBox<T> c = new ComboBox<>();
        c.setMaxWidth(Double.MAX_VALUE);
        c.setStyle("-fx-base: #222222;");
        return c;
    }

    protected Button primary(String t, Runnable action) {
        return button(t, action, "-fx-background-color: #c8ff00; -fx-text-fill: black; -fx-font-weight: bold;");
    }

    protected Button secondary(String t, Runnable action) {
        return button(t, action, "-fx-background-color: #2a2a2a; -fx-text-fill: #c8ff00; -fx-font-weight: bold;");
    }

    protected Button link(String t, Runnable action) {
        Button b = button(t, action, "-fx-background-color: transparent; -fx-text-fill: #c8ff00;");
        b.setMaxWidth(Double.MAX_VALUE);
        return b;
    }

    protected Button back(Runnable action) {
        Button b = button("< Back", action, "-fx-background-color: transparent; -fx-text-fill: #c8ff00;");
        b.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
        return b;
    }

    protected void logout() {
        Session.user = null;
        Nav.show(new LoginScreen());
    }

    /** Receipt line shown in lists. */
    protected VBox receiptRow(Txn t) {
        java.time.format.DateTimeFormatter f = java.time.format.DateTimeFormatter
                .ofPattern("MMM d, h:mm a").withZone(java.time.ZoneId.systemDefault());
        Label main = new Label(t.describe());
        main.setStyle("-fx-text-fill: white; -fx-font-weight: bold;");
        Label sub = new Label(f.format(t.getTime()) + "  |  Balance " + peso(t.getBalanceAfter()));
        sub.setStyle("-fx-text-fill: #999999; -fx-font-size: 11;");
        VBox row = new VBox(2, main, sub);
        row.setStyle("-fx-background-color: #1c1c1c; -fx-padding: 10;");
        return row;
    }

    /** Any error thrown by an action shows in the red label instead of crashing. */
    private Button button(String t, Runnable action, String style) {
        Button b = new Button(t);
        b.setMaxWidth(Double.MAX_VALUE);
        b.setStyle(style + " -fx-padding: 10;");
        b.setOnAction(e -> {
            error.setStyle("-fx-text-fill: #ff6b6b;");
            error.setText("");
            try {
                action.run();
            } catch (RuntimeException ex) {
                error.setText(ex.getMessage());
            }
        });
        return b;
    }
}
