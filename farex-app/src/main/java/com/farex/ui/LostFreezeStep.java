package com.farex.ui;

import javafx.geometry.Insets;
import javafx.scene.layout.VBox;

// Lost Card / 04 "Protecting your card…": fake server freeze.
// Demo: Esc acts like the connection dropping.
public class LostFreezeStep extends ProgressStep {

    private final Runnable onSuccess;
    private final Runnable onFailure;

    public LostFreezeStep(Runnable onSuccess, Runnable onFailure) {
        this.onSuccess = onSuccess;
        this.onFailure = onFailure;

        DemoCard card = LostCardState.getCard();

        VBox heading = new VBox(8,
                FetchUi.title("Protecting your card…"),
                FetchUi.subtitle("Please wait for server confirmation before leaving this screen."));
        VBox.setMargin(heading, new Insets(0, 0, 6, 0));

        VBox panel = FetchUi.panel(FetchUi.row("Old card UID", FetchUi.masked(card)));
        panel.getChildren().addAll(steps(
                "Cardholder verified",
                "Freezing old card UID…",
                "Secure remaining ledger balance"));
        panel.setPadding(new Insets(16, 14, 18, 14));

        getChildren().addAll(
                heading,
                panel,
                FetchUi.note("Your card is not confirmed frozen yet. If the connection fails, "
                        + "check the report status or contact support immediately."),
                FetchUi.spacer(),
                busyBar("Submitting report…"));

        start(1);
    }

    @Override
    protected void onFinished() {
        LostCardState.freeze();
        onSuccess.run();
    }

    @Override
    protected void onFailed() {
        onFailure.run();
    }
}
