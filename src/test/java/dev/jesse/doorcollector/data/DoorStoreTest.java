package dev.jesse.doorcollector.data;

import com.google.gson.JsonArray;
import com.google.gson.JsonParser;
import dev.jesse.doorcollector.model.Direction;
import dev.jesse.doorcollector.model.DoorObservation;
import dev.jesse.doorcollector.model.Int3;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DoorStoreTest {
    @TempDir Path temp;

    @Test
    void nearbyObservationsMergeAndDistantDoorsStaySeparate() throws Exception {
        DoorStore store = new DoorStore(temp);
        store.record("Temple", new DoorObservation(Direction.NORTH, new Int3(15,68,0), true));
        store.record("Temple", new DoorObservation(Direction.NORTH, new Int3(16,69,0), false));
        store.record("Temple", new DoorObservation(Direction.NORTH, new Int3(47,68,0), true));

        assertEquals(2, store.entries().size());
        CollectedDoor merged = store.entries().getFirst();
        assertEquals(2, merged.samples());
        assertEquals(1, merged.automaticSamples());
        assertEquals(15.5, merged.x());
    }

    @Test
    void oneRoomWallSegmentCanOnlyProduceOneDoor() throws Exception {
        DoorStore store = new DoorStore(temp);
        store.record("Temple", new DoorObservation(Direction.NORTH, new Int3(4, 68, 0), true));
        store.record(" temple ", new DoorObservation(Direction.NORTH, new Int3(27, 75, 0), false));
        store.record("TEMPLE", new DoorObservation(Direction.NORTH, new Int3(16, 69, 0), true));

        assertEquals(1, store.entries().size());
        assertEquals(3, store.entries().getFirst().samples());
        assertEquals(2, store.entries().getFirst().automaticSamples());
    }

    @Test
    void separateWallSegmentsRemainSeparate() throws Exception {
        DoorStore store = new DoorStore(temp);
        store.record("Temple", new DoorObservation(Direction.NORTH, new Int3(15, 68, 0), true));
        store.record("Temple", new DoorObservation(Direction.NORTH, new Int3(47, 68, 0), true));

        assertEquals(2, store.entries().size());
    }

    @Test
    void loadingCompactsHistoricalDuplicatesAndPreservesEvidence() throws Exception {
        Files.writeString(temp.resolve("collected-doors.json"), """
            [
              {"roomId":"Temple","direction":"NORTH","x":4,"y":68,"z":0,"samples":2,"automaticSamples":2,"lastSeenEpochMs":10},
              {"roomId":" temple ","direction":"NORTH","x":27,"y":75,"z":0,"samples":3,"automaticSamples":1,"lastSeenEpochMs":20}
            ]
            """, StandardCharsets.UTF_8);

        DoorStore store = new DoorStore(temp);
        store.load();

        assertEquals(1, store.entries().size());
        assertEquals(5, store.entries().getFirst().samples());
        assertEquals(3, store.entries().getFirst().automaticSamples());
        assertEquals(20, store.entries().getFirst().lastSeenEpochMs());
        assertEquals(1, JsonParser.parseString(Files.readString(temp.resolve("collected-doors.json"))).getAsJsonArray().size());
    }

    @Test
    void exportIsCompatibleMetadataWithEvidenceCounts() throws Exception {
        DoorStore store = new DoorStore(temp);
        store.record("Temple", new DoorObservation(Direction.NORTH, new Int3(15,68,0), true));
        Path exported = store.exportMetadata();
        JsonArray rooms = JsonParser.parseString(Files.readString(exported, StandardCharsets.UTF_8)).getAsJsonArray();
        var room = rooms.get(0).getAsJsonObject();
        var door = room.getAsJsonArray("possibleDoors").get(0).getAsJsonObject();

        assertEquals("Temple", room.get("roomId").getAsString());
        assertEquals("north", door.get("direction").getAsString());
        assertEquals(15, door.get("x").getAsInt());
        assertEquals(1, door.get("samples").getAsInt());
        assertTrue(Files.exists(temp.resolve("collected-doors.json")));
    }
}
