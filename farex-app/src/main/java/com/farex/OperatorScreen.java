package com.farex;

import farex.shared.data.Store;
import farex.shared.data.TapDesk;
import farex.shared.txn.Txn;
import farex.shared.user.Operator;
import javafx.collections.FXCollections;
import javafx.scene.control.*;

public class OperatorScreen extends Screen {
    private record CardItem(String uid, String label) {
        @Override public String toString() { return label; }
    }

    private final TapDesk desk = new TapDesk(Session.store, Session.user.getId());
    private final ComboBox<CardItem> cards = combo();
    private final ComboBox<String> stops = combo();
    private final Label result = lime("", 14);

    public OperatorScreen() {
        Operator op = (Operator) Session.user;

        for (String uid : Session.store.allCards()) {
            String owner = Session.store.findUser(Session.store.ownerOf(uid)).map(u -> u.getName()).orElse("?");
            cards.getItems().add(new CardItem(uid, Store.formatUid(uid) + " - " + owner));
        }
        desk.stops().forEach(s -> stops.getItems().add(s.name()));

        getChildren().addAll(heading("Operator Shift"),
                text("Vehicle " + op.getVehiclePlate() + "  |  Route: Philcoa - Fairview"),
                text("Passenger card (phone NFC is simulated)"), cards,
                text("Stop"), stops,
                primary("Tap In", () -> {
                    desk.tapIn(uid(), stopIdx());
                    result.setText("Tapped in at " + stops.getValue() + ". On board: " + desk.onBoard());
                }),
                primary("Tap Out", () -> {
                    String uid = uid();
                    Txn t = desk.tapOut(uid, stopIdx());
                    done(t, uid);
                }),
                secondary("No tap out (charge max fare)", () -> {
                    String uid = uid();
                    done(desk.missedTapOut(uid), uid);
                }),
                result, error,
                secondary("Log out", this::logout));
    }

    private void done(Txn t, String uid) {
        result.setText(t.describe() + "\nNew balance " + peso(Session.store.balance(uid))
                + "\nOn board: " + desk.onBoard());
    }

    private String uid() {
        if (cards.getValue() == null) throw new IllegalStateException("Pick a card");
        return cards.getValue().uid();
    }

    private int stopIdx() {
        int i = stops.getSelectionModel().getSelectedIndex();
        if (i < 0) throw new IllegalStateException("Pick a stop");
        return i;
    }
}
