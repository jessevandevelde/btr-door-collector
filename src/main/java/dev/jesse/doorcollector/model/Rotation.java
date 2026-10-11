package dev.jesse.doorcollector.model;

public enum Rotation {
    NONE, CLOCKWISE_90, CLOCKWISE_180, CLOCKWISE_270;

    public static Rotation parse(String value) {
        return switch (value) {
            case "0", "none" -> NONE;
            case "90", "cw90" -> CLOCKWISE_90;
            case "180", "cw180" -> CLOCKWISE_180;
            case "270", "cw270" -> CLOCKWISE_270;
            default -> throw new IllegalArgumentException("rotation must be 0, 90, 180, or 270");
        };
    }
}
