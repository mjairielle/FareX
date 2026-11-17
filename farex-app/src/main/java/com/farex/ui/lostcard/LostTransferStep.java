package com.farex.ui.lostcard;

import javafx.geometry.Insets;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;

import com.farex.ui.common.CommonUi;
import com.farex.ui.common.ProgressStep;
import com.farex.ui.common.Theme;
import com.farex.ui.data.DemoCard;

// Lost Card / 10 "Transferring your balance…": fake balance transfer.
// Demo: Esc acts like the connection dropping.
public class LostTransferStep extends ProgressStep {

    private final Runnable onSuccess;
    private final Runnable onFailure;

    public LostTransferStep(Runnable onSuccess, Runnable onFailure) {
        this.onSuccess = onSuccess;
        this.onFailure = onFailure;

        DemoCard card = LostCardState.getCard();
        String newNumber = LostCardState.getNewCardNumber();

        VBox heading = new VBox(8,
                CommonUi.title("Transferring your balance…"),
                CommonUi.subtitle("Please wait. Your replacement card will activate after the server "
                        + "confirms the transfer."));
        VBox.setMargin(heading, new Insets(0, 0, 6, 0));

        Label amount = new Label(CommonUi.peso(card.getBalance()));
        amount.setTextFill(Theme.LIME);
        amount.setFont(Theme.bold(26));

        // "6767 → 8842"
        String uids = card.getLastFour() + " → " + newNumber.substring(newNumber.length() - 4);

        VBox panel = CommonUi.panel(amount, CommonUi.row("Old UID → New UID", uids));
        panel.getChildren().addAll(steps(
                "Old UID frozen",
                "New card issued and verified",
                "Moving ledger balance…"));
        panel.setPadding(new Insets(16, 14, 18, 14));

        getChildren().addAll(
                heading,
                panel,
                CommonUi.note("Do not request a second transfer. If interrupted, check the transfer status "
                        + "to avoid duplicate requests."),
                CommonUi.spacer(),
                busyBar("Transferring…"));

        start(2);   // Figma: steps 1 and 2 done, step 3 running
    }

    @Override
    protected void onFinished() {
        LostCardState.transfer();
        onSuccess.run();
    }

    @Override
    protected void onFailed() {
        onFailure.run();
    }
}
