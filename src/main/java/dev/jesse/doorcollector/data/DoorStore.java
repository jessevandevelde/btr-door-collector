package dev.jesse.doorcollector.data;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import dev.jesse.doorcollector.model.Direction;
import dev.jesse.doorcollector.model.DoorObservation;

import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class DoorStore {
    private static final int ROOM_CELL_SIZE = 32;
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private final Path file;
    private final Path exportFile;
    private final List<CollectedDoor> doors = new ArrayList<>();
    private double horizontalTolerance = 3;
    private double verticalTolerance = 4;

    public DoorStore(Path root) {
        file = root.resolve("collected-doors.json");
        exportFile = root.resolve("exports").resolve("room-metadata.json");
    }

    public void configure(double horizontal, double vertical) {
        horizontalTolerance = Math.max(0.5, horizontal);
        verticalTolerance = Math.max(0.5, vertical);
    }

    public void load() throws Exception {
        doors.clear();
        if (!Files.exists(file)) return;
        try (Reader reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
            CollectedDoor[] loaded = GSON.fromJson(reader, CollectedDoor[].class);
            if (loaded == null) return;
            for (CollectedDoor door : loaded) {
                if (door != null) consolidate(door);
            }
            if (doors.size() != loaded.length) save();
        }
    }

    public CollectedDoor record(String roomId, DoorObservation observation) throws Exception {
        long now = System.currentTimeMillis();
        CollectedDoor created = CollectedDoor.first(roomId.trim(), observation, now);
        CollectedDoor result = consolidate(created);
        save();
        return result;
    }

    public Path exportMetadata() throws Exception {
        Map<String, List<CollectedDoor>> grouped = new LinkedHashMap<>();
        doors.stream().sorted(Comparator.comparing(CollectedDoor::roomId).thenComparing(d -> d.direction().ordinal()))
            .forEach(door -> grouped.computeIfAbsent(door.roomId(), ignored -> new ArrayList<>()).add(door));
        List<Map<String, Object>> rooms = new ArrayList<>();
        for (var room : grouped.entrySet()) {
            List<Map<String, Object>> possibleDoors = new ArrayList<>();
            for (CollectedDoor door : room.getValue()) {
                Map<String, Object> value = new LinkedHashMap<>();
                value.put("id", door.direction().name().toLowerCase() + "_" + Math.round(door.x()) + "_" + Math.round(door.y()) + "_" + Math.round(door.z()));
                value.put("direction", door.direction().name().toLowerCase());
                value.put("x", (int) Math.round(door.x()));
                value.put("y", (int) Math.round(door.y()));
                value.put("z", (int) Math.round(door.z()));
                value.put("horizontalTolerance", horizontalTolerance);
                value.put("verticalTolerance", verticalTolerance);
                value.put("samples", door.samples());
                value.put("automaticSamples", door.automaticSamples());
                possibleDoors.add(value);
            }
            Map<String, Object> metadata = new LinkedHashMap<>();
            metadata.put("roomId", room.getKey());
            metadata.put("possibleDoors", possibleDoors);
            rooms.add(metadata);
        }
        Files.createDirectories(exportFile.getParent());
        try (Writer writer = Files.newBufferedWriter(exportFile, StandardCharsets.UTF_8)) { GSON.toJson(rooms, writer); }
        return exportFile;
    }

    public void clear() throws Exception { doors.clear(); save(); }
    public List<CollectedDoor> entries() { return List.copyOf(doors); }
    public Path file() { return file; }

    private CollectedDoor consolidate(CollectedDoor candidate) {
        int insertionIndex = doors.size();
        boolean found;
        do {
            found = false;
            for (int i = 0; i < doors.size(); i++) {
                CollectedDoor existing = doors.get(i);
                if (!sameDoor(existing, candidate)) continue;
                insertionIndex = Math.min(insertionIndex, i);
                candidate = existing.merge(candidate);
                doors.remove(i);
                found = true;
                break;
            }
        } while (found);
        doors.add(Math.min(insertionIndex, doors.size()), candidate);
        return candidate;
    }

    private boolean sameDoor(CollectedDoor first, CollectedDoor second) {
        if (!first.roomId().trim().equalsIgnoreCase(second.roomId().trim()) || first.direction() != second.direction()) {
            return false;
        }
        double firstAlong = alongWall(first);
        double secondAlong = alongWall(second);
        double planeDistance = Math.abs(wallPlane(first) - wallPlane(second));
        if (wallSegment(firstAlong) == wallSegment(secondAlong) && planeDistance <= 1) return true;
        return Math.abs(firstAlong - secondAlong) <= horizontalTolerance
            && planeDistance <= 1 && Math.abs(first.y() - second.y()) <= verticalTolerance;
    }

    private double alongWall(CollectedDoor door) {
        return switch (door.direction()) {
            case NORTH, SOUTH -> door.x();
            case EAST, WEST -> door.z();
        };
    }

    private double wallPlane(CollectedDoor door) {
        return switch (door.direction()) {
            case NORTH, SOUTH -> door.z();
            case EAST, WEST -> door.x();
        };
    }

    private long wallSegment(double alongWall) {
        return Math.floorDiv(Math.round(alongWall), ROOM_CELL_SIZE);
    }

    private void save() throws Exception {
        Files.createDirectories(file.getParent());
        Path temporary = file.resolveSibling(file.getFileName() + ".tmp");
        try (Writer writer = Files.newBufferedWriter(temporary, StandardCharsets.UTF_8)) { GSON.toJson(doors, writer); }
        try { Files.move(temporary, file, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE); }
        catch (Exception ignored) { Files.move(temporary, file, StandardCopyOption.REPLACE_EXISTING); }
    }
}
