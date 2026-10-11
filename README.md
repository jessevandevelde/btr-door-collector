# Better Routes Door Collector

A separate, client-only Fabric mod for Minecraft Java Edition 26.1.2 that gathers doorway evidence while a player runs dungeon rooms. It does not depend on Better Routes, modify Better Routes, automate gameplay, or upload anything.

## Build and install

Requires Java 25, Fabric Loader 0.19.2 or newer, and Fabric API for Minecraft 26.1.2.

```text
gradlew.bat build
```

Copy `build/libs/better-routes-door-collector-1.0.0.jar` into the instance's `mods` directory.

## Collection workflow

1. Stand in the room's **physical north-west grid cell**.
2. Start the room, including its canonical rotation and full name:

   ```text
   /doorcollector begin 0 Temple
   ```

   Accepted rotations are `0`, `90`, `180`, and `270` degrees clockwise.
3. For a multi-cell room, set its dimensions and repeat the origin command while standing in its physical north-west cell:

   ```text
   /doorcollector size 2 1
   /doorcollector origin
   ```

4. Walk through the doors normally. Entry and exit crossings are recorded automatically.
5. If a crossing is missed, stand in the doorway and run `/doorcollector mark`, or specify the room-local wall explicitly with `/doorcollector mark north`.
6. Export reviewable Better Routes metadata:

   ```text
   /doorcollector export
   ```

The collector defaults to a Y origin of zero, preserving normal dungeon world Y coordinates. Use `/doorcollector originy <y>` when the matching Better Routes fingerprint uses a different local Y origin.

## Commands

```text
/doorcollector begin <0|90|180|270> <room name>
/doorcollector size <widthCells> <lengthCells>
/doorcollector origin
/doorcollector originy <y>
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

Nearby observations for the same room and local wall are merged. Each candidate records total samples, automatic samples, and last-seen time. Exported door objects include the evidence counts as extra JSON fields; Better Routes ignores those extra fields when loading `possibleDoors`.

Review the export before merging it into Better Routes. Incorrect room names, rotation, dimensions, or physical north-west origin will produce incorrect local coordinates. Teleports longer than five blocks are ignored to reduce false crossings.

## Privacy and fair play

- No network or upload code exists.
- No movement, clicking, combat, or interaction is automated.
- The mod only observes the local player's position after collection is explicitly started.
- `/doorcollector clear` deletes only this collector's local observation list.
