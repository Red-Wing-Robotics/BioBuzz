# vision/

Finding balls with the Limelight 3A and driving to them. Hardware-free and unit-tested
(`TeamCode/src/test/java/.../vision/`): the `Vision` subsystem (`subsystems/Vision`) is the only
code that touches the SDK's Limelight types, and turns each camera frame into the `Frame`s used
here. Not compiled by Jimothy, and nothing in `auto/` uses it.

Built on [Hive-Vision](https://github.com/hivevisionftc/Hive-Vision) (MIT, Copyright 2026 Harjas
Sidhu; see THIRD_PARTY_NOTICES.md). From it we use the **ball model**, which runs on the camera,
and its **tracking ideas**: the confidence gate, distinct-frame confirmation, colour-bound
matching, and ground-range geometry, adapted in `BallTracker` and `CameraMount`. We don't use
its driving classes (`BallHunt`, `BallWrangler`, `BallChaseFollower`): they block the loop, sleep,
and target Pedro 2. Driving here is a `Step` on our engine. docs/decisions.md (2026-10-15) has
the reasons.

## How it works

1. **The camera** runs the model in a Neural Detector pipeline. Each detection is a class
   (`yellow_pollen`, `red_nectar`, `blue_nectar`) and the angles from the image centre to it.
2. **`CameraMount`** turns those angles into a point on the field. The ray through the lens is cut
   at the ball's centre height (`BallKind`: 1.4 in for POLLEN, 1.8 in for NECTAR), from the mount
   measured in docs/hardware.md.
3. **`BallTracker`** keeps the balls as field positions. Each new frame is placed from where the
   robot was when it was taken (`PoseHistory`; the camera's latency is subtracted). A ball is
   *confirmed* after 3 consecutive frames, and remembered once out of view, which is how an
   approach finishes after the ball drops below the image.
4. **`DriveToBall`** (a `Step`) picks the legal ball it can reach soonest (`BallApproach`,
   `PickupRules`), unless a bigger group of balls is about as quick to reach. Balls within
   `clusterRadius` (6 in) of each other count as a group, and a group wins if it's at most
   `similarSeconds` (0.5 s, about 15–20 in) slower. It then goes for the group's biggest blob (the box
   with the most balls in it, not a lone ball beside it), then the quicker, and stops facing
   through it at the rest. The DS "vision pick" line lists every ball it weighed. A route can turn this off with `"prefer": "nearest"`. Touching balls often come from the
   model as one box, so each tracked ball also has a *blob* count: its box's area over the area
   one ball should have at that range (`CameraMount.ballAreaPct`, scaled by
   `VisionTuning.blobAreaScale`). A box three balls big counts as three. It
   drives to a stop pose `standoff` short of it (9 in, centre to centre: the
   front edge touches the ball), re-plans as the estimate firms up, and freezes the target close
   in. If the robot can't stop straight on, as with a ball against a wall seen at an angle, the
   approach swings round the ball, up to 90° either way, until the robot fits. With no ball in sight it finishes at once (NO_TARGET),
   so a route carries on.

## Rules it enforces (`PickupRules`)

- Never opponent NECTAR (G408).
- Never a ball under the HIVE (G409).
- In AUTO: only balls on our half, and the robot stops entirely on our half (G402).
- G407 (at most 4 elements) belongs to the intake, which isn't built yet.

## Using it

- **In a route:** `{ "seek": "vision.ball", "args": { "colors": "pollen" }, "timeout": 4 }`, then
  a `driveTo` back to a waypoint. The auto registers `r.seek("vision.ball", vision::toBall)` and,
  if it waits on sight, `r.condition("vision.seesPollen", …)` (see `opmodes/auto/VisionSeekAuto`).
- **In TeleOp:** an assist whose factory is
  `vision.driveToBall(vision.teleOpSettings(obstacles, margin))` (see `opmodes/test/BallVisionBench`).
- **Tuning:** `subsystems/VisionTuning` (Panels in dev builds). Calibrate on the bench first:
  `opmodes/test/readme.md`.

## What the camera can see

With the mount measured on 2026-10-15, the image covers ball centres from about 4.5 to 29 in
ahead of the lens. The tracker uses only **5 to 25 in**: it ignores the 2° next to each edge,
where balls may be cut off. Vision covers only the last two feet or so: a route drives to a
waypoint that puts the expected balls in that band, then seeks. To check a spot, park the robot
there in the vision bench. Each detection shows "(edge: not used)" if it's in the margin, and the
DS "vision frame" line says what happened to every detection in the latest frame.

When a vision drive ends, its result is on the DS ("vision drive") and in the robot log (tag
`Vision`). A seek that finds nothing says why: no ball confirmed, or what ruled out the nearest
one.
