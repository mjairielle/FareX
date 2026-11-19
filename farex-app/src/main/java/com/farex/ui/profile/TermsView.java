package com.farex.ui.profile;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

import com.farex.ui.common.CommonUi;
import com.farex.ui.common.Navigator;
import com.farex.ui.common.SubScreen;
import com.farex.ui.common.Theme;
import com.farex.ui.trip.TripUi;

// Profile / 09: draft Terms of use and Privacy policy (scrolls if the text grows)
public class TermsView extends SubScreen {

    public TermsView() {
        super("Terms & Privacy Policy", "privacy");

        VBox content = new VBox(12,
                CommonUi.title("Terms & Privacy Policy"),
                CommonUi.subtitle("Understand your rights and how your data is used."),
                TripUi.eyebrow("DRAFT POLICY OVERVIEW"),
                section("Terms of use",
                        "Use FareX responsibly and keep your account details accurate. Keep your sign-in "
                                + "details secure and report any unauthorized activity.",
                        "Review your route, fare and payment details before confirming a transaction."),
                section("Privacy policy",
                        "Account and trip details help provide your travel experience. Location access and "
                                + "optional notifications are managed in your settings.",
                        "You can review your personal information, update your preferences or request account "
                                + "deletion from your profile."),
                CommonUi.small("Draft copy · Review legal terms before publishing.", Theme.WHITE));

        ScrollPane scroll = new ScrollPane(content);
        scroll.setFitToWidth(true);
        scroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scroll.setVbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scroll.setStyle("-fx-background: transparent; -fx-background-color: transparent;");
        VBox.setVgrow(scroll, Priority.ALWAYS);

        Label footer = CommonUi.small("Your choices. Your privacy.", Theme.WHITE);
        footer.setMaxWidth(Double.MAX_VALUE);
        footer.setAlignment(Pos.CENTER);

        body.setSpacing(12);
        body.getChildren().addAll(
                scroll,
                CommonUi.limeButton("Back to Privacy & Security", () -> Navigator.go("privacy")),
                footer);
    }

    private Node section(String title, String first, String second) {
        VBox panel = CommonUi.panel(TripUi.bold(title, 13), CommonUi.subtitle(first), CommonUi.subtitle(second));
        panel.setPadding(new Insets(16));
        return panel;
    }
}
