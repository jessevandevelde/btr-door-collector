package dev.jesse.doorcollector.model;

public final class CoordinateTransformer {
    private CoordinateTransformer() {}

    public static Int3 localToWorld(Int3 local, Int3 anchor, Rotation rotation) {
        return switch (rotation) {
            case NONE -> new Int3(anchor.x() + local.x(), anchor.y() + local.y(), anchor.z() + local.z());
            case CLOCKWISE_90 -> new Int3(anchor.x() - local.z(), anchor.y() + local.y(), anchor.z() + local.x());
            case CLOCKWISE_180 -> new Int3(anchor.x() - local.x(), anchor.y() + local.y(), anchor.z() - local.z());
            case CLOCKWISE_270 -> new Int3(anchor.x() + local.z(), anchor.y() + local.y(), anchor.z() - local.x());
        };
    }

    public static Int3 worldToLocal(Int3 world, Int3 anchor, Rotation rotation) {
        int dx = world.x() - anchor.x(), dy = world.y() - anchor.y(), dz = world.z() - anchor.z();
        return switch (rotation) {
            case NONE -> new Int3(dx, dy, dz);
            case CLOCKWISE_90 -> new Int3(dz, dy, -dx);
            case CLOCKWISE_180 -> new Int3(-dx, dy, -dz);
            case CLOCKWISE_270 -> new Int3(-dz, dy, dx);
        };
    }
}
