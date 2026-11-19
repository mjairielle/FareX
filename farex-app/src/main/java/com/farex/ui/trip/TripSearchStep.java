package com.farex.ui.trip;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.VBox;

import com.farex.Api;
import com.farex.ui.common.CommonUi;
import com.farex.ui.common.ProgressStep;
import com.farex.ui.common.Theme;

// Trip Planner / 06 "Searching routes…". Live: asks the FareX planner (Api.plan) in the
// background while the steps light up, then shows its plans; no plan -> "No route found",
// no answer within 8 s -> "Connection lost". Offline preview: the sample routes.
// Demo keys still work: N = no route found, Esc = connection lost.
public class TripSearchStep extends ProgressStep {

    private static final int TIMEOUT_SECONDS = 8;

    private final Runnable onFound;
    private final Runnable onConnectionLost;
    private final Runnable onNoRoute;
    private final CompletableFuture<List<Api.PlanCard>> search;

    public TripSearchStep(Runnable onFound, Runnable onConnectionLost, Runnable onNoRoute, Runnable onCancel) {
        this.onFound = onFound;
        this.onConnectionLost = onConnectionLost;
        this.onNoRoute = onNoRoute;

        // Start asking right away; the animation below runs while the server works
        if (TripState.isLive()) {
            String from = TripState.getStart().getName();
            String to = TripState.getDestination().getName();
            search = CompletableFuture.supplyAsync(() -> Api.plan(from, to))
                    .orTimeout(TIMEOUT_SECONDS, TimeUnit.SECONDS);
        } else {
            search = CompletableFuture.completedFuture(null);   // offline preview: samples
        }

        VBox heading = new VBox(8,
                CommonUi.title("Searching routes…"),
                CommonUi.subtitle(TripState.summary()));
        VBox.setMargin(heading, new Insets(0, 0, 6, 0));

        VBox panel = CommonUi.panel(steps(
                "Match nearby stops",
                "Search available connections",
                "Rank up to three routes"));
        panel.getChildren().add(CommonUi.small(
                "Shortest distance · Fewest transfers · Cheapest fare", Theme.WHITE));
        panel.setPadding(new Insets(16, 14, 18, 14));

        VBox tip = CommonUi.panel(
                TripUi.bold("A little planning goes a long way", 12),
                CommonUi.subtitle("Fare and travel time are estimates, not live transport information."));
        tip.setSpacing(4);

        getChildren().addAll(
                heading,
                panel,
                tip,
                CommonUi.small("Having connection trouble?", Theme.WHITE),
                CommonUi.spacer(),
                CommonUi.link("Cancel search", () -> {
                    stopSteps();
                    search.cancel(true);
                    onCancel.run();
                }));

        start(1);
    }

    // The steps are done; show the result as soon as the search has one
    @Override
    protected void onFinished() {
        search.whenComplete((plans, error) -> Platform.runLater(() -> {
            if (getScene() == null) {
                return;   // left with the back arrow or Cancel meanwhile
            }
            if (error != null) {
                onConnectionLost.run();
            } else if (plans == null) {
                TripState.setRoutes(TripRoute.SAMPLES);
                onFound.run();
            } else if (plans.isEmpty()) {
                onNoRoute.run();
            } else {
                List<TripRoute> found = new ArrayList<>();
                for (Api.PlanCard p : plans) found.add(TripRoute.fromPlan(p));
                TripState.setRoutes(found);   // the first route starts highlighted
                onFound.run();
            }
        }));
    }

    @Override
    protected void onFailed() {
        search.cancel(true);
        onConnectionLost.run();
    }

    @Override
    protected void onOtherKey(KeyCode code) {
        if (code == KeyCode.N) {
            stopSteps();
            search.cancel(true);
            onNoRoute.run();
        }
    }
}
