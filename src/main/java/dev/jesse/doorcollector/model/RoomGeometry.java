package dev.jesse.doorcollector.model;

import java.util.Optional;

public record RoomGeometry(Int3 anchor, Rotation rotation, int width, int length) {
    public boolean contains(Int3 local) {
        return local.x() >= 0 && local.x() < width && local.z() >= 0 && local.z() < length;
    }

    public Optional<DoorObservation> crossing(Int3 previousWorld, Int3 currentWorld) {
        Int3 previous = CoordinateTransformer.worldToLocal(previousWorld, anchor, rotation);
        Int3 current = CoordinateTransformer.worldToLocal(currentWorld, anchor, rotation);
        boolean previousInside = contains(previous), currentInside = contains(current);
        if (previousInside == currentInside) return Optional.empty();
        Int3 outside = previousInside ? current : previous;
        Int3 inside = previousInside ? previous : current;
        Direction direction = boundaryFromOutside(outside);
        return direction == null ? Optional.empty() : Optional.of(new DoorObservation(direction, clamp(inside, direction), true));
    }

    public DoorObservation nearestBoundary(Int3 world, Direction requested) {
        Int3 local = CoordinateTransformer.worldToLocal(world, anchor, rotation);
        Direction direction = requested == null ? nearest(local) : requested;
        return new DoorObservation(direction, clamp(local, direction), false);
    }

    private Direction boundaryFromOutside(Int3 outside) {
        if (outside.z() < 0) return Direction.NORTH;
        if (outside.x() >= width) return Direction.EAST;
        if (outside.z() >= length) return Direction.SOUTH;
        if (outside.x() < 0) return Direction.WEST;
        return null;
    }

    private Direction nearest(Int3 local) {
        int north = Math.abs(local.z());
        int east = Math.abs(width - 1 - local.x());
        int south = Math.abs(length - 1 - local.z());
        int west = Math.abs(local.x());
        int minimum = Math.min(Math.min(north, east), Math.min(south, west));
        if (minimum == north) return Direction.NORTH;
        if (minimum == east) return Direction.EAST;
        if (minimum == south) return Direction.SOUTH;
        return Direction.WEST;
    }

    private Int3 clamp(Int3 local, Direction direction) {
        int x = Math.max(0, Math.min(width - 1, local.x()));
        int z = Math.max(0, Math.min(length - 1, local.z()));
        return switch (direction) {
            case NORTH -> new Int3(x, local.y(), 0);
            case EAST -> new Int3(width - 1, local.y(), z);
            case SOUTH -> new Int3(x, local.y(), length - 1);
            case WEST -> new Int3(0, local.y(), z);
        };
    }
}
