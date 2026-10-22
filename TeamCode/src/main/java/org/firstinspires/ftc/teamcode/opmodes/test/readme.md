# opmodes/test/

Checks for the competition robot (group `"test"`). Not for matches. Config names come from
`RobotHardware`. Simple checks are plain `OpMode`s that read the gamepad directly, like the
StarterBot OpMode. One that needs driving or assists, like the vision bench, uses the TeleOp engine
and has its own control scheme in `teleop/schemes/`.

| OpMode | What it checks | How |
|---|---|---|
| `Test: Drive Motors` | each drive motor is in the port and config name docs/hardware.md says, and spins forward | robot on blocks; hold gp1 X = front left, Y = front right, A = back left, B = back right (power 0.3). Only that wheel should spin, forward. Telemetry shows each motor's power, encoder count, and the port the SDK found next to the expected one |
| `Test: Ball Vision Bench` | the Limelight and Hive-Vision's ball model, and the camera mount numbers in `VisionTuning` | see below |

## Test: Ball Vision Bench (`BallVisionBench`)

Checks the Limelight and Hive-Vision's ball model, and calibrates the mount numbers in
`subsystems/VisionTuning` (live in Panels on a dev build; copy good values back into the file and
into docs/hardware.md).

Before starting:
- The model and labels are uploaded to a Neural Detector pipeline in the slot
  `VisionTuning.ballPipeline` (docs/hardware.md).
- The DS "vision" line says `… fps · …`. If it says `waiting for pipeline N`, the pipeline index
  is wrong.

For each detection the DS shows its kind and confidence, the image angles, where it puts the ball
(inches ahead of and left of the robot's tracking centre), and a pitch check. It also lists each
tracked ball with its field position, whether it's confirmed, and its *spread*: how far apart its
sightings have been.

1. **Pitch.** Put a ball straight ahead of the lens. Measure along the floor from the point
   under the lens to the ball's centre: 12, 18 or 24 in. The "pitch if" line gives the pitch the
   mount would need for that distance. Set `VisionTuning.pitchDown` to the average over a few
   distances.
2. **Distance and side.** With the pitch set, put balls at known spots. "in ahead, left" should
   match the tape measure within about 1 in. If side-to-side is off by the same amount
   everywhere, fix `left`; if it grows with distance, fix `yaw`.
3. **Latency: the spin test.** B forgets the tracked balls. With one ball in view, turn the robot
   slowly back and forth in place. The ball's spread should stay under about 2 in. If it grows
   with turning speed, raise `extraMs` until it's smallest.
4. **Box size.** With one ball in view at a few distances, read "area … ≈ N balls". It should
   say about 1. Set `VisionTuning.blobAreaScale` to the average N, so one ball reads 1.0. Then
   put three balls touching: the tracked ball should show "×3" (or three separate balls).
5. **Driving.** Hold A: the robot drives to the nearest legal ball and stops `standoff` short,
   facing it. Moving a stick cancels. There's no own-half rule here (TeleOp's rules).

The start pose is (12, 24, 0°): red half, audience end, front toward the blue wall. Put the robot
there on a field to read real field positions. Off the field, only the robot-relative numbers mean
anything.
