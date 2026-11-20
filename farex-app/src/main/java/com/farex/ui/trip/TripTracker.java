package com.farex.ui.trip;

import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

import javafx.application.Platform;

import com.farex.Api;
import com.farex.ui.common.LeafletMap;
import farex.shared.data.RouteShapes.Point;
import farex.shared.data.RouteStop;
import farex.shared.plan.TripProgress;

// The trip being tracked: the planned legs as a TripProgress, fed by a LocationSource. One at a
// time; TripState.clear() (ending the trip, leaving the planner) stops it, so location is only
// used while the trip screen is up.
public final class TripTracker {

    // One fix: where the passenger is and what that means for the trip
    public interface Listener {
        void onFix(Point at, TripProgress.Status status);
    }

    private static TripTracker current;

    private final TripProgress progress;
    private final LocationSource source;
    private Listener listener;

    private TripTracker(TripProgress progress, LocationSource source) {
        this.progress = progress;
        this.source = source;
    }

    // Builds the trip's road paths in the background (they come from the server), then calls
    // ready with the tracker - not yet started - or failed with a reason, on the FX thread.
    public static void prepare(List<Api.PlanRide> rides, Map<String, RouteStop> points,
                               Consumer<TripTracker> ready, Consumer<String> failed) {
        stopCurrent();
        Thread t = new Thread(() -> {
            try {
                List<TripProgress.Ride> legs = LeafletMap.progressLegs(rides, points);
                if (legs.isEmpty()) {
                    Platform.runLater(() -> failed.accept("This route’s map data is missing, so it cannot be tracked."));
                    return;
                }
                TripProgress progress = new TripProgress(legs);
                Platform.runLater(() -> {
                    stopCurrent();
                    current = new TripTracker(progress, LocationSource.best(progress));
                    ready.accept(current);
                });
            } catch (RuntimeException offline) {
                Platform.runLater(() -> failed.accept("Could not reach FareX to load the route. Check your connection."));
            }
        }, "farex-trip-tracker");
        t.setDaemon(true);
        t.start();
    }

    public void start(Listener onFix) {
        listener = onFix;
        source.start((lat, lng) -> {
            if (current != this) return;                   // stopped meanwhile
            listener.onFix(new Point(lat, lng), progress.update(lat, lng));
        });
    }

    public void stop() {
        source.stop();
        if (current == this) current = null;
    }

    public static void stopCurrent() {
        if (current != null) current.stop();
    }

    public TripProgress progress() {
        return progress;
    }

    public boolean simulated() {
        return source.simulated();
    }

    // Simulator only: wander off the route for a few seconds
    public void simulateDetour() {
        if (source instanceof LocationSource.SimulatedRide ride) ride.detour();
    }
}
