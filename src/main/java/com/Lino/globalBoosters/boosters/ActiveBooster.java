package com.Lino.globalBoosters.boosters;

import java.util.UUID;

public class ActiveBooster {

    private final BoosterType type;
    private final UUID activatorUUID;
    private final String activatorName;
    private final long startTime;
    private final int durationMinutes;
    private long remainingSeconds;

    public ActiveBooster(BoosterType type, UUID activatorUUID, String activatorName, int durationMinutes) {
        this.type = type;
        this.activatorUUID = activatorUUID;
        this.activatorName = activatorName;
        this.startTime = System.currentTimeMillis();
        this.durationMinutes = durationMinutes;
        this.remainingSeconds = durationMinutes * 60L;
    }

    public ActiveBooster(BoosterType type, UUID activatorUUID, String activatorName, long startTime, int durationMinutes, long remainingSeconds) {
        this.type = type;
        this.activatorUUID = activatorUUID;
        this.activatorName = activatorName;
        this.startTime = startTime;
        this.durationMinutes = durationMinutes;
        this.remainingSeconds = Math.max(0, remainingSeconds);
    }

    public void tick() {
        if (remainingSeconds > 0) {
            remainingSeconds--;
        }
    }

    public boolean isExpired() {
        return remainingSeconds <= 0;
    }

    public double getProgress() {
        if (durationMinutes * 60 == 0) return 0;
        return Math.max(0, Math.min(1, (double) remainingSeconds / (durationMinutes * 60)));
    }

    public String getTimeRemaining() {
        if (remainingSeconds <= 0) {
            return "0s";
        }

        long hours = remainingSeconds / 3600;
        long minutes = (remainingSeconds % 3600) / 60;
        long seconds = remainingSeconds % 60;

        if (hours > 0) {
            return String.format("%dh %dm %ds", hours, minutes, seconds);
        } else if (minutes > 0) {
            return String.format("%dm %ds", minutes, seconds);
        } else {
            return String.format("%ds", seconds);
        }
    }

    public BoosterType getType() {
        return type;
    }

    public UUID getActivatorUUID() {
        return activatorUUID;
    }

    public String getActivatorName() {
        return activatorName;
    }

    public long getStartTime() {
        return startTime;
    }

    public int getDurationMinutes() {
        return durationMinutes;
    }

    public long getRemainingSeconds() {
        return Math.max(0, remainingSeconds);
    }
}
