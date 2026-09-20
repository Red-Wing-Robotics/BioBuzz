# Decision Log

Short, dated entries. Newest first. Record *why*, not just *what*, so nobody (human or AI)
re-proposes a rejected idea without knowing the history.

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
