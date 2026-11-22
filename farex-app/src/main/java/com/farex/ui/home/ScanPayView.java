package com.farex.ui.home;

import java.util.Locale;
import java.util.Optional;

import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.geometry.Insets;
import javafx.scene.Cursor;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;

import com.farex.Api;
import com.farex.ui.common.CardPickerDialog;
import com.farex.ui.common.CommonUi;
import com.farex.ui.common.Navigator;
import com.farex.ui.common.PopupDialog;
import com.farex.ui.common.SubScreen;
import com.farex.ui.common.Theme;
import com.farex.ui.common.WarningDialog;
import com.farex.ui.data.DemoAccount;
import com.farex.ui.data.DemoCard;
import com.farex.ui.data.MockData;
import com.gluonhq.attach.barcodescan.BarcodeScanService;

// Scan to Pay (from Home): the rider scans the QR on the jeepney's operator phone with one of their cards.
// First scan = get on at the jeep's stop, second scan = get off there and pay, like tapping a card in and out.
// Phone: the camera scanner. Laptop (no camera service): paste the text printed under the jeep's QR.
public class ScanPayView extends SubScreen {

    private DemoCard card;
    private final VBox cardPanel = new VBox();
    private final Label status = CommonUi.small("", Theme.GRAY);
    private boolean busy;

    public ScanPayView() {
        super("Scan to Pay", "dashboard");
        DemoAccount account = MockData.getCurrentAccount();
        card = account == null ? null : account.getPrimaryCard();
        status.setWrapText(true);

        VBox heading = new VBox(8, CommonUi.title("Scan the jeepney's QR"),
                CommonUi.subtitle("Scan when you get on, and scan the same QR again when you get off. "
                        + "The fare is taken from your card when you get off."));
        VBox.setMargin(heading, new Insets(0, 0, 6, 0));
        body.getChildren().addAll(heading, cardPanel);
        drawCard();

        Optional<BarcodeScanService> scanner = BarcodeScanService.create();
        if (scanner.isPresent()) {
            // asyncScan + the result property: the blocking scan() only works on the UI thread (it runs a nested
            // event loop), and called from another thread it returned nothing while the camera was still open.
            // asyncScan clears the result first, so scanning the same jeep again (to get off) still fires.
            ChangeListener<String> onCode = (obs, old, code) -> {
                if (code != null && !code.isBlank()) {
                    pay(code);
                }
            };
            scanner.get().resultProperty().addListener(onCode);
            sceneProperty().addListener((obs, was, now) -> {
                if (now == null) {
                    scanner.get().resultProperty().removeListener(onCode);   // left the screen: no stray payments
                }
            });
            body.getChildren().addAll(status, CommonUi.spacer(),
                    CommonUi.limeButton("Scan QR", () -> {
                        if (!busy) {
                            scanner.get().asyncScan("Scan the jeepney QR", "Point the camera at the QR on the operator's phone", "");
                        }
                    }));
        } else {
            TextField pasted = new TextField();
            Theme.inputField(pasted, "FAREX:JEEP:...");
            body.getChildren().addAll(
                    CommonUi.note("No camera here. Type or paste the code printed under the jeepney's QR."),
                    pasted, status, CommonUi.spacer(),
                    CommonUi.limeButton("Pay", () -> pay(pasted.getText())));
            pasted.setOnAction(e -> pay(pasted.getText()));
        }
    }

    // "Pay with  xPress •••• 6767   ₱468.25   Change"
    private void drawCard() {
        if (card == null) {
            cardPanel.getChildren().setAll(CommonUi.note("Add an xPress card first: the fare is paid from a card."));
            return;
        }
        Label change = CommonUi.smallBold("Change", Theme.LIME);
        change.setCursor(Cursor.HAND);
        change.setOnMouseClicked(e -> Navigator.showOverlay(new CardPickerDialog(card, picked -> {
            card = picked;
            drawCard();
        })));
        cardPanel.getChildren().setAll(CommonUi.panel(
                CommonUi.row("Pay with", change),
                CommonUi.row("xPress " + CommonUi.masked(card), CommonUi.peso(card.getBalance()))));
    }

    private void pay(String qr) {
        if (busy || qr == null || qr.isBlank()) {
            return;
        }
        if (card == null) {
            fail("Add an xPress card first.");
            return;
        }
        if (!MockData.isLive()) {
            fail("Scan to Pay needs the FareX server. Log in with a server account.");
            return;
        }
        busy = true;
        status.setText("Paying…");
        String uid = card.getCardNumber();
        Thread t = new Thread(() -> {
            String[] r;
            try {
                r = Api.qrPay(qr.trim(), uid);
            } catch (Api.ApiException e) {
                Platform.runLater(() -> {
                    busy = false;
                    status.setText("");
                    fail(e.getMessage());
                });
                return;
            }
            Platform.runLater(() -> {
                busy = false;
                status.setText("");
                done(r);
            });
        }, "farex-qr-pay");
        t.setDaemon(true);
        t.start();
    }

    // r = {action, stop, plate, fare cents, balance cents}
    private void done(String[] r) {
        double balance = Long.parseLong(r[4]) / 100.0;
        card.setBalance(balance);
        drawCard();
        if ("in".equals(r[0])) {
            new PopupDialog(CommonUi.doubleCheck(), "You're on board",
                    "Jeepney " + r[2] + " at " + r[1] + ". Scan the same QR again when you get off.")
                    .button("OK", () -> { })
                    .show();
        } else {
            double fare = Long.parseLong(r[3]) / 100.0;
            new PopupDialog(CommonUi.doubleCheck(), "Fare paid",
                    String.format(Locale.US, "%s for your ride to %s on jeepney %s.", CommonUi.peso(fare), r[1], r[2]))
                    .add(CommonUi.panel(CommonUi.row("New balance", CommonUi.peso(balance))))
                    .button("Back to Home", () -> Navigator.go("dashboard"))
                    .show();
        }
        MockData.refreshInBackground(() -> { });   // history and points catch up
    }

    private void fail(String message) {
        new PopupDialog(WarningDialog.warningIcon(), "Couldn’t pay", message).button("OK", () -> { }).show();
    }
}
