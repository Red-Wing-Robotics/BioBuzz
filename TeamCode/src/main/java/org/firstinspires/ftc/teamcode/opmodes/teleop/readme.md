# opmodes/teleop/

TeleOp OpModes, built on the engine in `teleop/` (see its readme). `CompTeleOp` is the competition
TeleOp. It sets the start waypoint, defines the assists in `registerAssists`, turns
`DriverCommands` into subsystem calls in `apply`, and registers its variants as nested classes.

| OpMode | Group | Scheme | Notes |
|---|---|---|---|
| `Red: TeleOp` / `Blue: TeleOp` | comp | `TwoDriverScheme` | preselected by the autos of that alliance |
| `Red: Aim TeleOp` / `Blue: Aim TeleOp` | comp | `AimScheme` | the match layout plus heading aim (below); not preselected by any auto |
| `Red: Aim Shooter TeleOp` / `Blue: Aim Shooter TeleOp` | comp | `AimShooterScheme` | Aim TeleOp plus flywheel speed control and the shot log (below); not preselected |
| `Solo: TeleOp` | test | `SoloScheme` | one person, practice; B = drive back to start |

Assists (defined in `CompTeleOp.registerAssists`, so every variant has them):

| Assist | Button | Does |
|---|---|---|
| left / right shooting spot | gp1 d-pad ← / → (both schemes) | drives to `ShootingSpots.left` (far end of the HIVE) / `.right` (audience end) and holds there until a drive stick moves or the button is pressed again |
| return to start | gp1 B (Solo only) | drives to the start waypoint |

**Heading aim** (Aim TeleOps only, gp1 X / B): press to keep the robot's front pointed at our
HIVE's left / right CELL centre (`HiveCells`, placeholder, live-editable in Panels); press again
to stop, or the other button to switch. Forward and strafe work as usual; the turn stick does
nothing while it is on. It pauses while a driving assist runs and resumes after. Gains:
`teleop/HeadingAim`, untuned. The DS "aim" line shows the target and how far off the heading is.

**Shooter** (`subsystems/Shooter`, every variant): the top and bottom flywheels run together
under velocity control. They start at `Shooter.DEFAULT_VELOCITY` (1500 ticks/s) and keep the
last target while off, capped at `Shooter.MAX_VELOCITY`. In the Aim Shooter TeleOps, gp2 right
bumper toggles them and d-pad up / down changes the speed by `AimShooterScheme.VELOCITY_STEP`
(100 ticks/s); the other schemes toggle on gp2 Y at the default speed. The DS "shooter velocity"
line shows the target and both measured speeds. Motor directions are unverified (TODO in
`Shooter`).

**Shot log** (Aim Shooter TeleOps only): after a shot, the operator presses **gp2 A** if it
scored or **gp2 X** if it missed. That appends one row to `/sdcard/FIRST/data/shots.csv` on the
hub (header written when the file is new; rows from every session accumulate): time, match time
left, robot, alliance, CELL, whether aim was on, pose, **distance to the CELL** (the aimed-at one,
else the nearer; floor distance from robot centre to `HiveCells` centre, inches), target and
measured top / bottom flywheel speeds, aim error, hit / miss. gp2 gives one rumble blip and the DS
"shot log" line confirms it; a long rumble means the write failed (the DS line says why). Copy it
off with `adb pull /sdcard/FIRST/data/shots.csv`, then plot distance vs target velocity for the
hits. Distances are only as good as the odometry pose and the `HiveCells` placeholders: measure
those first. If the columns ever change, rename the old file so the header stays right. Writing a
file on the hub is not Wi-Fi logging (R704).

Every driving assist takes the **quickest way that keeps the whole robot clear of the HIVE's
floor rails**, round the HIVE or through the lane under it: `Steps.driveTo` with
`Field.HIVE_FLOOR` and `HiveClearance.margin` (8.3 in, live-editable in Panels; raise it if the
robot clips a rail), planned by `auto/DrivePlan`. Beside a rail the robot holds a heading square
to the field. Driving under the HIVE still needs a field check (overhead clearance, G409):
docs/decisions.md, 2026-10-11. `ShootingSpots` holds **placeholder** positions, live-editable in Panels;
its comment has the tuning steps. Positions are in the RED frame of `docs/coordinates.md`.

- Button assignments live in `teleop/schemes/`, never here.
- Start position, when no AUTO ended in the last 30 s: `CompTeleOp.START`, (8, 60, 0°), against
  the red alliance wall facing the blue one. It's TeleOp's own spot, not tied to any auto (there
  are many); after an auto, TeleOp starts where it ended. For practice, set the robot there
  before INIT. A variant can override `startWaypoint()` to start elsewhere.
- `@TeleOp(name = "...", group = "comp")` for match OpModes; `group = "test"` for experiments.
  Autos' `preselectTeleOp` must match the name exactly.
- No FTC Dashboard / Wi-Fi streaming in `comp` OpModes (R704).
