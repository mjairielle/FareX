package com.farex.ui.operator;

import java.util.Locale;

import javafx.geometry.Insets;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;

import com.farex.ui.common.CommonUi;
import com.farex.ui.common.Navigator;
import com.farex.ui.common.Theme;
import com.farex.ui.common.WarningDialog;
import com.farex.ui.trip.TripUi;

// The pop-up after a tap (Operator / O3 to O6), drawn on top of Ready Mode.
// Done / Back to shift closes it; Try again closes it and runs tryAgain (opens the tap list again).
public class TapResultDialog extends javafx.scene.layout.StackPane {

    private static final Color INNER = Color.web("#141414");

    private final Runnable tryAgain;

    public TapResultDialog(TapResult r, Runnable tryAgain) {
        this.tryAgain = tryAgain;
        // Dim layer over the screen; it also stops clicks reaching the screen below
        setBackground(Theme.solid(Color.rgb(0, 0, 0, 0.45)));

        VBox box = new VBox(10);
        box.setPadding(new Insets(20, 18, 18, 18));
        box.setMaxSize(300, VBox.USE_PREF_SIZE);
        box.setBackground(Theme.rounded(Color.web("#1E1E1E"), 14));
        box.getChildren().addAll(content(r));
        getChildren().add(box);
    }

    private Node[] content(TapResult r) {
        switch (r.kind) {
            case TAP_IN:
            case APP_TAP_IN:
                return new Node[] {
                    eyebrow("TAP IN"), title("Welcome aboard"), small(CommonUi.doubleCheck(), 0.7),
                    inner(bold(riderLine(r), 12), lime("Balance: " + CommonUi.peso(r.balance), 16)),
                    bold("Boarded at " + r.stop, 13), text(r.note), done("Done")};
            case APP_OFFLINE:
                return new Node[] {
                    eyebrow("APP TAP · OFFLINE"), title("Boarding saved"),
                    inner(lime("Pending server confirmation", 13), bold(riderLine(r), 12),
                            text("Boarded at " + r.stop)),
                    bold("Balance unavailable offline", 14),
                    text("Fare and server-held balance will be confirmed after sync. No payment confirmed yet."),
                    done("Back to shift")};
            case TAP_OUT:
                return new Node[] {
                    eyebrow("TAP OUT"), title(r.estimated ? "Fare saved" : "Fare charged"),
                    lime(OperatorUi.peso(r.fareCents) + (r.estimated ? " est." : ""), 28),
                    fareBreakdown(r),
                    r.estimated ? text("Payment pending · balance not confirmed")
                            : bold("New balance: " + CommonUi.peso(r.balance), 15),
                    done("Done")};
            case MAX_FARE:
                VBox maxFare = new VBox(10, eyebrow("NO TAP OUT"), small(WarningDialog.warningIcon(), 0.45),
                        title("Maximum fare charged"), lime(OperatorUi.peso(r.fareCents), 28),
                        text("Passenger didn't tap out before the route ended."),
                        text(r.who + " · Saved with a max-fare flag"));
                if (r.othersCharged > 0) {
                    maxFare.getChildren().add(lime("+" + r.othersCharged + " more "
                            + (r.othersCharged == 1 ? "passenger" : "passengers") + " charged", 11));
                }
                maxFare.getChildren().add(done("Back to shift"));
                return new Node[] {maxFare};
            case INSUFFICIENT:
                return error("Insufficient balance",
                        "This card cannot cover the fare. Ask the passenger to top up or pay another way.");
            case UNREADABLE:
                return error("Card unreadable", "Couldn't read card. Tap again.");
            case INVALID_QR:
                return error("Invalid QR", "This QR code isn't valid. Ask the passenger to refresh it.");
            default:
                return error(r.title, r.text);
        }
    }

    // ----- Pieces -----

    // O5: "TAP NOT COMPLETED", warning sign, title, sentence, Try again, Back to shift
    private Node[] error(String title, String sentence) {
        return new Node[] {
            eyebrow("TAP NOT COMPLETED"), small(WarningDialog.warningIcon(), 0.45), title(title), text(sentence),
            CommonUi.limeButton("Try again", () -> {
                Navigator.closeOverlay();
                if (tryAgain != null) {
                    tryAgain.run();
                }
            }),
            CommonUi.link("Back to shift", Navigator::closeOverlay)};
    }

    // "4.2 km · Philcoa → Batasan / Base fare ₱15.60 / Student discount -₱2.60"
    private VBox fareBreakdown(TapResult r) {
        VBox box = inner(
                bold(String.format(Locale.US, "%.1f km · %s › %s", r.km, r.from, r.to), 11),
                text("Base fare " + OperatorUi.peso(r.baseCents)));
        long discount = r.baseCents - r.fareCents;
        if (discount > 0) {
            box.getChildren().add(lime("Student discount -" + OperatorUi.peso(discount), 11));
        }
        return box;
    }

    // "xPress Card · **** 6767" or "App user · APP-0821"
    private String riderLine(TapResult r) {
        return (r.app ? "App user · " : "xPress Card · ") + r.who;
    }

    private VBox inner(Node... children) {
        VBox box = new VBox(4, children);
        box.setPadding(new Insets(12));
        box.setBackground(Theme.rounded(INNER, 8));
        return box;
    }

    private Label eyebrow(String text) {
        return TripUi.eyebrow(text);
    }

    private Label title(String text) {
        return TripUi.bold(text, 20);
    }

    private Label bold(String text, double size) {
        return TripUi.bold(text, size);
    }

    private Label lime(String text, double size) {
        Label label = TripUi.bold(text, size);
        label.setTextFill(Theme.LIME);
        return label;
    }

    private Label text(String text) {
        Label label = CommonUi.small(text, Theme.WHITE);
        label.setWrapText(true);
        return label;
    }

    // A drawn icon made smaller; the outer Group makes the layout use the smaller size
    private Group small(Node icon, double scale) {
        icon.setScaleX(scale);
        icon.setScaleY(scale);
        return new Group(icon);
    }

    private Node done(String text) {
        Node button = CommonUi.limeButton(text, Navigator::closeOverlay);
        VBox.setMargin(button, new Insets(8, 0, 0, 0));
        return button;
    }
}
