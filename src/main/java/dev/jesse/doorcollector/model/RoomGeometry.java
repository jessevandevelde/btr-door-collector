package dev.jesse.doorcollector.model;

import java.util.Optional;
import java.util.Set;

public record RoomGeometry(Int3 anchor, Rotation rotation, int width, int length, Set<RoomCell> cells) {
    private static final int CELL_SIZE = 32;

    public RoomGeometry {
        cells = Set.copyOf(cells);
    }

    public RoomGeometry(Int3 anchor, Rotation rotation, int width, int length) {
        this(anchor, rotation, width, length, rectangularCells(width, length));
    }

    public boolean contains(Int3 local) {
        return local.x() >= 0 && local.x() < width && local.z() >= 0 && local.z() < length
            && cells.contains(cellAt(local));
    }

    public Optional<DoorObservation> crossing(Int3 previousWorld, Int3 currentWorld) {
        Int3 previous = CoordinateTransformer.worldToLocal(previousWorld, anchor, rotation);
        Int3 current = CoordinateTransformer.worldToLocal(currentWorld, anchor, rotation);
        boolean previousInside = contains(previous), currentInside = contains(current);
        if (previousInside == currentInside) return Optional.empty();
        Int3 inside = previousInside ? previous : current;
        Int3 outside = previousInside ? current : previous;
        Direction direction = crossedBoundary(inside, outside);
        return direction == null ? Optional.empty() : Optional.of(new DoorObservation(direction, clampToCellBoundary(inside, direction), true));
    }

    public Optional<DoorObservation> nearestBoundary(Int3 world, Direction requested) {
        Int3 local = CoordinateTransformer.worldToLocal(world, anchor, rotation);
        Candidate best = null;
        for (RoomCell cell : cells) {
            for (Direction direction : Direction.values()) {
                if (requested != null && direction != requested || !isExposed(cell, direction)) continue;
                Int3 position = clampToCellBoundary(local, cell, direction);
                long dx = (long) local.x() - position.x();
                long dz = (long) local.z() - position.z();
                long distance = dx * dx + dz * dz;
                if (best == null || distance < best.distance()) best = new Candidate(direction, position, distance);
            }
        }
        return best == null ? Optional.empty() : Optional.of(new DoorObservation(best.direction(), best.position(), false));
    }

    private Direction crossedBoundary(Int3 inside, Int3 outside) {
        RoomCell cell = cellAt(inside);
        Direction found = null;
        for (Direction direction : Direction.values()) {
            if (!isExposed(cell, direction) || !crossesPlane(inside, outside, cell, direction)) continue;
            if (found != null) return null;
            found = direction;
        }
        return found;
    }

    private boolean crossesPlane(Int3 inside, Int3 outside, RoomCell cell, Direction direction) {
        return switch (direction) {
            case NORTH -> inside.z() >= north(cell) && outside.z() < north(cell);
            case EAST -> inside.x() <= east(cell) && outside.x() > east(cell);
            case SOUTH -> inside.z() <= south(cell) && outside.z() > south(cell);
            case WEST -> inside.x() >= west(cell) && outside.x() < west(cell);
        };
    }

    private Int3 clampToCellBoundary(Int3 local, Direction direction) {
        return clampToCellBoundary(local, cellAt(local), direction);
    }

    private Int3 clampToCellBoundary(Int3 local, RoomCell cell, Direction direction) {
        int x = Math.max(west(cell), Math.min(east(cell), local.x()));
        int z = Math.max(north(cell), Math.min(south(cell), local.z()));
        return switch (direction) {
            case NORTH -> new Int3(x, local.y(), north(cell));
            case EAST -> new Int3(east(cell), local.y(), z);
            case SOUTH -> new Int3(x, local.y(), south(cell));
            case WEST -> new Int3(west(cell), local.y(), z);
        };
    }

    private boolean isExposed(RoomCell cell, Direction direction) {
        RoomCell neighbor = switch (direction) {
            case NORTH -> new RoomCell(cell.x(), cell.z() - 1);
            case EAST -> new RoomCell(cell.x() + 1, cell.z());
            case SOUTH -> new RoomCell(cell.x(), cell.z() + 1);
            case WEST -> new RoomCell(cell.x() - 1, cell.z());
        };
        return !cells.contains(neighbor);
    }

    private RoomCell cellAt(Int3 local) {
        return new RoomCell(Math.floorDiv(local.x(), CELL_SIZE), Math.floorDiv(local.z(), CELL_SIZE));
    }

    private int west(RoomCell cell) { return cell.x() * CELL_SIZE; }
    private int north(RoomCell cell) { return cell.z() * CELL_SIZE; }
    private int east(RoomCell cell) { return Math.min(width - 1, (cell.x() + 1) * CELL_SIZE - 1); }
    private int south(RoomCell cell) { return Math.min(length - 1, (cell.z() + 1) * CELL_SIZE - 1); }

    private static Set<RoomCell> rectangularCells(int width, int length) {
        java.util.LinkedHashSet<RoomCell> result = new java.util.LinkedHashSet<>();
        int cellWidth = Math.floorDiv(width, CELL_SIZE) + 1;
        int cellLength = Math.floorDiv(length, CELL_SIZE) + 1;
        for (int x = 0; x < cellWidth; x++) for (int z = 0; z < cellLength; z++) result.add(new RoomCell(x, z));
        return result;
    }

    private record Candidate(Direction direction, Int3 position, long distance) {}
}
