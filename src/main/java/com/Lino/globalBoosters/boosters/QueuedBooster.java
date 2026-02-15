package com.Lino.globalBoosters.boosters;

import java.util.UUID;

public class QueuedBooster {

    private final BoosterType type;
    private final UUID activatorUUID;
    private final String activatorName;
    private final int durationMinutes;
    private final String source;

    public QueuedBooster(BoosterType type, UUID activatorUUID, String activatorName, int durationMinutes, String source) {
        this.type = type;
        this.activatorUUID = activatorUUID;
        this.activatorName = activatorName;
        this.durationMinutes = durationMinutes;
        this.source = source;
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

    public int getDurationMinutes() {
        return durationMinutes;
    }

    public String getSource() {
        return source;
    }
}