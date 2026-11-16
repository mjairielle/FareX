package com.farex.ui;

import javafx.geometry.Insets;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;

// Redeem Points / 03: fake server check. Esc (demo) -> "Couldn't redeem points"
public class RedeemProgressView extends ProgressScreen {

    public RedeemProgressView() {
        super("Redeem Points", "redeemReview");

        int points = RedeemState.getSelected();

        VBox heading = new VBox(8,
                FetchUi.title("Redeeming your points…"),
                FetchUi.subtitle("Please wait while we confirm your redemption. Do not submit another request."));
        VBox.setMargin(heading, new Insets(0, 0, 6, 0));

        // "100 pts → ₱100.00"
        Label exchange = new Label(FetchUi.pts(points) + " → " + FetchUi.peso(points));
        exchange.setTextFill(Theme.LIME);
        exchange.setFont(Theme.bold(24));

        VBox panel = FetchUi.panel(exchange);
        panel.getChildren().addAll(steps(
                "Points balance checked",
                "Confirming redemption…",
                "Make credit ready to fetch"));
        panel.setPadding(new Insets(16, 14, 18, 14));

        body.setSpacing(12);
        body.getChildren().addAll(
                heading,
                panel,
                FetchUi.note("If the app disconnects, check your redemption status before retrying."),
                FetchUi.spacer(),
                busyBar("Redeeming…"));

        start(1);
    }

    @Override
    protected void onFinished() {
        RedeemState.redeem();
        Navigator.go("redeemSuccess");
    }

    @Override
    protected void onFailed() {
        Navigator.go("redeemFailed");
    }
}
