package com.farex;

import farex.shared.reward.Rewards;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;

public class RewardsScreen extends Screen {
    public RewardsScreen(String uid, String notice) {
        Api.Points pts = Api.rewards();
        getChildren().addAll(back(() -> Nav.show(new PassengerHome())), heading("Rewards"),
                lime(pts.points() + " points", 34),
                text("You earn 1 point per km traveled, added automatically after each trip."));
        if (pts.multiplier() > 1) getChildren().add(lime("Promo running: x" + pts.multiplier() + " points", 14));
        if (!notice.isEmpty()) getChildren().add(lime(notice, 14));

        for (Rewards.Perk k : Api.perks()) {
            Button redeem = primary("Redeem", () -> {
                Api.redeem(uid, k.id());
                Nav.show(new RewardsScreen(uid, "Redeemed " + k.name() + ". Added to your card."));
            });
            redeem.setDisable(pts.points() < k.points());
            VBox row = new VBox(6, lime(k.name(), 14), text(k.points() + " points"), redeem);
            row.setStyle("-fx-background-color: #1c1c1c; -fx-padding: 12;");
            getChildren().add(row);
        }
        getChildren().add(error);
    }
}
