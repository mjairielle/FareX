package com.farex.operator;

import com.farex.Api;
import farex.shared.fare.Fares;

import java.util.ArrayList;
import java.util.List;

/**
 * Manages transit vehicle route stops, current GPS position, and auto-drive simulation.
 * Single Responsibility: GPS navigation along route waypoints and driving simulation.
 */
public class GpsSimulator {
    private final List<Api.StopCoord> routeStops = new ArrayList<>();
    private int currentStopIdx = 0;
    private boolean autoDriving = false;
    private int countdown = 6;
    private static final int COUNTDOWN_INTERVAL = 6;

    public void setRouteStops(List<Api.StopCoord> stops) {
        routeStops.clear();
        if (stops != null) {
            routeStops.addAll(stops);
        }
        currentStopIdx = 0;
        autoDriving = false;
        countdown = COUNTDOWN_INTERVAL;
    }

    public List<Api.StopCoord> getRouteStops() {
        return routeStops;
    }

    public int getCurrentStopIdx() {
        return currentStopIdx;
    }

    public void setCurrentStopIdx(int idx) {
        if (idx >= 0 && idx < routeStops.size()) {
            this.currentStopIdx = idx;
        }
    }

    public Api.StopCoord getCurrentStop() {
        if (routeStops.isEmpty() || currentStopIdx >= routeStops.size()) return null;
        return routeStops.get(currentStopIdx);
    }

    public Api.StopCoord getNextStop() {
        if (currentStopIdx < routeStops.size() - 1) {
            return routeStops.get(currentStopIdx + 1);
        }
        return null;
    }

    public double getDistanceToNextKm() {
        Api.StopCoord curr = getCurrentStop();
        Api.StopCoord next = getNextStop();
        if (curr == null || next == null) return 0.0;
        return Fares.haversineKm(curr.lat(), curr.lng(), next.lat(), next.lng());
    }

    public boolean isAutoDriving() {
        return autoDriving;
    }

    public void setAutoDriving(boolean autoDriving) {
        this.autoDriving = autoDriving;
        this.countdown = COUNTDOWN_INTERVAL;
    }

    public void toggleAutoDriving() {
        setAutoDriving(!this.autoDriving);
    }

    public int getCountdown() {
        return countdown;
    }

    /**
     * Advances simulation tick. Returns true if stop arrival occurred.
     */
    public boolean tick() {
        if (!autoDriving) return false;
        countdown--;
        if (countdown <= 0) {
            countdown = COUNTDOWN_INTERVAL;
            if (currentStopIdx < routeStops.size() - 1) {
                currentStopIdx++;
                return true;
            } else {
                autoDriving = false;
                return true;
            }
        }
        return false;
    }

    public boolean hasNextStop() {
        return currentStopIdx < routeStops.size() - 1;
    }

    public boolean hasPrevStop() {
        return currentStopIdx > 0;
    }

    public void advanceStop() {
        if (hasNextStop()) {
            currentStopIdx++;
        }
    }

    public void rewindStop() {
        if (hasPrevStop()) {
            currentStopIdx--;
        }
    }
}
