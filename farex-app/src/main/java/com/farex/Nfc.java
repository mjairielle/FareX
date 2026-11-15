package com.farex;

import farex.shared.card.CardIO;

import java.util.HashMap;
import java.util.HexFormat;
import java.util.Map;
import java.util.function.Function;

/**
 * Where the app meets a physical card. Laptop gets one pretend card per uid.
 * The Android build sets {@link #cardFor} to the real reader, nothing else changes.
 */
final class Nfc {
    private Nfc() {}

    private static final Map<String, CardIO> SIM = new HashMap<>();

    // shortcut: pretend cards are issued signed on first use and forgotten on exit, real cards replace this on Android
    static Function<String, CardIO> cardFor = uid -> SIM.computeIfAbsent(uid, u -> {
        CardIO c = new farex.shared.card.SimCardIO(u);
        c.writePayload(HexFormat.of().parseHex(Api.cardPayload(u)));
        return c;
    });

    /** App user ids (p1) are not cards, so tap checks skip them. */
    static boolean isCard(String id) {
        // /cards/all is operator only, a passenger asks for their own cards
        if (Session.user.role().equals("PASSENGER")) return Api.cardsOf().contains(id);
        for (String[] c : Api.allCards()) if (c[0].equals(id)) return true;
        return false;
    }

    /** Throws when the bytes on the card were not signed by our server. */
    static void checkOnTap(String id) {
        if (!isCard(id)) return;
        CardIO card = cardFor.apply(id);
        if (Api.verifyCard(id, HexFormat.of().formatHex(card.readPayload())) < 0)
            throw new IllegalStateException("Card failed the signature check. Rejected.");
    }

    /** Writes the server's current signed balance to the card. Call after every load and fare. */
    static void rewrite(String id) {
        if (!isCard(id)) return;
        cardFor.apply(id).writePayload(HexFormat.of().parseHex(Api.cardPayload(id)));
    }
}
