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
        store.record("Temple", new DoorObservation(Direction.NORTH, new Int3(25,68,0), true));

        assertEquals(2, store.entries().size());
        CollectedDoor merged = store.entries().getFirst();
        assertEquals(2, merged.samples());
        assertEquals(1, merged.automaticSamples());
        assertEquals(15.5, merged.x());
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
