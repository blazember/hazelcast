package com.hazelcast.internal.config.updater;

class UpdatePlanEntry {
    final int startPos;
    final int endPos;
    final ConfigUpdate update;

    UpdatePlanEntry(int startPos, int endPos, ConfigUpdate update) {
        this.startPos = startPos;
        this.endPos = endPos;
        this.update = update;
    }

    @Override
    public String toString() {
        return "UpdatePlanEntry{" +
                "startPos=" + startPos +
                ", endPos=" + endPos +
                ", update=" + update +
                '}';
    }
}
