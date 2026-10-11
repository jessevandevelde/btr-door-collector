package dev.jesse.doorcollector.model;

import java.util.Locale;

public enum Direction {
    NORTH, EAST, SOUTH, WEST;

    public static Direction parse(String value) {
        return valueOf(value.toUpperCase(Locale.ROOT));
    }
}
