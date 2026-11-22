package com.farex.ui.operator;

import javafx.geometry.Insets;
import javafx.scene.Group;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;

import com.farex.ui.common.Loading;
import com.farex.ui.common.CommonUi;
import com.farex.ui.common.Navigator;
import com.farex.ui.common.PopupDialog;
import com.farex.ui.common.Theme;
import com.farex.ui.common.WarningDialog;
import com.farex.ui.trip.TripUi;

// Operator / O9 "End this shift?" as a pop-up over the current tab
// (from END SHIFT, or Exit Operator Mode during a shift).
// End shift -> max fares for anyone still on board -> Shift summary.
public final class EndShiftDialog {

    private EndShiftDialog() {
    }

    public static void show() {
        int pending = OperatorState.pendingRecords();

        Group warning = WarningDialog.warningIcon();
        warning.setScaleX(0.7);
        warning.setScaleY(0.7);

        PopupDialog dialog = new PopupDialog(new Group(warning), "End this shift?",
                "NFC and QR will stop. You can start a new shift anytime.")
                .add(syncPanel(pending))
                .button("End shift", () -> Loading.run("Ending shift…",
                        TapTerminal::chargeMaxFares,  // anyone still on board pays the max fare
                        () -> {
                            TapTerminal.takeMaxFareResult();  // counted in the summary; no pop-up
                            OperatorState.endShift();
                            Navigator.go("operatorSummary");
                        }))
                .link("Keep driving", () -> { });
        if (pending > 0) {
            dialog.link("Sync first", () -> Navigator.go("operatorSync"));
        }
        dialog.show();
    }

    // "12 records not yet synced / Sync before ending? / Records stay safely saved..."
    private static VBox syncPanel(int pending) {
        if (pending == 0) {
            return CommonUi.panel(TripUi.bold("All records synced", 14),
                    CommonUi.small("Nothing is waiting to be sent.", Theme.WHITE));
        }
        Label count = TripUi.bold(pending + (pending == 1 ? " record" : " records") + " not yet synced", 14);
        count.setTextFill(Theme.LIME);
        Label saved = CommonUi.small("Records stay safely saved on this device if you end now.", Theme.WHITE);
        saved.setWrapText(true);

        VBox panel = CommonUi.panel(count, TripUi.bold("Sync before ending?", 12), saved);
        panel.setSpacing(6);
        panel.setPadding(new Insets(14));
        return panel;
    }
}
