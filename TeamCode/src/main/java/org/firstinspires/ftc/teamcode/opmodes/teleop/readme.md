# opmodes/teleop/

TeleOp OpModes, built on the engine in `teleop/` (see its readme). `CompTeleOp` is the competition
TeleOp. It sets the start waypoint, defines the assists in `registerAssists`, turns
`DriverCommands` into subsystem calls in `apply`, and registers its variants as nested classes.

| OpMode | Group | Scheme | Notes |
|---|---|---|---|
| `Red: TeleOp` / `Blue: TeleOp` | comp | `TwoDriverScheme` | preselected by the autos of that alliance |
| `Solo: TeleOp` | test | `SoloScheme` | one person, practice; B = drive back to start |

Assists (defined in `CompTeleOp.registerAssists`, so every variant has them):

| Assist | Button | Does |
|---|---|---|
| left / right shooting spot | gp1 d-pad ← / → (both schemes) | drives to `ShootingSpots.left` / `.right` and holds there until a drive stick moves or the button is pressed again |
| return to start | gp1 B (Solo only) | drives to the start waypoint |

`ShootingSpots` holds **placeholder** positions, live-editable in Panels; its comment has the
tuning steps.

- Button assignments live in `teleop/schemes/`, never here.
- Start position: `startWaypoint()` (RED frame), used when no AUTO ended in the last 30 s. A
  variant can override it to start elsewhere.
- `@TeleOp(name = "...", group = "comp")` for match OpModes; `group = "test"` for experiments.
  Autos' `preselectTeleOp` must match the name exactly.
- No FTC Dashboard / Wi-Fi streaming in `comp` OpModes (R704).
