package com.farex.ui;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;

// Profile / 01: name, email and phone (read-only), with an Edit button
public class AccountInfoView extends SubScreen {

    public AccountInfoView() {
        super("Account Information", "profile");

        DemoAccount account = MockData.getCurrentAccount();

        VBox heading = new VBox(6,
                FetchUi.title("Account information"),
                FetchUi.subtitle("Your personal details, all in one place."));

        Label name = TripUi.bold(account.getFullName(), 15);
        VBox photo = new VBox(12, new Avatar(account, 80), name);
        photo.setAlignment(Pos.CENTER);
        VBox.setMargin(photo, new Insets(8, 0, 8, 0));

        Label privateNote = FetchUi.small("Only you can see your contact details.", Theme.WHITE);
        privateNote.setMaxWidth(Double.MAX_VALUE);
        privateNote.setAlignment(Pos.CENTER);

        body.setSpacing(12);
        body.getChildren().addAll(
                heading,
                photo,
                new LabeledField("Full name", account.getFullName()),
                new LabeledField("Email address", orDash(account.getEmail())),
                new LabeledField("Phone number", orDash(account.getPhone())),
                FetchUi.spacer(),
                FetchUi.limeButton("Edit", () -> {
                    ProfileState.clearDraft();
                    Navigator.go("accountEdit");
                }),
                privateNote);
    }

    private String orDash(String value) {
        return value.isEmpty() ? "—" : value;
    }
}
