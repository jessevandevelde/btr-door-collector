package dev.jesse.doorcollector.model;

public record DoorObservation(Direction direction, Int3 position, boolean automatic) {}
