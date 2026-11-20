package com.farex.ui.trip;

import javafx.scene.Cursor;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;

import com.farex.ui.common.CheckRing;
import com.farex.ui.common.CommonUi;
import com.farex.ui.common.Navigator;
import com.farex.ui.common.PopupDialog;
import com.farex.ui.common.Theme;
import com.farex.ui.common.WarningDialog;

// The Trip Planner's pop-ups (shown over TripScreen instead of opening pages).
// Each one takes what its buttons should do, e.g. noRoute(tryAgain, editLocations).
public final class TripDialogs {

    private TripDialogs() {
    }

    // 13: ask for location before tracking ("Not now" stays on the route)
    public static void enableLocation(Runnable allow) {
        new PopupDialog(TripUi.steeringWheel(), "Use location while traveling",
                "Your device location helps show trip progress and possible route deviations. "
                        + "You can still view route details without tracking.")
                .add(lime("Tracking stops when you end your trip."), TripUi.routeCard())
                .button("Allow location", allow)
                .link("Not now", () -> { })
                .show();
    }

    // 14: location was not allowed; the route can still be used without tracking
    public static void trackingUnavailable(Runnable enable) {
        new PopupDialog(warning(), "Tracking unavailable",
                "To track a trip, enable device location and allow location access while using FareX.")
                .add(lime("Your selected route is still available."))
                .button("Enable location", enable)
                .link("Return to route", () -> { })
                .show();
    }

    // 16: off-route warning while tracking
    public static void offRoute(Runnable endAndReplan) {
        new PopupDialog(warning(), "You may be off route",
                "GPS can be inaccurate. Check your surroundings and confirm the route with the driver. "
                        + "Do not use your phone while crossing.")
                .add(lime("Stay safe. Replan once you’re in a safe place."))
                .button("Continue tracking", () -> { })
                .link("End trip & replan", endAndReplan)
                .show();
    }

    // 17: confirm ending the trip before reaching the destination
    public static void endTrip(Runnable end) {
        new PopupDialog(warning(), "End this trip?",
                "Location tracking will stop. Not at your destination yet? Choose Keep tracking.")
                .add(TripUi.routeCard())
                .button("End trip", end)
                .link("Keep tracking", () -> { })
                .show();
    }

    // 18: trip complete
    public static void arrived(Runnable done) {
        new PopupDialog(new CheckRing(70, Theme.DARK), "You’ve arrived!",
                "Thanks for riding with FareX. Location tracking is now off.")
                .add(lime(TripState.summary()),
                        CommonUi.subtitle("Fares here are estimates. You pay when you tap your card on the jeep."))
                .button("Plan another trip", done)
                .show();
    }

    // A short lime line that wraps instead of being cut off
    private static Label lime(String text) {
        Label label = CommonUi.smallBold(text, Theme.LIME);
        label.setWrapText(true);
        return label;
    }

    // The lime warning triangle, a bit smaller for pop-ups
    private static Group warning() {
        Group icon = WarningDialog.warningIcon();
        icon.setScaleX(0.75);
        icon.setScaleY(0.75);
        return new Group(icon);
    }
}
