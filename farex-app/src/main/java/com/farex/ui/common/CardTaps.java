package com.farex.ui.common;

import java.util.function.Consumer;

import javafx.application.Platform;
import javafx.scene.Node;

import com.farex.Api;
import com.farex.CardReader;

// Real NFC card taps on a phone. One thread waits for each new tap (CardReader.next); the screen on show decides
// what a tap does by claiming it (Ready Mode taps in/out, Add Card links the card, Fetch Load starts fetching).
// Anywhere else a tap shows the card's balance. A laptop has no card reader: start() does nothing there.
public final class CardTaps {

    private static Node owner;                 // the claim holds while this node is on screen
    private static Consumer<String> onTap;

    private CardTaps() {
    }

    /** From now on, while owner is on screen, a tap calls onTap with the card's UID (on the FX thread). */
    public static void claim(Node owner, Consumer<String> onTap) {
        CardTaps.owner = owner;
        CardTaps.onTap = onTap;
    }

    public static void start() {
        if (!com.gluonhq.attach.util.Platform.isAndroid()) {
            return;
        }
        Thread t = new Thread(() -> {
            while (true) {
                try {
                    String uid = CardReader.next();
                    Platform.runLater(() -> dispatch(uid));
                } catch (Exception e) {
                    if (!"no card".equals(e.getMessage())) {          // 30 s without a tap is normal: ask again
                        System.err.println("Card reader: " + e.getMessage());
                        sleep();                                    // reader not up yet, or failing: do not spin
                    }
                }
            }
        }, "farex-card-taps");
        t.setDaemon(true);
        t.start();
    }

    private static void dispatch(String uid) {
        if (owner != null && owner.getScene() != null) {
            onTap.accept(uid);
        } else {
            showBalance(uid);
        }
    }

    // Passengers see only their own cards' balance, operators any card's; the server says which
    private static void showBalance(String uid) {
        Thread t = new Thread(() -> {
            String text;
            try {
                text = "Balance: " + CommonUi.peso(Api.balance(uid) / 100.0);
            } catch (Api.ApiException e) {
                text = "Card is not yours".equals(e.getMessage())   // the server's words for "not linked to you"
                        ? "This card is not linked to your account. To link it, open Add Card and tap the card there."
                        : e.getMessage();
            }
            String shown = text;
            Platform.runLater(() -> new PopupDialog("Card " + uid, shown).button("OK", () -> { }).show());
        }, "farex-card-balance");
        t.setDaemon(true);
        t.start();
    }

    private static void sleep() {
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
