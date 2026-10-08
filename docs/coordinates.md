# Field coordinates

**This is the one definition.** Every pose in code, route files, Jimothy and the other docs uses
it, and they link here instead of restating it. The numbers live in one class,
[`auto/Field.java`](../TeamCode/src/main/java/org/firstinspires/ftc/teamcode/auto/Field.java).

## The frame

Pedro Pathing's field coordinates, with the BIOBUZZ field the way Pedro's visualizer draws it:
red alliance on the left, audience at the bottom, seen from above.

```
  (0, 144) ┌──────────────── far wall ────────────────┐ (144, 144)
           │                                     ▒▒▒▒▒│ blue GARDEN
           │▓                                         │
   RED     │▓ red LOADING ZONE                        │    BLUE
 ALLIANCE  │▓                                         │  ALLIANCE
   AREA    │                   HIVE                   │    AREA
           │                 (72, 72)                ▓│
           │                       blue LOADING ZONE ▓│
           │                                         ▓│
    (0, 0) └▒▒▒▒▒─────────── audience wall ───────────┘ (144, 0)
        red GARDEN              AUDIENCE

  +y ↑   → +x        heading: 0° = +x, 90° = +y, counter-clockwise positive
```

- **Origin (0, 0):** the corner by the red GARDEN, where the red alliance wall meets the audience
  wall.
- **+x** runs from the red alliance wall toward the blue one; **+y** from the audience toward the
  far wall.
- **Size:** 144 × 144 in ([`Field.SIZE`](#field-size-144-and-where-1415-comes-from)), centre
  (72, 72) (`Field.CENTER`). Units are inches, which is what odometry measures.
- **Heading** is the way the robot's **front** faces. It's in degrees in route files and
  `Waypoint`s, and in radians inside Pedro. **0°** faces +x (the blue wall), **90°** +y (the far
  wall), **180°** the red wall, **−90° / 270°** the audience. Counter-clockwise is positive.
- **Drivers' view:** red drivers stand beyond x = 0 facing +x, so **their left is +y** (the far
  wall) and their right is −y (the audience). Blue drivers face −x, so their left is −y.

## RED frame, BLUE, and the alliance frame

- **RED frame.** Everything we write once for both alliances is written for red, in the frame
  above: route-file waypoints, `Waypoint` constants (`ShootingSpots`, TeleOp start waypoints).
- **BLUE** is the red pose rotated 180° about the centre: (x, y, h) → (144 − x, 144 − y,
  h + 180°). Code: `Alliance`. BIOBUZZ is rotationally symmetric: in the visualizer image, every
  red element (GARDEN, LOADING ZONE, FLOWERs, the starting NECTAR's CELL) lands on its blue twin
  under that rotation. A left/right mirror (`mirrorX`) is **wrong** for BIOBUZZ, and that's what
  the visualizer's "mirror" code export writes.
- **Alliance frame:** a pose after that mapping. It's what the Follower is given and reports, and
  what the DS "pose" line shows. Field-fixed things, like the HIVE in `Field` or any `KeepOut`,
  are written directly in this frame. They are the same for both alliances and are never
  mirrored.

## How the robot knows where it is

Odometry only measures movement. The frame comes from the pose the robot is **told at INIT**:

- in AUTO, the route's START;
- in TeleOp, the pose AUTO ended at if it stopped within 30 s, otherwise the TeleOp's start
  waypoint.

Set the robot down exactly there, facing the way the heading says. A robot facing a different
wall than its start pose says has its whole frame rotated by that much, and every field position
lands rotated too. That was the TeleOp shooting-spot bug of 2026-10-04 (`docs/decisions.md`).

## Field size: 144, and where 141.5 comes from

The sources disagree (checked 2026-10-04):

| Source | Field | Centre |
|---|---|---|
| Game manual: inside of the walls, 36 tiles of ≈ 24 in | ≈ 144 in | |
| [Pedro docs](https://pedropathing.com/docs/pathing/reference/coordinates): FTC → Pedro conversion; Pedro 2.x `PedroCoordinates` javadoc | 144 | (72, 72) |
| [Pedro visualizer](https://visualizer.pedropathing.com) (`assets/index.js`) | 141.5 | (70.75, 70.75) |

**We use 144**, Pedro's documented system and the manual's nominal size. What follows from that:

- **Field geometry** (`Field.HIVE`, `automaps/BiobuzzField`) is measured from the visualizer's
  BIOBUZZ image (`/fields/biobuzz.webp`, 1080 px square, the whole field) in **pixels**. It's
  converted with `Field.fromImage(px) = px × SIZE / 1080`, so it follows `Field.SIZE`.
- **Numbers read straight off visualizer.pedropathing.com** are on its 141.5 scale. Multiply them
  by 144 / 141.5 (≈ 1.018), or read them in Jimothy, which uses this frame.
- **Real fields are only "approximately" 144 in.** If ours measures differently inside the walls,
  change `Field.SIZE`; the BLUE rotation, the HIVE and the maps all follow. Until then, positions
  far from the wall the robot starts against are off by up to the difference.
- Pedro 2.x's `PedroCoordinates` javadoc says heading 0 "faces the red alliance wall". That
  describes another season's field image. For BIOBUZZ, 0° faces the blue wall.

## Converting from other frames

AprilTag, Limelight and VisionPortal poses use the FTC standard field frame, which has its origin
at the field centre. Pedro's documented conversion, in inches, is: x = y<sub>ftc</sub> + 72,
y = 72 − x<sub>ftc</sub>, heading = heading<sub>ftc</sub> − 90°. Use `Field.CENTER` for the 72.
It was written for DECODE's field, so check it against a known tag on the real field before
trusting it.
