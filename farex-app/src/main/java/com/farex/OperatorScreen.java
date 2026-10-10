package com.farex;

import farex.shared.data.Store;
import farex.shared.user.Operator;
import javafx.scene.control.*;

public class OperatorScreen extends Screen {
    private record CardItem(String uid, String label) {
        @Override public String toString() { return label; }
    }

    private final ComboBox<CardItem> cards = combo();
    private final ComboBox<String> stops = combo();
    private final Label result = lime("", 14);

    public OperatorScreen() {
        Operator op = (Operator) Session.user;

        for (String[] c : Api.allCards()) cards.getItems().add(new CardItem(c[0], Store.formatUid(c[0]) + " - " + c[1]));
        stops.getItems().addAll(Api.stops());

        getChildren().addAll(heading("Operator Shift"),
                text("Vehicle " + op.getVehiclePlate() + "  |  Route: Philcoa - Fairview"),
                text("Passenger card (phone NFC is simulated)"), cards,
                text("Stop"), stops,
                primary("Tap In", () -> {
                    int onBoard = Api.tapIn(uid(), stopIdx());
                    result.setText("Tapped in at " + stops.getValue() + ". On board: " + onBoard);
                }),
                primary("Tap Out", () -> done(Api.tapOut(uid(), stopIdx()))),
                secondary("No tap out (charge max fare)", () -> done(Api.tapMissed(uid()))),
                result, error,
                secondary("Log out", this::logout));
    }

    private void done(String[] r) {
        result.setText(r[0] + "\nNew balance " + peso(Long.parseLong(r[1])) + "\n+" + r[3] + " reward points\nOn board: " + r[2]);
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
