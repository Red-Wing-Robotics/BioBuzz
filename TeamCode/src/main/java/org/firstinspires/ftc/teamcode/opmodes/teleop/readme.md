# opmodes/teleop/

TeleOp OpModes, built on the engine in `teleop/` (see its readme). `CompTeleOp` is the competition
TeleOp. It sets the start waypoint, defines the assists in `registerAssists`, turns
`DriverCommands` into subsystem calls in `apply`, and registers its variants as nested classes.

| OpMode | Group | Scheme | Notes |
|---|---|---|---|
| `Red: TeleOp` / `Blue: TeleOp` | comp | `TwoDriverScheme` | preselected by the autos of that alliance |
| `Red: Aim TeleOp` / `Blue: Aim TeleOp` | comp | `AimScheme` | the match layout plus heading aim (below); not preselected by any auto |
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
