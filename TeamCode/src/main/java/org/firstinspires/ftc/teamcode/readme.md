# TeamCode

BioBuzz's robot code for the 2026-2027 season (FTC SDK 12.0). Layout, conventions and build
commands are in the repo's [CLAUDE.md](../../../../../../../../CLAUDE.md). Read
[RULES.md](../../../../../../../../RULES.md) before designing a mechanism, OpMode or strategy.

| Where | What | Read |
|---|---|---|
| `RobotHardware` | config names, bulk reads | [docs/hardware.md](../../../../../../../../docs/hardware.md) |
| `subsystems/` | one class per mechanism or sensor (`Vision`) | [subsystems/readme.md](subsystems/readme.md) |
| `auto/` | autonomous engine: JSON routes, `Step`s, `DrivePlan`, `Field` | [auto/readme.md](auto/readme.md) |
| `vision/` | finding balls with the Limelight (Hive-Vision's model) and driving to them; hardware-free | [vision/readme.md](vision/readme.md) |
| `opmodes/auto/` | one small class per route | [opmodes/auto/readme.md](opmodes/auto/readme.md) |
| `teleop/` | TeleOp engine, assists, control schemes | [teleop/readme.md](teleop/readme.md) |
| `opmodes/teleop/` | the competition TeleOp and its variants | [opmodes/teleop/readme.md](opmodes/teleop/readme.md) |
| `opmodes/test/` | test OpModes, e.g. the vision bench | [opmodes/test/readme.md](opmodes/test/readme.md) |
| `opmodes/starterbot/` | OpModes for the goBILDA StarterBot, a separate robot | [opmodes/starterbot/readme.md](opmodes/starterbot/readme.md) |
| `pedro/` | PedroPathing constants | [pedro/readme.md](pedro/readme.md) |
| `util/` | hardware-free helpers | [util/readme.md](util/readme.md) |

Elsewhere in `TeamCode/src`:
- `dev/java`: dev-build-only code (Pedro tuning, Jimothy's remote INIT/START/STOP). It never
  ships in a competition build (R704).
- `test/java`: unit tests, plus the auto maps (`docs/autos.pdf`). `testDev/java` holds tests for
  dev-only code.
- `main/assets/autos`: the autonomous route files.

The field's coordinates are defined once, in
[docs/coordinates.md](../../../../../../../../docs/coordinates.md). Dated design decisions are in
[docs/decisions.md](../../../../../../../../docs/decisions.md).

For SDK API usage, copy from the samples in
`FtcRobotController/src/main/java/org/firstinspires/ftc/robotcontroller/external/samples/`.
They're reference only: never edit `FtcRobotController`.
