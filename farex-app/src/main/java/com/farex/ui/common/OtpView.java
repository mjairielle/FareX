package com.farex.ui.common;

import javafx.animation.Animation;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.Border;
import javafx.scene.layout.BorderStroke;
import javafx.scene.layout.BorderStrokeStyle;
import javafx.scene.layout.BorderWidths;
import javafx.scene.layout.CornerRadii;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.util.Duration;

import com.farex.Api;
import com.farex.ui.data.MockData;

/**
 * Six digit boxes for an email code. The server sends the code and checks it (Api.sendOtp / verifyOtp).
 * One invisible field sits over the boxes and takes all typing, so the phone keyboard stays open
 * from the first digit to the last; the boxes only show its digits.
 */
public class OtpView extends VBox {

    private static final String DEMO_CODE = "123456";   // mock mode only (no server login)
    private static final int START_SECONDS = 300;

    private static final Border IDLE_BORDER = new Border(new BorderStroke(
            Color.web("#333333"), BorderStrokeStyle.SOLID, new CornerRadii(8), new BorderWidths(1)));
    private static final Border FILLED_BORDER = new Border(new BorderStroke(
            Theme.LIME, BorderStrokeStyle.SOLID, new CornerRadii(8), new BorderWidths(2)));

    private final Label[] boxes = new Label[6];
    private final TextField input = new TextField();
    protected String otpPass = "";       // one-time pass from the server for the next step (register, freeze)
    private final HBox boxRow = new HBox(8);
    private final Label timerLabel = new Label();
    private final Label errorLabel = new Label();
    private int secondsLeft = START_SECONDS;
    private final Timeline timer = new Timeline(new KeyFrame(Duration.seconds(1), e -> tick()));

    // Registration: the code goes to the email or number typed on the Register screen
    public OtpView() {
        this("A verification code has been sent to", registrationContact());
    }

    // sentText is the line above, sentTo the bold line, e.g. "We sent a code to your" + "GCash Account"
    protected OtpView(String sentText, String sentTo) {
        setBackground(Theme.solid(Theme.DARK));
        setAlignment(Pos.CENTER);
        setSpacing(16);
        setPadding(new Insets(28));

        Label title = Theme.headline("Enter OTP", Theme.WHITE);
        VBox.setMargin(title, new Insets(0, 0, 12, 0));

        Label sentLabel = Theme.bodyText(sentText, Theme.WHITE);

        Label contactLabel = new Label(sentTo);
        contactLabel.setTextFill(Theme.WHITE);
        contactLabel.setFont(Theme.bold(14));

        timerLabel.setTextFill(Theme.WHITE);
        timerLabel.setFont(Theme.bold(14));
        timerLabel.setText(formatTime(secondsLeft));

        boxRow.setAlignment(Pos.CENTER);
        HBox digits = new HBox();
        digits.spacingProperty().bind(boxRow.spacingProperty());
        digits.alignmentProperty().bind(boxRow.alignmentProperty());
        for (int i = 0; i < boxes.length; i++) {
            boxes[i] = createBox();
            digits.getChildren().add(boxes[i]);
        }
        input.setOpacity(0);                       // invisible, but it is what gets the taps and the keyboard
        input.setPrefHeight(52);
        input.setMaxWidth(Double.MAX_VALUE);
        input.textProperty().addListener((obs, oldText, text) -> {
            String clean = text.replaceAll("[^0-9]", "");
            if (clean.length() > boxes.length) clean = clean.substring(0, boxes.length);
            if (!clean.equals(text)) {
                input.setText(clean);
                return;
            }
            for (int i = 0; i < boxes.length; i++) {
                boolean filled = i < text.length();
                boxes[i].setText(filled ? String.valueOf(text.charAt(i)) : "");
                boxes[i].setBorder(filled ? FILLED_BORDER : IDLE_BORDER);
            }
            input.end();                           // a tap in the middle must not insert digits there
            if (text.length() == boxes.length) checkCode();
        });
        input.setOnMouseReleased(e -> input.end());
        boxRow.getChildren().add(new StackPane(digits, input));

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
        Platform.runLater(() -> {
            sendCode();
            input.requestFocus();
        });
    }

    // Mock mode only: there is no server to email a code, so the screen tells it
    protected static Label demoHint() {
        return Theme.bodyText("Demo: the code is " + DEMO_CODE, Theme.GRAY);
    }

    // ----- Pieces a child screen can place in its own layout (see LostOtpStep) -----

    protected HBox getCodeBoxes() {
        return boxRow;
    }

    protected Label getTimerLabel() {
        return timerLabel;
    }

    protected Label getErrorLabel() {
        return errorLabel;
    }

    // One digit box: shows a digit of the invisible field
    private Label createBox() {
        Label box = new Label();
        box.setPrefSize(44, 52);
        box.setMinSize(44, 52);
        box.setMaxSize(44, 52);
        box.setAlignment(Pos.CENTER);
        box.setBackground(Theme.rounded(Theme.FIELD, 8));
        box.setFont(Theme.bold(20));
        box.setTextFill(Theme.WHITE);
        box.setBorder(IDLE_BORDER);
        return box;
    }

    private String enteredCode() {
        return input.getText();
    }

    private void checkCode() {
        String code = enteredCode();
        if (code.length() < boxes.length) {
            return;
        }
        if (secondsLeft <= 0) {
            showError("Code expired. Tap Resend code.");
            return;
        }
        String error = verify(code);
        if (error == null) {
            timer.stop();
            onCodeAccepted();
        } else {
            showError(error);                      // e.g. "Wrong code. Try again."
            clearBoxes();
        }
    }

    // What the code is for and where it goes. Sign-up: the email typed on Register.
    protected String otpPurpose() {
        return "SIGNUP";
    }

    protected String otpEmail() {
        return MockData.getPendingContact();
    }

    // Asks the server to email a code (it prints it in its window when no email account is set up)
    protected void sendCode() {
        try {
            Api.sendOtp(otpPurpose(), otpEmail());
        } catch (Api.ApiException e) {
            showError(e.getMessage());             // e.g. "Wait 30 seconds before asking for another code"
        }
    }

    // The server checks the code. null = right (and otpPass is set), else why not.
    protected String verify(String code) {
        try {
            otpPass = Api.verifyOtp(otpPurpose(), otpEmail(), code);
            return null;
        } catch (Api.ApiException e) {
            return e.getMessage();
        }
    }

    // Mock mode (no server login): the old fixed code
    protected static String verifyDemo(String code) {
        return code.equals(DEMO_CODE) ? null : "Wrong code. Try again.";
    }

    // For a "Verify" button: checks now, or asks for the missing digits ("1234" -> error)
    protected void verifyNow() {
        if (enteredCode().length() < boxes.length) {
            showError("Enter all 6 digits of the code.");
        } else {
            checkCode();
        }
    }

    // What happens after the right code. Registration goes on to "Your details", then Account Type;
    // LostCardScreen's code step overrides this.
    protected void onCodeAccepted() {
        MockData.setPendingOtp(otpPass);       // sign-up sends it with the new account
        Navigator.go("signUpDetails");
    }

    protected void stopTimer() {
        timer.stop();
    }

    private static String registrationContact() {
        return MockData.getPendingContact().isEmpty()
                ? MockData.DEMO_CONTACT : MockData.getPendingContact();
    }

    private void clearBoxes() {
        input.clear();
        input.requestFocus();
    }

    protected void resendCode() {
        secondsLeft = START_SECONDS;
        timerLabel.setText(formatTime(secondsLeft));
        errorLabel.setVisible(false);
        clearBoxes();
        timer.playFromStart();
        sendCode();
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

    private String formatTime(int seconds) {
        return String.format("%02d:%02d", seconds / 60, seconds % 60);
    }
}
