package dev.jesse.doorcollector;

public final class RoomGrid {
    public static final int CELL = 32;
    private RoomGrid() {}

    public static int corner(double coordinate) {
        int shifted = (int) Math.floor(coordinate + 8.5);
        return shifted - Math.floorMod(shifted, CELL) - 8;
    }
}
