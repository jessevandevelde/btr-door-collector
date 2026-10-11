package dev.jesse.doorcollector.data;

import dev.jesse.doorcollector.model.Direction;
import dev.jesse.doorcollector.model.DoorObservation;

public record CollectedDoor(String roomId, Direction direction, double x, double y, double z,
                            int samples, int automaticSamples, long lastSeenEpochMs) {
    public static CollectedDoor first(String roomId, DoorObservation observation, long now) {
        var p = observation.position();
        return new CollectedDoor(roomId, observation.direction(), p.x(), p.y(), p.z(), 1,
            observation.automatic() ? 1 : 0, now);
    }

    public CollectedDoor merge(DoorObservation observation, long now) {
        var p = observation.position();
        int nextSamples = samples + 1;
        return new CollectedDoor(roomId, direction,
            (x * samples + p.x()) / nextSamples,
            (y * samples + p.y()) / nextSamples,
            (z * samples + p.z()) / nextSamples,
            nextSamples, automaticSamples + (observation.automatic() ? 1 : 0), now);
    }

    public CollectedDoor merge(CollectedDoor other) {
        int nextSamples = samples + other.samples;
        return new CollectedDoor(roomId, direction,
            (x * samples + other.x * other.samples) / nextSamples,
            (y * samples + other.y * other.samples) / nextSamples,
            (z * samples + other.z * other.samples) / nextSamples,
            nextSamples, automaticSamples + other.automaticSamples,
            Math.max(lastSeenEpochMs, other.lastSeenEpochMs));
    }
}
