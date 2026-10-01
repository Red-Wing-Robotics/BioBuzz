# Decision Log

Short, dated entries. Newest first. Record *why*, not just *what*, so nobody (human or AI)
re-proposes a rejected idea without knowing the history.

## 2026-10-01 — TeleOp controls live in control schemes (`teleop/`)

Every gamepad assignment is a `Binding` declared in a `ControlScheme` (`teleop/schemes/`).
Schemes map input to plain `DriverCommands`; `TeleOpBase` drives, and `CompTeleOp.apply` drives
the subsystems. Variants are nested OpModes: `Red`/`Blue` (two drivers, preselected by each
alliance's autos) and `Solo` (practice). Schemes are Java, not JSON like the autos. Bindings carry
behaviour (hold vs toggle, guards) that needs code, compile errors beat INIT errors, and drivers
rely on fixed button positions, so remapping at an event is rare. The numbers that do get tweaked
(deadband, curve, slow scale) are `@Configurable` in `DriveFeel`. We chose not to use a
command-based library (FTCLib, NextFTC) because it would duplicate the `Step` engine; any TeleOp
macros should run `Step`s. Edge detection is our own (`GamepadState` compares with the previous
loop): the SDK's `aWasPressed()` reports a press only to its first caller, and `Gamepad` can't be
constructed in JUnit (it calls `android.os.SystemClock`). Replaces `DriveTeleOp`; driving is
unchanged.

## 2026-09-20 — `dev` / `comp` build variants; Panels and tuning are dev-only

R704.D names FTC Dashboard and FTControl Panels as prohibited streaming services during matches,
and the Pedro tuning server (port 10158) starts unconditionally whenever its library is present.
So TeamCode has product flavors: `dev` (Panels `fullpanels` + `tuning`, source in
`TeamCode/src/dev/java`) and `comp` (neither). `com.bylazar:configurables` is `compileOnly` so
`@Configurable` compiles in both; Android drops annotations whose class is absent at runtime.
Competition robots run `compDebug`.

## 2026-09-24 — Autonomous routines are JSON route files

Supersedes "Declarative autonomous framework" below: the team asked for a declarative *format*,
not a Java DSL. Routes live in `TeamCode/src/main/assets/autos/NAME.json` (waypoints + step
list, RED frame) and are parsed/validated/compiled at INIT by `auto/route/` onto the existing
`Step` engine, which remains the interpreter. A copy at `/sdcard/FIRST/autos/NAME.json` on the
Control Hub overrides the APK asset so routes can change at an event without a rebuild.
Subsystem actions stay in Java and are exposed by name via `ActionRegistry`. Waypoints are
copied into `@Configurable AutoWaypoints` for live tuning in Panels. JSON parsing uses the
Gson 2.8.0 bundled inside RobotCore (adding a newer Gson shadows it and breaks the build), so
the parser sticks to that API; it is pure Java, so parser/compiler are unit-tested on the desktop.

## 2026-09-20 — TeamCode compiles against API 34 (SDK default is 30)

Panels' AndroidX dependencies refuse to compile against API 30. `compileSdkVersion 34` is set in
`TeamCode/build.gradle` only (not the SDK-shipped `build.common.gradle`), so it survives SDK
updates. `targetSdkVersion` is untouched, so runtime behaviour on the Control Hub is unchanged.

## 2026-09-20 — Declarative autonomous framework (`auto/`)

Small command-style engine rather than a data file: `Step` (start/update/end), combinators in
`Steps` (sequence, parallel, race, timeout, waits, instant, follow, hold), `AutoOpMode` owning the
loop and Follower. Chosen over a JSON/YAML route format because subsystem actions need code
binding anyway, and because Panels edits `public static` fields — so waypoints live in
`@Configurable` points classes as mutable `Waypoint`s (Pedro's `Pose` is immutable and private).
Waypoints are authored in the RED frame; `Alliance.BLUE` applies
`PoseFactory.mirrorAroundPoint(72, 72)` (180° rotation — verified empirically on the JDK; field
symmetry itself still TODO in docs/field.md).

## 2026-09-20 — TeleOp is robot-centric

Driver preference. Field-centric drive (heading from the Pinpoint via `ManualDrive.fieldCentric`)
was implemented and removed; don't re-propose it unless the drivers ask.

## 2026-09-20 — Follower enabled; TeleOp switched to `follower.manual()`

Mecanum, Pinpoint and Foresight tuners were run and their output pasted into
`pedro/Constants.java`; `Constants.create()` now builds the `Follower`. `DriveTeleOp` drives via
`follower.manual()` + `follower.update()` — verified from the 3.0.1 bytecode that MANUAL mode is
`localizer.update()` then `drivetrain.drive(powers, true)`, identical to the previous direct path.
Autonomous can now use `follower.follow(path)`.

## 2026-09-20 — Vendor Pedro's tuning procedures into `pedro/procedures/`

`com.pedropathing:tuning` ships only the framework (web UI on RC port 10158, `@Tuner` scanner,
`TuningOpMode`); the actual Mecanum/Pinpoint/Foresight procedures live in the Pedro Quickstart.
Copied verbatim and registered in `pedro/Tuning.java`. Bumped tuning to 1.0.1 to match the
Quickstart HEAD the procedures came from. Don't edit them — re-copy on library upgrades.

## 2026-09-20 — TeleOp drives the Pedro `Mecanum` directly; no `Follower` until Foresight is tuned

Pedro 3.0's `Follower` requires a tuned `Foresight` (12 required constants from the Foresight
Tuner). Rather than invent placeholder values, `DriveTeleOp` uses `Mecanum.drive()` +
`PinpointLocalizer` + `ManualDrive.fieldCentric()` directly — the same way Pedro's own tuning
tests drive. When tuning is done, add `Constants.create()` (template in the file) and switch the
TeleOp to `follower.manual(...)`. Driver controls do not change.

## 2026-09-20 — Use PedroPathing for autonomous

Chosen over RoadRunner. Added `com.pedropathing:revhub` and `com.pedropathing:tuning` via the
Dairy Foundation Maven repo in `build.dependencies.gradle`.

## 2026-09-20 — Repo conventions

Thin OpModes, one class per subsystem, `RobotHardware` as the single owner of config names,
hardware-free `util/` with JUnit tests. See `CLAUDE.md`.
