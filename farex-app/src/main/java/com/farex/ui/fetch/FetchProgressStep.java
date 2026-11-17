package com.farex.ui.fetch;

import javafx.geometry.Insets;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;

import com.farex.ui.common.CommonUi;
import com.farex.ui.common.ProgressStep;
import com.farex.ui.common.Theme;
import com.farex.ui.data.DemoCard;
import com.farex.ui.data.PendingLoad;

// Fetch Load / 03 "Fetching your load…": a fake card read. The steps light up one by one,
// then the load is added. Demo: Esc acts like the card being pulled away.
public class FetchProgressStep extends ProgressStep {

    private final PendingLoad load;
    private final Runnable onSuccess;
    private final Runnable onFailure;

    public FetchProgressStep(Runnable onSuccess, Runnable onFailure) {
        this.onSuccess = onSuccess;
        this.onFailure = onFailure;

        DemoCard card = FetchLoadState.getCard();
        load = card.getPendingLoad();
        FetchLoadState.setLastLoad(load);   // the failure pop-up shows this one

        VBox heading = new VBox(8,
                CommonUi.title("Fetching your load…"),
                CommonUi.subtitle("Keep your card in place. Do not close the app until the update is confirmed."));
        VBox.setMargin(heading, new Insets(0, 0, 6, 0));

        Label amount = new Label(CommonUi.peso(load.getAmount()));
        amount.setTextFill(Theme.LIME);
        amount.setFont(Theme.bold(30));

        VBox panel = CommonUi.panel(amount, CommonUi.row("Card", CommonUi.masked(card)));
        panel.getChildren().addAll(steps(
                "Payment verified",
                "Writing load to your card…",
                "Confirm updated balance"));
        panel.setPadding(new Insets(16, 14, 18, 14));

        getChildren().addAll(
                heading,
                panel,
                CommonUi.note("If the connection drops, check the fetch status before trying again. "
                        + "The same top-up can only be collected once."),
                CommonUi.spacer(),
                busyBar("Fetching…"));

        start(1);   // Figma start: step 1 done, step 2 in progress
    }

    @Override
    protected void onFinished() {
        FetchLoadState.collect(load);
        onSuccess.run();
    }

    @Override
    protected void onFailed() {
        onFailure.run();
    }
}
