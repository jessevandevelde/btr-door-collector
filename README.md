# Better Routes Door Collector

A separate, client-only Fabric mod for Minecraft Java Edition 26.1.2 that gathers doorway evidence while a player runs dungeon rooms. It does not depend on Better Routes, modify Better Routes, automate gameplay, or upload anything.

## Build and install

Requires Java 25, Fabric Loader 0.19.2 or newer, and Fabric API for Minecraft 26.1.2.

```text
gradlew.bat build
```

Copy `build/libs/better-routes-door-collector-1.0.0.jar` into the instance's `mods` directory.

## Collection workflow

1. Stand on the room's **main floor** in its **physical north-west grid cell**.
2. Configure the room, including its canonical rotation and full name:

   ```text
   /doorcollector begin 0 Temple
   ```

   Accepted rotations are `0`, `90`, `180`, and `270` degrees clockwise.
   This captures the dungeon grid origin and makes the current player-feet Y coordinate local `Y=0`. It enters setup mode and does not record yet.
3. For a multi-cell room, set its bounding dimensions and repeat the origin command while standing in its physical north-west cell:

   ```text
   /doorcollector size 2 1
   /doorcollector origin
   ```

4. Rectangular rooms need no shape setup. For an L-shaped or other non-rectangular room, clear the default rectangle, stand in every occupied physical dungeon cell, and add it:

   ```text
   /doorcollector shape clear
   /doorcollector cell add
   ```

   Cell positions are converted into canonical room orientation automatically. Use `/doorcollector cell remove` to correct a mistake.
5. Validate the setup and start recording:

   ```text
   /doorcollector ready
   ```

   The command refuses to start if your position is outside the configured room shape. Any later size, origin, floor, or shape change pauses collection until `ready` is run again.
6. Walk through the doors normally. Entry and exit crossings through exposed room-cell edges are recorded automatically. Ambiguous diagonal corner crossings are ignored instead of guessed.
7. If a crossing is missed, stand near the doorway and run `/doorcollector mark`, or specify the canonical wall direction with `/doorcollector mark north`.
8. Export reviewable Better Routes metadata:

   ```text
   /doorcollector export
   ```

The collector saves canonical room-local coordinates, not the randomized dungeon world coordinates. X/Z are translated from the room's dungeon-grid origin and unrotated; Y is relative to the captured room floor. If setup was started away from the main floor, stand on it and run `/doorcollector floor`. `/doorcollector originy <worldY>` remains available as an explicit override.

## Commands

```text
/doorcollector begin <0|90|180|270> <room name>
/doorcollector size <widthCells> <lengthCells>
/doorcollector origin
/doorcollector floor
/doorcollector originy <y>
/doorcollector shape rectangle
/doorcollector shape clear
/doorcollector cell add
/doorcollector cell remove
/doorcollector ready
/doorcollector mark [north|east|south|west]
/doorcollector auto
/doorcollector status
/doorcollector export
/doorcollector stop
/doorcollector clear
/doorcollector help
```

The manual mark action is also available as an unbound control in Minecraft's Controls screen.

## Output

All data remains under:

```text
config/better-routes-door-collector/
├── config.json
├── collected-doors.json
└── exports/room-metadata.json
```

Each exposed room-cell wall segment and wall plane can contain only one collected doorway. Repeated observations, capitalization differences in room names, and duplicates from older data files are merged automatically while preserving their combined evidence counts. Distinct walls in irregular rooms remain separate doors. Each candidate records total samples, automatic samples, and last-seen time. Exported door objects include the evidence counts as extra JSON fields; Better Routes ignores those extra fields when loading `possibleDoors`.

Review the export before merging it into Better Routes. The standalone collector cannot identify a room or its canonical rotation without fingerprint data, so the supplied room name and rotation must still be correct. Incorrect dimensions, shape, physical north-west origin, or floor produce incorrect local coordinates. Teleports longer than five blocks are ignored to reduce false crossings.

## Privacy and fair play

- No network or upload code exists.
- No movement, clicking, combat, or interaction is automated.
- The mod only observes the local player's position after collection is explicitly started.
- `/doorcollector clear` deletes only this collector's local observation list.
