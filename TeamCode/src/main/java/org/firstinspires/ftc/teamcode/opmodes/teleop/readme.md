# opmodes/teleop/

TeleOp OpModes, built on the engine in `teleop/` (see its readme). `CompTeleOp` is the competition
TeleOp: it turns `DriverCommands` into subsystem calls in `apply` and registers its variants as
nested classes.

| OpMode | Group | Scheme | Notes |
|---|---|---|---|
| `Red: TeleOp` / `Blue: TeleOp` | comp | `TwoDriverScheme` | preselected by the autos of that alliance |
| `Solo: TeleOp` | test | `SoloScheme` | one person, practice |

- Button assignments live in `teleop/schemes/`, never here.
- `@TeleOp(name = "...", group = "comp")` for match OpModes; `group = "test"` for experiments.
  Autos' `preselectTeleOp` must match the name exactly.
- No FTC Dashboard / Wi-Fi streaming in `comp` OpModes (R704).
