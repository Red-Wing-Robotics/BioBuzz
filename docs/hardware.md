# Hardware Map

Source of truth for the **competition robots'** wiring. We have two, **Belteshazzar** and
**Ashurbanipal**. They share this port map, the config names and the motor directions; only the
Pinpoint pod offsets and the Pedro tuning differ (sections below). Each Control Hub says which
robot it is in `/sdcard/FIRST/robot.txt` (`RobotId.java`; set it with the `Robot ID: ...`
OpModes). Config names here must match
`RobotHardware.java` **and** the Robot Controller configuration exactly. When you change one,
change all three. The goBILDA StarterBot is a separate robot: its names are in
`opmodes/starterbot/StarterBotHardware.java`.

Actuator budget (R503): **8 motors / 8 servos** max. In use: 7 motors, 3 servos (+ a PWM light on
a servo port — confirm with the LRI whether it counts as a servo).

## Still to confirm on the robot

No test can catch these. Tick each one here, and fix the table row or code it names.

- [ ] **`frontRightMotor` spelling** in the RC config (it was given as `frrontRightMotor`). If it
      doesn't match, every OpMode stops at INIT: "unable to find a hardware device".
- [ ] **`pinpoint` config name** (assumed). Same failure.
- [ ] **Odometry pod type**: 4-bar is assumed (`pedro/Constants.java`, TODO). 4-bar and swingarm
      pods differ by about 1.5× in ticks per mm, so a wrong type scales every distance. To check,
      push the robot a measured 48 in and read the DS "pose" line.
- [ ] **Drive type**: mecanum is assumed (the Mecanum Tuner ran on 2026-09-20).
- [ ] **PWM light and R503**: ask the LRI whether it counts as a servo.
- [ ] **Ashurbanipal pod offsets and tuning**: measure the pod offsets into `pedro/Ashurbanipal.java`,
      run the Pinpoint and Foresight tuners on it, and set `TUNED = true`. Until then its paths
      use Belteshazzar's values.
- [ ] **Robot footprint**: `auto/RobotSize.java` holds the 18 × 18 maximum, and the TeleOp
      assists' clearance from the HIVE depends on it (docs/decisions.md, 2026-10-11).
- [ ] **Limelight pitch, checked optically**: put a ball straight ahead at measured floor
      distances `d` from the point under the lens and read `ty`. Each reading gives
      pitch = ty + atan((7.75 − ball centre height) / d), and they should average about 33.3°.
- [ ] **Ball sizes**: caliper a real POLLEN and NECTAR. The Limelight section and `vision/BallKind`
      assume 2.8 in and 3.6 in.
- [ ] **Limelight pipelines**: upload Hive-Vision's v1.1.0 model and labels to a Neural Detector
      pipeline (CPU, threshold about 0.40), set up an AprilTag pipeline, and record both slots in
      the Limelight section and `VisionTuning`.
- [ ] **Ball model on the camera** (the vision bench, `opmodes/test/readme.md`): frame rate,
      bumpers or tape read as NECTAR, the confidence scale, then the latency spin test
      (`VisionTuning.extraMs`).

## Control Hub

| Port | Type | Config name | Device | Direction | Notes |
|---|---|---|---|---|---|
| Motor 0 | Motor | `frontRightMotor` | drive, front right | FORWARD (verified 2026-09-20) | ⚠ given as `frrontRightMotor` — confirm the spelling in the RC config |
| Motor 1 | Motor | `backRightMotor` | drive, back right | FORWARD (verified 2026-09-20) | |
| Motor 2 | — | | unused | | |
| Motor 3 | Motor | `topShooter` | shooter flywheel, top (was `rightShooter`) | TODO | |
| Servo 0 | Servo | `rightFeeder` | feeder, right | | |
| Servo 1 | Servo | `diverter` | diverter | | |
| Servo 2 | PWM | `light` | indicator light | | TODO device type (e.g. goBILDA RGB indicator) |
| USB (Ethernet over USB) | — | `limelight` | Limelight 3A | | The only legal programmable vision coprocessor (R702). Mount and pipelines: "Limelight 3A mount" |

## Expansion Hub

| Port | Type | Config name | Device | Direction | Notes |
|---|---|---|---|---|---|
| Motor 0 | Motor | `frontLeftMotor` | drive, front left | REVERSE (verified 2026-09-20) | |
| Motor 1 | Motor | `backLeftMotor` | drive, back left | REVERSE (verified 2026-09-20) | |
| Motor 2 | Motor | `bottomShooter` | shooter flywheel, bottom (was `leftShooter`) | TODO | |
| Motor 3 | Motor | `intake` | intake | TODO | |
| Servo 0 | Servo | `leftFeeder` | feeder, left | | |
| I2C bus 2 | I2C | `pinpoint` | goBILDA Pinpoint odometry computer | | config name assumed — confirm |

## Drivetrain

| Property | Value |
|---|---|
| Drive type | mecanum (assumed from the 4 corner motors — confirm) |
| Wheel diameter | TODO |
| Motor | TODO (model, gear ratio, ticks/rev) |
| Track width | TODO |
| Motor directions | FL REVERSE, BL REVERSE, FR FORWARD, BR FORWARD — verified with the Mecanum Tuner 2026-09-20 |

## Odometry (Pinpoint)

| Property | Belteshazzar (2026-09-20) | Ashurbanipal |
|---|---|---|
| Pod type | goBILDA 4-bar (assumed — confirm) | same (assumed) |
| X (forward) pod offset | −4 23⁄32 in (−4.719 in) | TODO measure (differs; Belteshazzar's value as placeholder) |
| Y (strafe) pod offset | −2 11⁄32 in (−2.344 in) | TODO measure (differs; Belteshazzar's value as placeholder) |
| Pod directions | X REVERSED, Y FORWARD | same |

## Limelight 3A mount — measured 2026-10-15

Measured from the robot's tracking centre (the point the Pinpoint pod offsets use), on a field
tile. Forward is the launcher side; left is positive. Hive-Vision's names for the same values are
in brackets.

| Property | Value |
|---|---|
| Lens height above the tiles | 7 3⁄4 in (7.75) `[CAM_H]` |
| Pitch, below horizontal | 33.3° `[CAM_PITCH_DEG]` |
| Forward offset | 6 3⁄4 in (6.75) `[CAM_X_OFFSET]` |
| Left offset | 6 1⁄8 in (6.125) `[CAM_Y_OFFSET]` |
| Yaw / roll | 0° / 0° |
| Nearest visible floor (measured) | 4 in |
| Field of view (spec) | 54.5° × 42° |
| Ball pipeline (Hive-Vision model, Neural Detector) | slot 0, **placeholder** `[VisionTuning.ballPipeline]` |
| AprilTag pipeline | slot 1, **placeholder** `[VisionTuning.tagPipeline]` |

Runtime truth: `subsystems/VisionTuning.java`. Change a value there and here together.

What it sees, worked out from these numbers (POLLEN centre 1.4 in, NECTAR 1.8 in up):
- **Ball centres from about 4.5 in to 29 in ahead of the lens** (NECTAR to 27 in), across about
  8 in at the near edge and 28 in at the far edge. Beyond about 36 in from the tracking centre,
  balls are out of view.
- **Usable: about 5 in to 25 in ahead of the lens.** The tracker ignores balls within 2° of an
  image edge (`VisionTuning.edgeMargin`), where they may be cut off. Put a seek's start waypoint
  so the balls you expect are in that band, and not at its far end. The first vision-seek test
  (2026-10-15) stopped 26 in from the GARDEN balls: the camera saw them at the top edge, every one
  was ignored, and the seek gave up. The DS "vision frame" line shows this as "… at the image edge".
- **Off-centre:** the lens is 6 in left of the centreline, so a ball dead ahead of the robot
  only comes into view about 17 in ahead of the tracking centre. Closer than that it's out of
  frame to the right.
- **Range error per degree of pitch error:** 0.5 in at 12 in, 1 in at 18 in, 2.4 in at the far
  edge.
- **Nothing above lens height is ever in view:** the top of the image is 12° below horizontal.
  This mount can't read the CELL-underside AprilTags.

## PedroPathing Foresight constants

**Belteshazzar, tuned 2026-09-20.** Ashurbanipal is **not tuned yet**: it runs on these values
as placeholders, and its INIT telemetry says so. Units: inches and seconds. Full precision lives
in `pedro/Belteshazzar.java` / `pedro/Ashurbanipal.java` (runtime truth); this table is the
backup if those files are ever lost.

| Constant | Value |
|---|---|
| forwardTranslational | piecewise P: 0.05211 below 2.5, 0.14104 above |
| strafeTranslational | piecewise P: 0.07662 below 2.5, 0.20739 above |
| coast | proportional feedforward 0.015097 |
| brake | proportional feedforward 0.012833 |
| headingFeedback | P 3.1481 |
| headingBrakeCoefficients | (0.040507, 0.0072420) |
| linearBrakeCoefficients | diag(0.066028, 0.066922) |
| quadraticBrakeCoefficients | diag(0.0010080, 0.00085377) |
| maxAchievableForwardVelocity | 69.649 |
| maxAchievableStrafeVelocity | 59.057 |
| naturalForwardDeceleration | 37.227 |
| naturalStrafeDeceleration | 51.941 |

## Mechanisms

| Mechanism | Actuators | Sensors | Notes |
|---|---|---|---|
| Drivetrain | 4 drive motors | Pinpoint | |
| Shooter | `topShooter`, `bottomShooter` (flywheels, velocity control) | motor encoders | `subsystems/Shooter` |
| Feeders | `leftFeeder`, `rightFeeder` (servos) | | |
| Diverter | `diverter` (servo) | | |
| Intake | `intake` (motor) | | G407: never CONTROL more than 4 balls — needs a count/stop |
| Light | `light` | | driver / alliance signalling |
| Vision | Limelight 3A (`limelight`) | | Floor balls (Hive-Vision; `subsystems/Vision`, `vision/`). The current mount can't see the CELL-underside AprilTags (docs/field.md): see "Limelight 3A mount" |

## Devices

- Robot Controller: `<team>-RC` (R705)
- Driver Station: `<team>-DS`
- Control Hub Wi-Fi password changed from default (R711)
