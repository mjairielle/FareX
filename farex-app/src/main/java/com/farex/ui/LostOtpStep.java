package com.farex.ui;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;

// Lost Card / 03 "Verify it’s you": the same OTP logic as registration (code 123456, 5-minute timer),
// placed in the Lost Card layout as a step of LostCardScreen. The right code runs onAccepted.
public class LostOtpStep extends OtpView {

    private final Runnable onAccepted;

    public LostOtpStep(Runnable onAccepted) {
        super("", "");
        this.onAccepted = onAccepted;

        DemoAccount account = MockData.getCurrentAccount();
        String email = (account == null) ? MockData.DEMO_CONTACT : account.getContact();

        // ----- Reuse OtpView's boxes, timer and error message in a new layout -----
        HBox boxes = getCodeBoxes();
        boxes.setSpacing(6);
        boxes.setAlignment(Pos.CENTER_LEFT);

        Label timer = getTimerLabel();
        timer.setFont(Font.font("System", 11));
        HBox expires = new HBox(4, FetchUi.small("Code expires in", Theme.WHITE), timer);
        expires.setAlignment(Pos.CENTER_LEFT);

        VBox codePanel = FetchUi.panel(FetchUi.small("Verification code", Theme.WHITE), boxes, expires);
        codePanel.setPadding(new Insets(14, 10, 14, 10));

        Label emailLabel = new Label(email);
        emailLabel.setTextFill(Theme.WHITE);
        emailLabel.setFont(Theme.bold(13));

        VBox heading = new VBox(8,
                FetchUi.title("Verify it’s you"),
                FetchUi.subtitle("Enter the 6-digit code sent to your registered email to authorize the card freeze."),
                emailLabel);

        // Replace OtpView's centered layout with this step's layout (the screen gives the lime header)
        setAlignment(Pos.TOP_LEFT);
        setSpacing(12);
        setPadding(Insets.EMPTY);
        setBackground(null);
        VBox.setVgrow(this, Priority.ALWAYS);
        getChildren().setAll(
                heading,
                codePanel,
                getErrorLabel(),
                FetchUi.note("Never share this code. FareX support will not ask for your verification code."),
                FetchUi.spacer(),
                FetchUi.limeButton("Verify and submit report", this::verifyNow),
                FetchUi.link("Resend code", this::resendCode));
    }

    @Override
    protected void onCodeAccepted() {
        onAccepted.run();
    }
}
