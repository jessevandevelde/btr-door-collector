package dev.jesse.doorcollector;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class RoomGridTest {
    @Test
    void resolvesDungeonGridCornersAcrossNegativeCoordinates() {
        assertEquals(24, RoomGrid.corner(30));
        assertEquals(-8, RoomGrid.corner(0));
        assertEquals(-40, RoomGrid.corner(-30));
    }
}
