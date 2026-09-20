# BioBuzz 2026-2027 FTC Season

Team robot code for the FTC BIOBUZZ season, built on FTC SDK v12.0.
Game rules summary: [RULES.md](RULES.md) — read it before designing any mechanism, OpMode, or scoring strategy.

## Repo layout

- `TeamCode/` — **our code.** Everything we write lives under `TeamCode/src/main/java/org/firstinspires/ftc/teamcode/`.
- `FtcRobotController/` — the upstream FTC SDK. **Never edit it.** It is replaced wholesale on SDK updates. Its `external/samples/` folder is reference material only.
- `build.dependencies.gradle` — the only shared gradle file we modify (third-party deps such as PedroPathing).
- `docs/` — team knowledge: hardware map, field data, decision log.

## Code structure (TeamCode)

- `RobotHardware.java` — single source of truth for hardware config names and `hardwareMap` lookups. Never hardcode a config string anywhere else.
- `subsystems/` — one class per mechanism (Drivetrain, Intake, Launcher, ...). Subsystems own their hardware and expose intent-level methods (`launch()`, `setIntake(...)`), not raw motor access.
- `opmodes/auto/` — Autonomous OpModes. Thin: build Pedro paths, sequence subsystems.
- `opmodes/teleop/` — TeleOp OpModes. Thin: map gamepad → subsystem calls.
- `pedro/` — PedroPathing constants and tuning OpModes.
- `util/` — hardware-free helpers (math, state machines, timers). Must be unit-testable.

## Conventions

- Java 8 source level (SDK constraint): no `var`, records, switch expressions, or text blocks.
- Annotate OpModes with `@TeleOp(name=..., group=...)` / `@Autonomous(name=..., group=..., preselectTeleOp=...)`. Groups: `"comp"` for match OpModes, `"tune"` for tuning, `"test"` for experiments.
- Copy patterns from the SDK samples in `FtcRobotController/src/main/java/org/firstinspires/ftc/robotcontroller/external/samples/` rather than recalling API usage from memory. Key samples: `SensorLimelight3A`, `ConceptAprilTag*`, `SensorGoBildaPinpoint`, `ConceptMotorBulkRead`, `RobotTeleopMecanumFieldRelativeDrive`.
- Enable bulk reads on every hub (`LynxModule.BulkCachingMode.MANUAL` + `clearBulkCache()` once per loop — see `ConceptMotorBulkRead`). Cycle time is the top optimization target.
- No blocking `sleep()` in TeleOp loops; use timers/state machines.
- Keep decision-making logic hardware-free so it can be unit tested.

## Libraries

- [PedroPathing](https://pedropathing.com/) for autonomous pathing (`com.pedropathing:revhub`, `com.pedropathing:tuning`).
  Pedro **3.0** — its API differs from the 2.x examples common online. Verify against the library source before use: `revhub` ships a `-sources.jar` in `~/.gradle/caches/modules-2/files-2.1/com.pedropathing/`; `core` needs `javap`. Construction patterns are in `pedro/Constants.java`.

## Build & verify

Java is not on PATH; the build uses Android Studio's bundled JDK. `.claude/settings.json` sets
`JAVA_HOME` for Claude Code sessions; in a plain terminal run
`export JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home"` first.

```
./gradlew :TeamCode:compileDebugJavaWithJavac   # fast check (~2 s warm)
./gradlew :TeamCode:testDebugUnitTest           # unit tests (TeamCode/src/test)
./gradlew :TeamCode:assembleDebug               # full APK
```

A PostToolUse hook (`scripts/compile-check.sh`) recompiles TeamCode automatically after any
Java edit and reports errors back — but still run the unit tests yourself before declaring
work done. Code is not done until it compiles and tests pass. There is no simulator — anything touching hardware can only be validated on the robot; say so explicitly when reporting.

## Rules that constrain code (details in RULES.md)

- **R704** — no FTC Dashboard, streaming, or Wi-Fi logging during matches. Tuning/telemetry tools must be gated off in `comp` OpModes.
- **G305 / G403** — use `preselectTeleOp` so TeleOp auto-loads after AUTO; INIT must not move any actuator (the 8 s AUTO→TELEOP transition must be motionless).
- **G407** — never CONTROL more than 4 scoring elements. Intake logic should count and stop.
- **G410 / G426** — NECTAR into FLOWERS, and entry of all remaining NECTAR, only in the final 60 s.
- **R503** — 8 motors + 8 servos max. **R105** — expansion limit 18×24×29 in, enforced mechanically, not in software.

## Optimizations

- Always optimize for minimum cycle time — fewer hardware reads, bulk reads, no blocking sleeps.

## Team knowledge

- [docs/hardware.md](docs/hardware.md) — port map and config names. If it disagrees with `RobotHardware.java`, fix both.
- [docs/field.md](docs/field.md) — AprilTag IDs, start poses, field coordinates.
- [docs/decisions.md](docs/decisions.md) — dated design decisions. Check before proposing a change of direction; add an entry when one is made.
