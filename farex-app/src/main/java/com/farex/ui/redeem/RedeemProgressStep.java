package com.farex.ui.redeem;

import javafx.geometry.Insets;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;

import com.farex.ui.common.CommonUi;
import com.farex.ui.common.ProgressStep;
import com.farex.ui.common.Theme;

// Redeem Points / 03 "Redeeming your points…": a fake server check.
// Demo: Esc acts like the connection dropping.
public class RedeemProgressStep extends ProgressStep {

    private final Runnable onSuccess;
    private final Runnable onFailure;

    public RedeemProgressStep(Runnable onSuccess, Runnable onFailure) {
        this.onSuccess = onSuccess;
        this.onFailure = onFailure;

        int points = RedeemState.getSelected();

        VBox heading = new VBox(8,
                CommonUi.title("Redeeming your points…"),
                CommonUi.subtitle("Please wait while we confirm your redemption. Do not submit another request."));
        VBox.setMargin(heading, new Insets(0, 0, 6, 0));

        // "100 pts → ₱100.00"
        Label exchange = new Label(CommonUi.pts(points) + " → " + CommonUi.peso(points));
        exchange.setTextFill(Theme.LIME);
        exchange.setFont(Theme.bold(24));

        VBox panel = CommonUi.panel(exchange);
        panel.getChildren().addAll(steps(
                "Points balance checked",
                "Confirming redemption…",
                "Make credit ready to fetch"));
        panel.setPadding(new Insets(16, 14, 18, 14));

        getChildren().addAll(
                heading,
                panel,
                CommonUi.note("If the app disconnects, check your redemption status before retrying."),
                CommonUi.spacer(),
                busyBar("Redeeming…"));

        start(1);
    }

    @Override
    protected void onFinished() {
        RedeemState.redeem();
        onSuccess.run();
    }

    @Override
    protected void onFailed() {
        onFailure.run();
    }
}
