package dev.jesse.doorcollector.model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RoomGeometryTest {
    @Test
    void detectsAllFourBoundaries() {
        RoomGeometry room = new RoomGeometry(new Int3(0, 0, 0), Rotation.NONE, 31, 31);
        assertEquals(Direction.NORTH, room.crossing(new Int3(15,68,-1), new Int3(15,68,0)).orElseThrow().direction());
        assertEquals(Direction.EAST, room.crossing(new Int3(31,68,15), new Int3(30,68,15)).orElseThrow().direction());
        assertEquals(Direction.SOUTH, room.crossing(new Int3(15,68,31), new Int3(15,68,30)).orElseThrow().direction());
        assertEquals(Direction.WEST, room.crossing(new Int3(-1,68,15), new Int3(0,68,15)).orElseThrow().direction());
    }

    @Test
    void detectsExitAsSameDoor() {
        RoomGeometry room = new RoomGeometry(new Int3(0, 0, 0), Rotation.NONE, 31, 31);
        DoorObservation observation = room.crossing(new Int3(15,68,0), new Int3(15,68,-1)).orElseThrow();
        assertEquals(Direction.NORTH, observation.direction());
        assertEquals(new Int3(15,68,0), observation.position());
    }

    @Test
    void rotatedRoomProducesLocalDoorCoordinates() {
        RoomGeometry room = new RoomGeometry(new Int3(100, 0, 100), Rotation.CLOCKWISE_90, 31, 31);
        DoorObservation observation = room.crossing(new Int3(101,68,115), new Int3(100,68,115)).orElseThrow();
        assertEquals(Direction.NORTH, observation.direction());
        assertEquals(new Int3(15,68,0), observation.position());
    }

    @Test
    void ignoresMovementEntirelyInsideOrOutside() {
        RoomGeometry room = new RoomGeometry(new Int3(0, 0, 0), Rotation.NONE, 31, 31);
        assertTrue(room.crossing(new Int3(10,68,10), new Int3(11,68,10)).isEmpty());
        assertTrue(room.crossing(new Int3(-2,68,10), new Int3(-1,68,10)).isEmpty());
    }
}
