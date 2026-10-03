package com.farex.ui;

import javafx.animation.Animation;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.Border;
import javafx.scene.layout.BorderStroke;
import javafx.scene.layout.BorderStrokeStyle;
import javafx.scene.layout.BorderWidths;
import javafx.scene.layout.CornerRadii;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.util.Duration;

/**
 * Figma "Verification" screen: 6 digit boxes, 5:00 countdown, Resend code.
 * Demo code is 123456 (no real email is sent yet).
 */
public class OtpView extends VBox {

    private static final String DEMO_CODE = "123456";
    private static final int START_SECONDS = 5;

    private static final Border IDLE_BORDER = new Border(new BorderStroke(
            Color.web("#333333"), BorderStrokeStyle.SOLID, new CornerRadii(8), new BorderWidths(1)));
    private static final Border FILLED_BORDER = new Border(new BorderStroke(
            Theme.LIME, BorderStrokeStyle.SOLID, new CornerRadii(8), new BorderWidths(2)));

    private final TextField[] boxes = new TextField[6];
    private final Label timerLabel = new Label();
    private final Label errorLabel = new Label();
    private int secondsLeft = START_SECONDS;
    private final Timeline timer = new Timeline(new KeyFrame(Duration.seconds(1), e -> tick()));

    public OtpView() {
        setBackground(Theme.solid(Theme.DARK));
        setAlignment(Pos.CENTER);
        setSpacing(16);
        setPadding(new Insets(28));

        Label title = Theme.headline("Enter OTP", Theme.WHITE);
        VBox.setMargin(title, new Insets(0, 0, 12, 0));

        Label sentLabel = Theme.bodyText("A verification code has been sent to", Theme.WHITE);

        String contact = MockData.getPendingContact().isEmpty()
                ? MockData.DEMO_CONTACT : MockData.getPendingContact();
        Label contactLabel = new Label(contact);
        contactLabel.setTextFill(Theme.WHITE);
        contactLabel.setFont(Theme.bold(14));

        timerLabel.setTextFill(Theme.WHITE);
        timerLabel.setFont(Theme.bold(14));
        timerLabel.setText(formatTime(secondsLeft));

        HBox boxRow = new HBox(8);
        boxRow.setAlignment(Pos.CENTER);
        for (int i = 0; i < boxes.length; i++) {
            boxes[i] = createBox(i);
            boxRow.getChildren().add(boxes[i]);
        }

        errorLabel.setTextFill(Theme.ERROR);
        errorLabel.setWrapText(true);
        errorLabel.setVisible(false);
        errorLabel.managedProperty().bind(errorLabel.visibleProperty());

        Label didntGet = Theme.bodyText("Didn't receive the code?", Theme.WHITE);
        Label resend = new Label("Resend code");
        resend.setTextFill(Theme.LIME);
        resend.setFont(Theme.bold(14));
        resend.setCursor(Cursor.HAND);
        resend.setOnMouseClicked(e -> resendCode());

        getChildren().addAll(title, sentLabel, contactLabel, timerLabel,
                boxRow, errorLabel, didntGet, resend);

        timer.setCycleCount(Animation.INDEFINITE);
        timer.play();
        Platform.runLater(() -> boxes[0].requestFocus());
    }

    // One digit box. index = its position (0 to 5)
    private TextField createBox(int index) {
        TextField box = new TextField();
        box.setPrefSize(44, 52);
        box.setMinSize(44, 52);
        box.setMaxSize(44, 52);
        box.setPadding(Insets.EMPTY);
        box.setAlignment(Pos.CENTER);
        box.setBackground(Theme.rounded(Theme.FIELD, 8));
        box.setFont(Theme.bold(20));
        box.setStyle("-fx-text-fill: white;");   // same reason as in Theme.inputField
        box.setBorder(IDLE_BORDER);

        box.textProperty().addListener((obs, oldText, newText) -> {
            if (!newText.matches("\\d*")) {          // digits only
                box.setText(oldText);
                return;
            }
            if (newText.length() > 1) {              // typed over a filled box: keep the new digit
                box.setText(newText.substring(newText.length() - 1));
                return;
            }
            box.setBorder(newText.isEmpty() ? IDLE_BORDER : FILLED_BORDER);
            if (newText.length() == 1 && index < boxes.length - 1) {
                boxes[index + 1].requestFocus();     // jump to the next box
            }
            checkCode();
        });

        box.setOnKeyPressed(e -> {
            if (e.getCode() == KeyCode.BACK_SPACE && box.getText().isEmpty() && index > 0) {
                boxes[index - 1].requestFocus();     // Backspace on an empty box: go back and clear
                boxes[index - 1].clear();
            }
        });
        return box;
    }

    private String enteredCode() {
        StringBuilder code = new StringBuilder();
        for (TextField box : boxes) {
            code.append(box.getText());
        }
        return code.toString();
    }

    // Runs after every change; only acts once all 6 digits are in
    private void checkCode() {
        String code = enteredCode();
        if (code.length() < boxes.length) {
            return;
        }
        if (secondsLeft <= 0) {
            showError("Code expired. Tap Resend code.");
        } else if (code.equals(DEMO_CODE)) {
            timer.stop();
            Navigator.go("accountType");
        } else {
            showError("Wrong code. Try again.");
            clearBoxes();
        }
    }

    private void clearBoxes() {
        for (TextField box : boxes) {
            box.clear();
        }
        boxes[0].requestFocus();
    }

    private void resendCode() {
        secondsLeft = START_SECONDS;
        timerLabel.setText(formatTime(secondsLeft));
        errorLabel.setVisible(false);
        clearBoxes();
        timer.playFromStart();
    }

    private void tick() {
        secondsLeft--;
        timerLabel.setText(formatTime(secondsLeft));
        if (secondsLeft <= 0) {
            timer.stop();
            showError("Code expired. Tap Resend code.");
        }
    }

    private void showError(String message) {
        errorLabel.setText(message);
        errorLabel.setVisible(true);
    }

    // 300 -> "05:00", 65 -> "01:05"
    private String formatTime(int seconds) {
        return String.format("%02d:%02d", seconds / 60, seconds % 60);
    }
}
