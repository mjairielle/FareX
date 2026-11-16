package com.farex.ui;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;

// Lost Card / 03: the same OTP logic as registration (code 123456, 5-minute timer),
// placed in the Lost Card layout. The right code starts the freeze.
public class LostCardOtpView extends OtpView {

    public LostCardOtpView() {
        super("", "");

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

        VBox body = new VBox(12,
                heading,
                codePanel,
                getErrorLabel(),
                FetchUi.note("Never share this code. FareX support will not ask for your verification code."),
                FetchUi.spacer(),
                FetchUi.limeButton("Verify and submit report", this::verifyNow),
                FetchUi.link("Resend code", this::resendCode));
        body.setPadding(new Insets(28, 20, 24, 20));
        VBox.setVgrow(body, Priority.ALWAYS);

        // Replace OtpView's centered layout with the lime header + body
        setAlignment(Pos.TOP_LEFT);
        setSpacing(0);
        setPadding(Insets.EMPTY);
        getChildren().setAll(SubScreen.header("Replace or Report Lost Card", "lostConfirm"), body);
    }

    @Override
    protected void onCodeAccepted() {
        Navigator.go("lostProgress");
    }
}
