package dev.jesse.doorcollector;

import dev.jesse.doorcollector.config.ConfigManager;
import dev.jesse.doorcollector.data.CollectedDoor;
import dev.jesse.doorcollector.data.DoorStore;
import dev.jesse.doorcollector.model.Direction;
import dev.jesse.doorcollector.model.DoorObservation;
import dev.jesse.doorcollector.model.Int3;
import dev.jesse.doorcollector.model.RoomGeometry;
import dev.jesse.doorcollector.model.Rotation;
import net.minecraft.client.Minecraft;

import java.nio.file.Path;

public final class CollectorState {
    private final DoorStore store;
    private boolean active;
    private String roomId = "";
    private Rotation rotation = Rotation.NONE;
    private int widthCells = 1;
    private int lengthCells = 1;
    private int physicalNorthWestX;
    private int physicalNorthWestZ;
    private int originY;
    private RoomGeometry geometry;
    private Int3 previousWorld;
    private String lastMessage = "idle";
    private Object levelIdentity;

    public CollectorState(DoorStore store) { this.store = store; }

    public void load() {
        try {
            store.configure(ConfigManager.get().horizontalMergeTolerance, ConfigManager.get().verticalMergeTolerance);
            store.load();
        } catch (Exception e) { DoorCollectorClient.LOG.error("Could not load collected doors", e); }
    }

    public String begin(Minecraft client, Rotation selectedRotation, String selectedRoom) {
        if (client.player == null || selectedRoom == null || selectedRoom.isBlank()) return "join a world and provide a room name";
        roomId = selectedRoom.trim();
        rotation = selectedRotation;
        physicalNorthWestX = RoomGrid.corner(client.player.getX());
        physicalNorthWestZ = RoomGrid.corner(client.player.getZ());
        active = true;
        previousWorld = worldPosition(client);
        rebuildGeometry();
        return "collecting " + roomId + " (rotation " + rotationText() + ", " + widthCells + "x" + lengthCells + " cells)";
    }

    public String setSize(int width, int length) {
        if (width < 1 || width > 6 || length < 1 || length > 6) return "room size must be between 1 and 6 cells";
        widthCells = width;
        lengthCells = length;
        rebuildGeometry();
        return "room size set to " + width + "x" + length + " cells; use /doorcollector origin in its physical north-west cell";
    }

    public String setOrigin(Minecraft client) {
        if (client.player == null) return "join a world first";
        physicalNorthWestX = RoomGrid.corner(client.player.getX());
        physicalNorthWestZ = RoomGrid.corner(client.player.getZ());
        previousWorld = worldPosition(client);
        rebuildGeometry();
        return "physical north-west origin set to " + physicalNorthWestX + ", " + physicalNorthWestZ;
    }

    public String setOriginY(int y) { originY = y; rebuildGeometry(); return "local Y origin set to " + y; }
    public String stop() { active = false; previousWorld = null; return "collection stopped"; }

    public void tick(Minecraft client) {
        if (client.level != levelIdentity) { levelIdentity = client.level; previousWorld = null; }
        if (!active || client.player == null || geometry == null) return;
        Int3 current = worldPosition(client);
        if (previousWorld != null && ConfigManager.get().automaticCapture && closeMovement(previousWorld, current)) {
            geometry.crossing(previousWorld, current).ifPresent(observation -> record(observation, true));
        }
        previousWorld = current;
    }

    public String mark(Minecraft client, Direction direction) {
        if (!active || geometry == null) return "start a room first with /doorcollector begin <rotation> <room name>";
        if (client.player == null) return "join a world first";
        DoorObservation observation = geometry.nearestBoundary(worldPosition(client), direction);
        CollectedDoor door = record(observation, false);
        return door == null ? "could not save door" : formatDoor(door);
    }

    private CollectedDoor record(DoorObservation observation, boolean automatic) {
        DoorObservation evidence = new DoorObservation(observation.direction(), observation.position(), automatic);
        try {
            CollectedDoor door = store.record(roomId, evidence);
            lastMessage = formatDoor(door);
            return door;
        } catch (Exception e) {
            DoorCollectorClient.LOG.error("Could not save door observation", e);
            lastMessage = "save failed";
            return null;
        }
    }

    public String export() {
        try { Path path = store.exportMetadata(); return "exported metadata to " + path; }
        catch (Exception e) { DoorCollectorClient.LOG.error("Could not export metadata", e); return "export failed"; }
    }

    public String clear() {
        try { store.clear(); lastMessage = "data cleared"; return lastMessage; }
        catch (Exception e) { DoorCollectorClient.LOG.error("Could not clear data", e); return "clear failed"; }
    }

    public String status() {
        return (active ? "active room=" + roomId + " rotation=" + rotationText() + " size=" + widthCells + "x" + lengthCells : "inactive")
            + " observations=" + store.entries().stream().mapToInt(CollectedDoor::samples).sum()
            + " doors=" + store.entries().size() + " auto=" + ConfigManager.get().automaticCapture;
    }

    private void rebuildGeometry() {
        int localWidth = widthCells * 32 - 1;
        int localLength = lengthCells * 32 - 1;
        int worldWidth = rotation == Rotation.CLOCKWISE_90 || rotation == Rotation.CLOCKWISE_270 ? localLength : localWidth;
        int worldLength = rotation == Rotation.CLOCKWISE_90 || rotation == Rotation.CLOCKWISE_270 ? localWidth : localLength;
        int anchorX = switch (rotation) {
            case NONE, CLOCKWISE_270 -> physicalNorthWestX;
            case CLOCKWISE_90, CLOCKWISE_180 -> physicalNorthWestX + worldWidth - 1;
        };
        int anchorZ = switch (rotation) {
            case NONE, CLOCKWISE_90 -> physicalNorthWestZ;
            case CLOCKWISE_180, CLOCKWISE_270 -> physicalNorthWestZ + worldLength - 1;
        };
        geometry = new RoomGeometry(new Int3(anchorX, originY, anchorZ), rotation, localWidth, localLength);
    }

    private Int3 worldPosition(Minecraft client) {
        return new Int3((int) Math.floor(client.player.getX()), (int) Math.floor(client.player.getY()), (int) Math.floor(client.player.getZ()));
    }

    private boolean closeMovement(Int3 a, Int3 b) {
        long dx = a.x() - b.x(), dy = a.y() - b.y(), dz = a.z() - b.z();
        return dx * dx + dy * dy + dz * dz <= 25;
    }

    private String formatDoor(CollectedDoor door) {
        return door.roomId() + " " + door.direction().name().toLowerCase() + " @ "
            + Math.round(door.x()) + "," + Math.round(door.y()) + "," + Math.round(door.z()) + " (samples=" + door.samples() + ")";
    }

    private String rotationText() {
        return switch (rotation) { case NONE -> "0"; case CLOCKWISE_90 -> "90"; case CLOCKWISE_180 -> "180"; case CLOCKWISE_270 -> "270"; };
    }

    public boolean active() { return active; }
    public String roomId() { return roomId; }
    public String lastMessage() { return lastMessage; }
    public DoorStore store() { return store; }
}
