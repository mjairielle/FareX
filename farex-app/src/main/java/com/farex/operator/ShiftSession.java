package com.farex.operator;

import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

/**
 * Tracks shift state, start/end timestamps, and aggregate earnings metrics.
 * Single Responsibility: Shift lifecycle and session earnings calculations.
 */
public class ShiftSession {
    private static final DateTimeFormatter TIME_FMT =
            DateTimeFormatter.ofPattern("h:mm a").withZone(ZoneId.systemDefault());

    private boolean onShift = false;
    private Instant startTime = null;
    private long earningsCents = 0;
    private int completedTrips = 0;
    private int totalBoarded = 0;

    public void start() {
        this.onShift = true;
        this.startTime = Instant.now();
        this.earningsCents = 0;
        this.completedTrips = 0;
        this.totalBoarded = 0;
    }

    public void end() {
        this.onShift = false;
    }

    public boolean isOnShift() {
        return onShift;
    }

    public Instant getStartTime() {
        return startTime;
    }

    public String getStartTimeFormatted() {
        return startTime != null ? TIME_FMT.format(startTime) : "--:--";
    }

    public Duration getDuration() {
        if (startTime == null) return Duration.ZERO;
        return Duration.between(startTime, Instant.now());
    }

    public String getFormattedDuration() {
        Duration d = getDuration();
        long hours = d.toHours();
        long minutes = d.toMinutesPart();
        return hours > 0 ? String.format("%dh %02dm", hours, minutes) : String.format("%dm", minutes);
    }

    public void recordTrip(long fareCents) {
        this.earningsCents += fareCents;
        this.completedTrips++;
    }

    public void recordBoarded() {
        this.totalBoarded++;
    }

    public long getEarningsCents() {
        return earningsCents;
    }

    public int getCompletedTrips() {
        return completedTrips;
    }

    public int getTotalBoarded() {
        return totalBoarded;
    }
}
