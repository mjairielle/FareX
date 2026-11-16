package com.farex.ui;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.Border;
import javafx.scene.layout.BorderStroke;
import javafx.scene.layout.BorderStrokeStyle;
import javafx.scene.layout.BorderWidths;
import javafx.scene.layout.CornerRadii;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.TextAlignment;

// Profile / 12: "Log out of FareX?"
public class LogoutDialog extends StackPane {

    public LogoutDialog() {
        setBackground(Theme.solid(Color.rgb(0, 0, 0, 0.45)));

        Label title = new Label("Log out of FareX?");
        title.setTextFill(Theme.WHITE);
        title.setFont(Theme.bold(18));

        Label text = FetchUi.small("You can log back in anytime to access your account.", Theme.WHITE);
        text.setWrapText(true);
        text.setTextAlignment(TextAlignment.CENTER);

        Button logOut = new Button("Log out");
        logOut.setFont(Theme.bold(13));
        logOut.setPadding(new Insets(10));
        logOut.setMaxWidth(Double.MAX_VALUE);
        Theme.paintButton(logOut, "#CAFE06", "#D8FF3A", "#A9D400", 8, "black");
        logOut.setOnAction(e -> {
            MockData.logout();
            leaveAccount();
        });
        VBox.setMargin(logOut, new Insets(6, 0, 0, 0));

        Button cancel = new Button("Cancel");
        cancel.setFont(Theme.bold(13));
        cancel.setPadding(new Insets(10));
        cancel.setMaxWidth(Double.MAX_VALUE);
        Theme.paintButton(cancel, "#141414", "#1E1E1E", "#0E0E0E", 8, "white");
        cancel.setBorder(new Border(new BorderStroke(Color.web("#4A4A4A"),
                BorderStrokeStyle.SOLID, new CornerRadii(8), new BorderWidths(1))));
        cancel.setOnAction(e -> Navigator.closeOverlay());

        VBox box = new VBox(10, WarningDialog.warningIcon(), title, text, logOut, cancel);
        box.setAlignment(Pos.CENTER);
        box.setPadding(new Insets(20, 16, 16, 16));
        box.setMaxSize(250, VBox.USE_PREF_SIZE);
        box.setBackground(Theme.rounded(Color.web("#1C1C1C"), 10));
        box.setBorder(new Border(new BorderStroke(Theme.LIME,
                BorderStrokeStyle.SOLID, new CornerRadii(10), new BorderWidths(1.2))));

        getChildren().add(box);
    }

    // After log out (or delete account): forget every in-progress flow, then go to Welcome
    public static void leaveAccount() {
        BuyLoadState.clear();
        FetchLoadState.clear();
        RedeemState.clear();
        LostCardState.clear();
        TripState.clear();
        Navigator.go("welcome");
    }
}
