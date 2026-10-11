# Hardware Map

Source of truth for the **competition robot's** wiring. Config names here must match
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
- [ ] **Robot footprint**: `auto/RobotSize.java` holds the 18 × 18 maximum, and the TeleOp
      assists' clearance from the HIVE depends on it (docs/decisions.md, 2026-10-11).

## Control Hub

| Port | Type | Config name | Device | Direction | Notes |
|---|---|---|---|---|---|
| Motor 0 | Motor | `frontRightMotor` | drive, front right | FORWARD (verified 2026-09-20) | ⚠ given as `frrontRightMotor` — confirm the spelling in the RC config |
| Motor 1 | Motor | `backRightMotor` | drive, back right | FORWARD (verified 2026-09-20) | |
| Motor 2 | — | | unused | | |
| Motor 3 | Motor | `rightShooter` | shooter flywheel, right | TODO | |
| Servo 0 | Servo | `rightFeeder` | feeder, right | | |
| Servo 1 | Servo | `diverter` | diverter | | |
| Servo 2 | PWM | `light` | indicator light | | TODO device type (e.g. goBILDA RGB indicator) |
| Ethernet | — | `limelight` | Limelight 3A | | The only legal programmable vision coprocessor (R702) |

## Expansion Hub

| Port | Type | Config name | Device | Direction | Notes |
|---|---|---|---|---|---|
| Motor 0 | Motor | `frontLeftMotor` | drive, front left | REVERSE (verified 2026-09-20) | |
| Motor 1 | Motor | `backLeftMotor` | drive, back left | REVERSE (verified 2026-09-20) | |
| Motor 2 | Motor | `leftShooter` | shooter flywheel, left | TODO | |
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

## Odometry (Pinpoint) — tuned 2026-09-20

| Property | Value |
|---|---|
| Pod type | goBILDA 4-bar (assumed — confirm) |
| X (forward) pod offset | −4 23⁄32 in (−4.719 in) |
| Y (strafe) pod offset | −2 11⁄32 in (−2.344 in) |
| Pod directions | X REVERSED, Y FORWARD |

## PedroPathing Foresight constants — tuned 2026-09-20

Units: inches and seconds. Full precision lives in `pedro/Constants.java` (runtime truth); this
table is the backup if that file is ever lost.

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
| Shooter | `leftShooter`, `rightShooter` (flywheels) | | |
| Feeders | `leftFeeder`, `rightFeeder` (servos) | | |
| Diverter | `diverter` (servo) | | |
| Intake | `intake` (motor) | | G407: never CONTROL more than 4 balls — needs a count/stop |
| Light | `light` | | driver / alliance signalling |
| Vision | Limelight 3A (`limelight`) | | AprilTags on CELL undersides — see docs/field.md |

## Devices

- Robot Controller: `<team>-RC` (R705)
- Driver Station: `<team>-DS`
- Control Hub Wi-Fi password changed from default (R711)
