package com.farex.ui.trip;

import java.util.Optional;
import java.util.function.BiConsumer;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.util.Duration;

import com.gluonhq.attach.position.Parameters;
import com.gluonhq.attach.position.Position;
import com.gluonhq.attach.position.PositionService;

import farex.shared.data.RouteShapes.Point;
import farex.shared.plan.TripProgress;

// Where position fixes come from while a trip is tracked: the phone's GPS when there is one,
// otherwise a simulated ride along the planned route (desktop, or a phone without location).
// Fixes are delivered on the JavaFX thread as (lat, lng).
public interface LocationSource {

    void start(BiConsumer<Double, Double> onFix);

    void stop();

    // True for the simulated ride: the screen says so, and offers "Simulate a detour"
    boolean simulated();

    // The phone's GPS (Gluon Attach Position: Android and iOS). Empty on desktop, where Attach has
    // no position service, or if the library is missing.
    static Optional<LocationSource> phone() {
        try {
            return PositionService.create().map(PhoneGps::new);
        } catch (Throwable noAttach) {
            return Optional.empty();
        }
    }

    // The phone if it has GPS, else a simulated ride along `trip`
    static LocationSource best(TripProgress trip) {
        return phone().orElseGet(() -> new SimulatedRide(trip));
    }

    final class PhoneGps implements LocationSource {
        private final PositionService service;
        private ChangeListener<Position> listener;

        PhoneGps(PositionService service) {
            this.service = service;
        }

        @Override
        public void start(BiConsumer<Double, Double> onFix) {
            listener = (obs, old, p) -> {
                if (p == null) return;
                if (Platform.isFxApplicationThread()) onFix.accept(p.getLatitude(), p.getLongitude());
                else Platform.runLater(() -> onFix.accept(p.getLatitude(), p.getLongitude()));
            };
            service.positionProperty().addListener(listener);
            // HIGH accuracy (GPS) while the trip screen is open; no background tracking
            service.start(new Parameters(Parameters.Accuracy.HIGH, false));
            Position now = service.getPosition();
            if (now != null) onFix.accept(now.getLatitude(), now.getLongitude());
        }

        @Override
        public void stop() {
            if (listener != null) service.positionProperty().removeListener(listener);
            service.stop();
        }

        @Override
        public boolean simulated() {
            return false;
        }
    }

    // Rides the planned path once a second, at the planner's walking and jeep speeds sped up
    // SPEEDUP times, so a whole trip plays in a few minutes. "Simulate a detour" pushes the
    // position ~550 m off the road for a few seconds to show the off-route warning.
    final class SimulatedRide implements LocationSource {
        public static final int SPEEDUP = 30;
        private static final int DETOUR_TICKS = 6;
        private static final double DETOUR_LNG = 0.005;     // ~540 m east at Manila's latitude

        private final TripProgress trip;
        private final Timeline timer;
        private BiConsumer<Double, Double> onFix;
        private double km = 0;
        private int detourLeft = 0;

        SimulatedRide(TripProgress trip) {
            this.trip = trip;
            timer = new Timeline(new KeyFrame(Duration.seconds(1), e -> tick()));
            timer.setCycleCount(Timeline.INDEFINITE);
        }

        @Override
        public void start(BiConsumer<Double, Double> onFix) {
            this.onFix = onFix;
            send();
            timer.play();
        }

        public void detour() {
            detourLeft = DETOUR_TICKS;
        }

        private void tick() {
            if (detourLeft > 0) {
                detourLeft--;                     // stuck on a side street: no progress along the route
            } else {
                double kph = trip.legAt(km).walk() ? TripProgress.WALK_KPH : TripProgress.JEEP_KPH;
                km = Math.min(trip.totalKm(), km + kph * SPEEDUP / 3600);
            }
            send();
            if (km >= trip.totalKm()) timer.stop();
        }

        private void send() {
            Point p = trip.pointAt(km);
            onFix.accept(p.lat(), p.lng() + (detourLeft > 0 ? DETOUR_LNG : 0));
        }

        @Override
        public void stop() {
            timer.stop();
        }

        @Override
        public boolean simulated() {
            return true;
        }
    }
}
