package dev.jesse.doorcollector.model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class CoordinateTransformerTest {
    @Test
    void allRotationsRoundTrip() {
        Int3 local = new Int3(12, 4, 27);
        Int3 anchor = new Int3(100, 64, -50);
        for (Rotation rotation : Rotation.values()) {
            Int3 world = CoordinateTransformer.localToWorld(local, anchor, rotation);
            assertEquals(local, CoordinateTransformer.worldToLocal(world, anchor, rotation));
        }
    }
}
