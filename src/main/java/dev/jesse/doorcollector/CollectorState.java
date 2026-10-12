package dev.jesse.doorcollector;

import dev.jesse.doorcollector.config.ConfigManager;
import dev.jesse.doorcollector.data.CollectedDoor;
import dev.jesse.doorcollector.data.DoorStore;
import dev.jesse.doorcollector.model.Direction;
import dev.jesse.doorcollector.model.DoorObservation;
import dev.jesse.doorcollector.model.CoordinateTransformer;
import dev.jesse.doorcollector.model.Int3;
import dev.jesse.doorcollector.model.RoomCell;
import dev.jesse.doorcollector.model.RoomGeometry;
import dev.jesse.doorcollector.model.Rotation;
import net.minecraft.client.Minecraft;

import java.nio.file.Path;
import java.util.LinkedHashSet;
import java.util.Set;

public final class CollectorState {
    private final DoorStore store;
    private boolean active;
    private boolean collecting;
    private String roomId = "";
    private Rotation rotation = Rotation.NONE;
    private int widthCells = 1;
    private int lengthCells = 1;
    private int physicalNorthWestX;
    private int physicalNorthWestZ;
    private int originY;
    private final Set<RoomCell> occupiedCells = new LinkedHashSet<>();
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
        widthCells = 1;
        lengthCells = 1;
        physicalNorthWestX = RoomGrid.corner(client.player.getX());
        physicalNorthWestZ = RoomGrid.corner(client.player.getZ());
        originY = (int) Math.floor(client.player.getY());
        active = true;
        collecting = false;
        previousWorld = worldPosition(client);
        fillRectangle();
        rebuildGeometry();
        return "configured " + roomId + " at floor Y=" + originY + "; set size/origin/shape, then run /doorcollector ready";
    }

    public String setSize(int width, int length) {
        if (width < 1 || width > 6 || length < 1 || length > 6) return "room size must be between 1 and 6 cells";
        widthCells = width;
        lengthCells = length;
        collecting = false;
        fillRectangle();
        rebuildGeometry();
        return "room size set to " + width + "x" + length + " cells; collection paused until /doorcollector ready";
    }

    public String setOrigin(Minecraft client) {
        if (client.player == null) return "join a world first";
        physicalNorthWestX = RoomGrid.corner(client.player.getX());
        physicalNorthWestZ = RoomGrid.corner(client.player.getZ());
        collecting = false;
        previousWorld = worldPosition(client);
        rebuildGeometry();
        return "physical north-west origin set to " + physicalNorthWestX + ", " + physicalNorthWestZ + "; run /doorcollector ready";
    }

    public String setOriginY(int y) {
        originY = y;
        collecting = false;
        rebuildGeometry();
        return "room floor world Y set to " + y + "; run /doorcollector ready";
    }

    public String setFloor(Minecraft client) {
        if (client.player == null) return "join a world first";
        return setOriginY((int) Math.floor(client.player.getY()));
    }

    public String rectangleShape() {
        collecting = false;
        fillRectangle();
        rebuildGeometry();
        return "shape reset to a full " + widthCells + "x" + lengthCells + " rectangle; run /doorcollector ready";
    }

    public String clearShape() {
        collecting = false;
        occupiedCells.clear();
        rebuildGeometry();
        return "shape cleared; stand in each occupied room cell and run /doorcollector cell add";
    }

    public String changeCurrentCell(Minecraft client, boolean add) {
        if (!active) return "configure a room first with /doorcollector begin";
        if (client.player == null) return "join a world first";
        int physicalX = Math.floorDiv(RoomGrid.corner(client.player.getX()) - physicalNorthWestX, RoomGrid.CELL);
        int physicalZ = Math.floorDiv(RoomGrid.corner(client.player.getZ()) - physicalNorthWestZ, RoomGrid.CELL);
        RoomCell local = physicalToLocalCell(physicalX, physicalZ);
        if (local.x() < 0 || local.x() >= widthCells || local.z() < 0 || local.z() >= lengthCells) {
            return "current cell is outside the configured room bounds; check size and north-west origin";
        }
        collecting = false;
        if (add) occupiedCells.add(local); else occupiedCells.remove(local);
        rebuildGeometry();
        return (add ? "added" : "removed") + " canonical cell " + local.x() + "," + local.z()
            + " (occupied=" + occupiedCells.size() + "); run /doorcollector ready when finished";
    }

    public String ready(Minecraft client) {
        if (!active || geometry == null) return "configure a room first with /doorcollector begin";
        if (client.player == null) return "join a world first";
        if (occupiedCells.isEmpty()) return "room shape has no occupied cells";
        Int3 local = CoordinateTransformer.worldToLocal(worldPosition(client), geometry.anchor(), rotation);
        if (!geometry.contains(local)) return "you are outside the configured room shape; check rotation, size, origin, and cells";
        collecting = true;
        previousWorld = worldPosition(client);
        lastMessage = "ready; local=" + local.x() + "," + local.y() + "," + local.z();
        return "collecting " + roomId + " with local floor Y=0, rotation " + rotationText()
            + ", " + occupiedCells.size() + " occupied cells";
    }

    public String stop() { active = false; collecting = false; previousWorld = null; return "collection stopped"; }

    public void tick(Minecraft client) {
        if (client.level != levelIdentity) { levelIdentity = client.level; previousWorld = null; }
        if (!collecting || client.player == null || geometry == null) return;
        Int3 current = worldPosition(client);
        if (previousWorld != null && ConfigManager.get().automaticCapture && closeMovement(previousWorld, current)) {
            geometry.crossing(previousWorld, current).ifPresent(observation -> record(observation, true));
        }
        previousWorld = current;
    }

    public String mark(Minecraft client, Direction direction) {
        if (!collecting || geometry == null) return "configure the room and run /doorcollector ready first";
        if (client.player == null) return "join a world first";
        DoorObservation observation = geometry.nearestBoundary(worldPosition(client), direction).orElse(null);
        if (observation == null) return "no exposed boundary matches that direction";
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
        return (active ? (collecting ? "collecting" : "configured") + " room=" + roomId + " rotation=" + rotationText()
            + " size=" + widthCells + "x" + lengthCells + " cells=" + occupiedCells.size() + " floorWorldY=" + originY : "inactive")
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
        geometry = new RoomGeometry(new Int3(anchorX, originY, anchorZ), rotation, localWidth, localLength, occupiedCells);
    }

    private void fillRectangle() {
        occupiedCells.clear();
        for (int x = 0; x < widthCells; x++) for (int z = 0; z < lengthCells; z++) occupiedCells.add(new RoomCell(x, z));
    }

    private RoomCell physicalToLocalCell(int physicalX, int physicalZ) {
        return switch (rotation) {
            case NONE -> new RoomCell(physicalX, physicalZ);
            case CLOCKWISE_90 -> new RoomCell(physicalZ, lengthCells - 1 - physicalX);
            case CLOCKWISE_180 -> new RoomCell(widthCells - 1 - physicalX, lengthCells - 1 - physicalZ);
            case CLOCKWISE_270 -> new RoomCell(widthCells - 1 - physicalZ, physicalX);
        };
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
    public boolean collecting() { return collecting; }
    public String roomId() { return roomId; }
    public String lastMessage() { return lastMessage; }
    public DoorStore store() { return store; }
}
