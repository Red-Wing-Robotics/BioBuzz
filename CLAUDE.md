# BioBuzz 2026-2027 FTC Season

Team robot code for the FTC BIOBUZZ season, built on FTC SDK v12.0.
Game rules summary: [RULES.md](RULES.md) — read it before designing any mechanism, OpMode, or scoring strategy.

## Repo layout

- `TeamCode/` — **our code.** Everything we write lives under `TeamCode/src/main/java/org/firstinspires/ftc/teamcode/`.
- `FtcRobotController/` — the upstream FTC SDK. **Never edit it.** It is replaced wholesale on SDK updates. Its `external/samples/` folder is reference material only.
- `build.dependencies.gradle` — the only shared gradle file we modify (third-party deps such as PedroPathing).
- `docs/` — team knowledge: hardware map, field data, decision log.
- `tools/routeview/` — **Jimothy** (code names: RouteView), a desktop tool (its own Gradle build, run with
  `./gradlew -p tools/routeview run`): live map and editor for the route files, and a push to the
  hub on every save. It compiles TeamCode's `auto/`, `auto/route/` and test `automaps/` sources itself
  (`TEAMCODE_SOURCES` in its `build.gradle`), so those must stay free of Android/SDK types. See
  its `readme.md`.

## Code structure (TeamCode)

- `RobotHardware.java` — single source of truth for the competition robot's hardware config names and `hardwareMap` lookups. Never hardcode a config string anywhere else.
- `subsystems/` — one class per mechanism (Drivetrain, Intake, Launcher, ...). Subsystems own their hardware and expose intent-level methods (`launch()`, `setIntake(...)`), not raw motor access.
- `auto/` — autonomous engine: `JsonAutoOpMode` (routine from a JSON route file), `Step`/`Steps`,
  `Waypoint`/`Alliance`, `Field` (field size, centre, HIVE outline and floor rails), `KeepOut`
  (rectangles and the way round them), `DrivePlan` (TeleOp assist routes), `RobotSize`,
  `route/` (parser, compiler, action registry, file loading). Format and rules in
  `auto/readme.md`.
- `TeamCode/src/main/assets/autos/*.json` — **the autonomous routines.** Waypoints + step list,
  RED frame ([docs/coordinates.md](docs/coordinates.md)). A copy at `/sdcard/FIRST/autos/` on the hub overrides the APK one (no rebuild);
  Jimothy pushes it on every save.
- `opmodes/auto/` — one tiny `JsonAutoOpMode` subclass per route naming the file and registering
  subsystem actions; nested `Red`/`Blue` classes register both alliances.
- `teleop/` — TeleOp engine: `TeleOpBase` (loop, drive, start pose, endgame cue),
  `DriverCommands`, `Assists` (button-triggered `Step`s with resource ownership), `DriveFeel`
  (`@Configurable` stick feel). `teleop/schemes/` holds **the gamepad layouts**, the only place
  buttons are assigned. Rules in `teleop/readme.md`.
- `opmodes/teleop/` — `CompTeleOp` with nested `Red`/`Blue`/`Solo` variants: start waypoint,
  assist definitions, and `DriverCommands` → subsystem calls. Never read gamepads here; bindings
  belong in a scheme.
- `pedro/` — PedroPathing constants (`Constants.java`).
- `opmodes/starterbot/` — OpModes for the goBILDA **StarterBot**, a different robot with its own
  RC config. Its config names are in `StarterBotHardware`, not `RobotHardware`. It's a vendored
  sample, so the competition conventions don't apply. See its readme.
- `TeamCode/src/dev/java/` — **dev-only source set** (build variant `devDebug`): Pedro tuning
  registrations/procedures, `auto/RouteViewRemote` (Jimothy's remote INIT/START/STOP), and
  anything else that must not ship in a competition build. Unit tests for dev-only code go in
  `TeamCode/src/testDev/java` (the comp variant's tests must compile without it).
- `util/` — hardware-free helpers (math, state machines, timers). Must be unit-testable.

## Conventions

- Java 8 source level (SDK constraint): no `var`, records, switch expressions, or text blocks.
- Annotate OpModes with `@TeleOp(name=..., group=...)` / `@Autonomous(name=..., group=..., preselectTeleOp=...)`. Groups: `"comp"` for match OpModes, `"tune"` for tuning, `"test"` for experiments, `"starterbot"` for the StarterBot (name prefix `StarterBot:`).
- Copy patterns from the SDK samples in `FtcRobotController/src/main/java/org/firstinspires/ftc/robotcontroller/external/samples/` rather than recalling API usage from memory. Key samples: `SensorLimelight3A`, `ConceptAprilTag*`, `SensorGoBildaPinpoint`, `ConceptMotorBulkRead`, `RobotTeleopMecanumFieldRelativeDrive`.
- Enable bulk reads on every hub (`LynxModule.BulkCachingMode.MANUAL` + `clearBulkCache()` once per loop — see `ConceptMotorBulkRead`). Cycle time is the top optimization target.
- No blocking `sleep()` in TeleOp loops; use timers/state machines.
- Keep decision-making logic hardware-free so it can be unit tested. Pedro `core` is pure Java,
  so path/pose/`Step` logic runs in JUnit on the desktop (see `TeamCode/src/test`).
- Autonomous: routines are JSON route files, not Java. New subsystem actions are exposed to
  routes by registering them in the OpMode's `registerActions`. Never sleep or loop inside a
  `Step`; the `AutoOpMode` loop drives everything.
- Coordinates: [docs/coordinates.md](docs/coordinates.md) is the **one definition** of the field
  frame (origin, axes, heading, 144 in size, RED frame, BLUE rotation). Code and docs link to it
  rather than restating it. Field numbers live in `auto/Field.java`, and map geometry is measured
  in visualizer-image pixels through `Field.fromImage`. Never hard-code the field size or centre;
  use `Field.SIZE` / `Field.CENTER`.

## Libraries

- [PedroPathing](https://pedropathing.com/) for autonomous pathing (`com.pedropathing:revhub`; `tuning` is dev-only).
- [Panels](https://panels.bylazar.com/) dashboard, **dev builds only** (`com.bylazar:fullpanels`),
  at http://192.168.43.1:8001. `@Configurable` classes with `public static` fields are live-editable;
  the annotation module is compile-only so the same code builds for `comp`.
  Pedro **3.0** — its API differs from the 2.x examples common online. Verify against the library source before use: `revhub` ships a `-sources.jar` in `~/.gradle/caches/modules-2/files-2.1/com.pedropathing/`; `core` needs `javap`. Construction patterns are in `pedro/Constants.java`.

## Build & verify

Java is not on PATH; the build uses Android Studio's bundled JDK. `.claude/settings.json` sets
`JAVA_HOME` for Claude Code sessions; in a plain terminal run
`export JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home"` first.

Two build variants (flavor `dev` | `comp` × `debug`): **devDebug** for daily work (Panels +
Pedro tuning web UI), **compDebug** for competition (no Wi-Fi dashboards — R704.D). Switch in
Android Studio's Build Variants panel. Both must compile.

```
./gradlew :TeamCode:compileDevDebugJavaWithJavac :TeamCode:compileCompDebugJavaWithJavac  # fast check
./gradlew :TeamCode:testDevDebugUnitTest        # unit tests (TeamCode/src/test) — hardware-free
./gradlew :TeamCode:assembleDevDebug            # dev APK;  assembleCompDebug for competition
./gradlew -p tools/routeview test               # Jimothy unit tests (also after auto/ or automaps/ changes)
```

A PostToolUse hook (`scripts/compile-check.sh`) recompiles TeamCode automatically after any
Java edit, and Jimothy after edits to it or to the TeamCode sources it shares, and reports
errors back — but still run the unit tests yourself before declaring
work done. Code is not done until it compiles and tests pass. There is no simulator — anything touching hardware can only be validated on the robot; say so explicitly when reporting.

## Rules that constrain code (details in RULES.md)

- **R704** — no FTC Dashboard/Panels, streaming, or Wi-Fi logging during matches. Panels and the
  Pedro tuning server exist only in the `dev` build variant; competition robots run `compDebug`.
- **G305 / G403** — use `preselectTeleOp` so TeleOp auto-loads after AUTO; INIT must not move any actuator (the 8 s AUTO→TELEOP transition must be motionless).
- **G407** — never CONTROL more than 4 scoring elements. Intake logic should count and stop.
- **G410 / G426** — NECTAR into FLOWERS, and entry of all remaining NECTAR, only in the final 60 s.
- **R503** — 8 motors + 8 servos max. **R105** — expansion limit 18×24×29 in, enforced mechanically, not in software.

## Optimizations

- Always optimize for minimum cycle time — fewer hardware reads, bulk reads, no blocking sleeps.

## Team knowledge

- [docs/hardware.md](docs/hardware.md) — port map and config names. If it disagrees with `RobotHardware.java`, fix both.
- [docs/coordinates.md](docs/coordinates.md) — the field coordinate system: the one definition.
- [docs/field.md](docs/field.md) — AprilTag IDs, start poses, field positions (in that frame).
- [docs/decisions.md](docs/decisions.md) — dated design decisions. Check before proposing a change of direction; add an entry when one is made.
