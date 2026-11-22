package com.farex.ui.load;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.scene.control.Label;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.util.Duration;

import com.farex.ui.common.CommonUi;
import com.farex.ui.common.Theme;

// Buy Load, live only (title "Payment"): after Confirm, until PayMongo says the payment went through.
// The rider pays on the PayMongo page; this screen checks every few seconds and on "Check payment".
// Only a confirmed payment goes on to the receipt and appears in Fetch Load. Leaving is safe:
// a payment confirmed later still appears in Fetch Load (Home asks the server for paid loads).
public class LoadWaitStep extends VBox {

    private final Runnable onPaid;
    private final Label status = CommonUi.small("Not confirmed yet. Finish paying on the PayMongo page.", Theme.GRAY);
    private final Timeline poll = new Timeline(new KeyFrame(Duration.seconds(4), e -> check()));
    private boolean checking;
    private boolean done;

    // onPaid runs once, on the UI thread, when the payment is confirmed
    public LoadWaitStep(Runnable onPaid, Runnable onLeave) {
        super(16);
        VBox.setVgrow(this, Priority.ALWAYS);
        this.onPaid = onPaid;
        status.setWrapText(true);

        VBox heading = new VBox(8, CommonUi.title("Waiting for your payment"),
                CommonUi.subtitle("Pay on the PayMongo page that opened. Your load is added only after "
                        + "PayMongo confirms the payment."));
        VBox.setMargin(heading, new Insets(0, 0, 6, 0));

        getChildren().addAll(
                heading,
                CommonUi.panel(
                        CommonUi.row("Amount", CommonUi.peso(BuyLoadState.getAmount())),
                        CommonUi.row("xPress card", CommonUi.masked(BuyLoadState.getCard())),
                        CommonUi.row("Payment method", BuyLoadState.getMethod()),
                        status),
                CommonUi.note("Paid already? It can take a moment to confirm. If you leave now, the load "
                        + "appears in Fetch Load once it is confirmed. Do not pay twice."),
                CommonUi.spacer(),
                CommonUi.limeButton("Check payment", this::check),
                CommonUi.link("Open the payment page again", LoadPayment::openCheckout),
                CommonUi.link("Back to Home", onLeave));

        poll.setCycleCount(Timeline.INDEFINITE);
        // check while on screen only: stop when this step is replaced or the screen is left
        sceneProperty().addListener((obs, was, now) -> {
            if (now == null) {
                poll.stop();
            } else if (!done) {
                poll.play();
            }
        });
        Platform.runLater(this::check);   // a server without PayMongo confirms at once
    }

    private void check() {
        if (checking || done) {
            return;
        }
        checking = true;
        status.setText("Checking with PayMongo…");
        Thread t = new Thread(() -> {
            boolean paid = LoadPayment.confirmed();
            Platform.runLater(() -> {
                checking = false;
                if (done) {
                    return;
                }
                if (!paid) {
                    status.setText("Not confirmed yet. Finish paying on the PayMongo page.");
                    return;
                }
                done = true;
                poll.stop();
                LoadPayment.addPending(BuyLoadState.getCheckoutId());
                onPaid.run();
            });
        }, "farex-payment-check");
        t.setDaemon(true);
        t.start();
    }
}
