package com.farex.ui;

import javafx.geometry.Insets;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;

// Lost Card / 10: fake balance transfer. Esc (demo) -> "Transfer not confirmed"
public class LostTransferView extends ProgressScreen {

    public LostTransferView() {
        super("Replace or Report Lost Card", "lostTransferReview");

        DemoCard card = LostCardState.getCard();
        String newNumber = LostCardState.getNewCardNumber();

        VBox heading = new VBox(8,
                FetchUi.title("Transferring your balance…"),
                FetchUi.subtitle("Please wait. Your replacement card will activate after the server "
                        + "confirms the transfer."));
        VBox.setMargin(heading, new Insets(0, 0, 6, 0));

        Label amount = new Label(FetchUi.peso(card.getBalance()));
        amount.setTextFill(Theme.LIME);
        amount.setFont(Theme.bold(26));

        // "6767 → 8842"
        String uids = card.getLastFour() + " → " + newNumber.substring(newNumber.length() - 4);

        VBox panel = FetchUi.panel(amount, FetchUi.row("Old UID → New UID", uids));
        panel.getChildren().addAll(steps(
                "Old UID frozen",
                "New card issued and verified",
                "Moving ledger balance…"));
        panel.setPadding(new Insets(16, 14, 18, 14));

        body.setSpacing(12);
        body.getChildren().addAll(
                heading,
                panel,
                FetchUi.note("Do not request a second transfer. If interrupted, check the transfer status "
                        + "to avoid duplicate requests."),
                FetchUi.spacer(),
                busyBar("Transferring…"));

        start(2);   // Figma: steps 1 and 2 done, step 3 running
    }

    @Override
    protected void onFinished() {
        LostCardState.transfer();
        Navigator.go("lostReplaced");
    }

    @Override
    protected void onFailed() {
        Navigator.go("lostTransferFailed");
    }
}
