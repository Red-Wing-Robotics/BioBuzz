# Decision Log

Short, dated entries. Newest first. Record *why*, not just *what*, so nobody (human or AI)
re-proposes a rejected idea without knowing the history.

## 2026-10-15 — Two robots, one APK: the hub names its robot in /sdcard/FIRST/robot.txt

We now run the same code on two robots, **Belteshazzar** (the first, tuned 2026-09-20) and
**Ashurbanipal** (new, not tuned yet). They share config names, the port map and motor directions;
their Pinpoint pod offsets and Pedro tuning differ. Each Control Hub holds a one-line file,
`/sdcard/FIRST/robot.txt`, read at INIT by `RobotId`; `pedro/Constants` picks that robot's values
from `pedro/Belteshazzar` / `pedro/Ashurbanipal`. The file lives beside the RC configurations, so
APK installs, SDK updates and route pushes don't touch it; it belongs to the hub, so set it again
after swapping hubs. Set it with the `Robot ID: ...` OpModes (group `setup`, both builds) or adb.
A missing or unknown ID stops INIT with how to fix it: no default, because the wrong tuning is
worse than not moving. INIT telemetry shows the robot and warns while its Pedro values are
placeholders. Only what differs is per-robot; `RobotSize`, `DriveFeel`, `HeadingAim` and the
`DrivePlan` speeds stay shared until a robot needs its own.
- **Rejected: a build flavor per robot.** Four variants (dev/comp × robot), and installing the
  wrong APK would load the wrong tuning silently.
- **Rejected: separate OpModes per robot.** Doubles the Driver Station list and lets a driver
  pick the wrong one.
- **Rejected: detecting a marker device in the RC config.** Renaming a config entry would
  silently switch robots.

## 2026-10-15 — Ball vision: Hive-Vision's model and tracking ideas, our own driving

The Limelight 3A runs [Hive-Vision](https://github.com/hivevisionftc/Hive-Vision)'s ball model
(v1.1.0, an SSD detector for POLLEN and red/blue NECTAR) in a Neural Detector pipeline. On the
robot:
- `vision/` turns detections into balls on the field. It adapts Hive-Vision's `BallTracker` ideas
  (confidence gate, distinct-frame confirmation, colour-bound matching, ground range) under its
  MIT licence (THIRD_PARTY_NOTICES.md).
- `subsystems/Vision` is the only code touching the SDK's Limelight types.
- `DriveToBall` is a `Step` that drives to a ball.
- Routes get two step kinds:
  - `seek` (`vision.ball`) drives where the robot decides;
  - `driveTo` gets back onto the route's waypoints from wherever the robot is.
- `Subsystem`/`add(...)` gives AUTO and TeleOp a per-loop hook. AutoOpMode had none, and the
  intake will need one too.

**Rejected: Hive-Vision's driving classes** (`BallHunt`, `BallWrangler`, `BallChaseFollower`).
- Their `go()` blocks the loop, and `BallWrangler` sleeps.
- They target Pedro 2. The Pedro 3 port is a beta that isn't tested in CI and ignores camera
  offsets.
- They use their own drive conventions (turn + clockwise).
- Our `Step`s compose with routes, assists, timeouts and Jimothy's maps.

**Balls are tracked as field positions, not image angles.** A ball keeps its identity while the
robot drives and turns. The approach also finishes after the ball drops out of the bottom of the
image (from about 4.5 in ahead of the lens), aiming at the remembered position. Each frame is
placed from the robot's pose when it was captured: 60 ms late while turning fast puts a ball
inches off.

**Re-plan in field coordinates; don't steer by camera angle.** Steering from tx/ty with
`follower.manual` would bypass Pedro's tuned control. It would also lose the ball close in, and
couldn't be unit-tested. `DriveToBall` plans with `DrivePlan`, re-plans at most every 250 ms while
the estimate moves, and freezes the target within 18 in.

**`seek` is its own step kind, not an `action`.** Jimothy and the auto maps only see the file, and
treat actions as instant no-ops. With `seek` the file says the robot went somewhere nobody can
draw. The maps show a search area, the `driveTo` after it as a dashed guess, and flag a `follow`
straight after a seek.

**The camera's two jobs.** The 10-11 choice was balls in one OpMode, AprilTags in another.
`Vision.Job` selects the pipeline at INIT, switching on a background thread because the TeleOp
INIT falls inside the 8 s transition. But the mount measured today is pitched 33.3° down, and the
top of its image is 12° below horizontal: it can't see the CELL-underside tags. So `TAGS` only
selects a pipeline, and nothing reads tags. Open: re-mount for tags, or keep the Limelight
ball-only.

**Still to do on the robot (phase 0).** Hive-Vision's own checklist says the model hasn't been
verified on a real Limelight 3A. Check:
- frame rate;
- bumpers or alliance tape read as NECTAR;
- the pipeline indexes, the confidence scale (the SDK says 0–100, Hive-Vision 0–1; both are
  accepted), the pitch and the latency, on the bench (`opmodes/test/readme.md`).

The weights stay out of the repo and the APK: treat them as AGPL-3.0 until the author says
otherwise. Pickup itself waits for the intake: G407 counting, and confirming a ball was taken.

## 2026-10-11 — Heading aim at our HIVE's CELLs, in separate Aim TeleOps

`Red: Aim TeleOp` / `Blue: Aim TeleOp` (`CompTeleOp.RedAim` / `BlueAim`, `AimScheme`) are the
match TeleOp plus driver X / B: keep the robot's front (launcher) pointed at the left / right
CELL centre (`HiveCells`) while the driver still drives and strafes; the turn stick does
nothing while it is on. A PD controller on the heading error (`teleop/HeadingAim`, gains in
Panels, untuned). It is not an assist: assists own DRIVE and would cancel on any drive stick,
and this must run alongside manual driving. Two buttons, one per CELL, because the robot doesn't
yet know which CELL is up. Next step: pick the up CELL automatically (count TIPs, or read the
CELL AprilTags), then one button. Kept out of `Red/Blue: TeleOp`, which the autos preselect,
until it is tried on the robot.

## 2026-10-11 — StarterBot OpModes get their own package and group

`BioBuzzStarterbotTeleop` runs the goBILDA StarterBot, a separate robot with its own RC config.
It sat in `opmodes/teleop/` in the `comp` group with hardcoded config names, so it appeared in
the match OpMode list and crashed at INIT on the competition robot, and a review flagged it as
breaking the config-name rule. It now lives in `opmodes/starterbot/`, in group `"starterbot"`,
named `StarterBot: TeleOp`, with its names in `StarterBotHardware` (tested like `RobotHardware`).
- **Rejected: a custom annotation** to mark StarterBot OpModes. The Driver Station shows only an
  OpMode's name and group, so drivers would never see it, and only extra tooling could read it.

## 2026-10-11 — TeleOp assists may drive under the HIVE, between its floor rails

Takes up the 10-08 entry's next steps. `Steps.driveTo` now plans with `auto/DrivePlan`, which
keeps every part of the robot `HiveClearance.margin` from the HIVE's **floor rails**
(`Field.HIVE_FLOOR`) instead of keeping its centre 21 in from the whole outline (`Field.HIVE`).
Within reach of a rail the robot holds a heading square to the field, so its footprint lines up
with the rails and it passes them with half its width plus the margin. That makes the ~45 in
lane between the two rails wide enough for an 18 in robot. The planner takes the quickest way by
`DrivePlan.SPEED`, `ACCEL` and `TURN_RATE` (rough; calibrate from timed runs), round the HIVE
or under it. The margin is 8.3 in: the gap that 21 in left a robot turned 45°, which stopped it
snagging on 10-08. Kept from 10-04: straight legs via corners, never smooth curves.

This replaces the model in which the whole outline was floor footprint ("no robot fits here").
`Field.HIVE` is now the outline seen from above.

**Open: check before relying on it in a match.**
- **Overhead clearance.** `Field.HIVE_FLOOR` assumes nothing overhead comes low enough to reach
  a robot in the lane, even mid-tip. That hasn't been measured: RULES.md gives only the 43.95 in
  pivot height. Measure the lowest point of a down-facing CELL and of the dampers over the lane,
  and compare it with the robot's height.
- **G409.** A tipping HIVE dumps its CELL onto the floor under it. A robot passing under at that
  moment can deflect falling elements, which G409 forbids, and G409 also says not to camp under
  the HIVE. The assists only pass through (the shooting spots are off the HIVE's ends), but a pass
  during a tip is possible. If that is judged too risky, set `HiveClearance.margin` above 13.6:
  the lane closes and every assist goes round.
- **Placeholders.** The rails are measured in the visualizer image (`Field.HIVE_RAIL_RED`), and
  `RobotSize` is the 18 × 18 maximum. Turning on the spot beside a rail leaves only half the
  width plus the margin, less the reach: 4.6 in. Errors in these numbers matter more than they did
  at 21 in.

Autos are unchanged: the auto maps still flag any route that crosses the HIVE outline.
`DrivePlanTest` replaces `DriveToPathTest`. It samples the whole footprint along every plan from a
grid of starts to every shooting spot. It checks the gap to both rails, the square heading beside
them, and that the lane closes above 13.6. Docs updated to match: `opmodes/teleop/readme.md`,
`teleop/readme.md`, `auto/readme.md`, `docs/field.md`, `CLAUDE.md`.

## 2026-10-08 — TeleOp HIVE clearance raised from 15 in to 21 in

On the robot, the shooting-spot assists went round the HIVE but drove close enough to get caught
on it. At 15 in the robot's centre ran 15 in from the HIVE, and an 18 × 18 robot turned 45°
reaches 12.7 in, leaving 2.3 in for path-following error, pose error, the image-measured HIVE
footprint and anything sticking out of the robot. 21 in leaves 8.3 in; the route still keeps the
robot's centre over 24 in from the walls. The left shooting spot (y 117) is now inside the grown
zone, so that press goes round and then drives straight in from the zone's edge (y 118.6).
Tried first because it's one number. If it still clips, next: hold the final heading while
beside the HIVE (the heading now turns evenly over the whole route, so the robot is mid-turn
alongside it), size the clearance from the measured robot, and a stop if the robot gets close.

## 2026-10-04 — Field coordinates: Pedro's 144 in grid, defined once in docs/coordinates.md

[docs/coordinates.md](coordinates.md) is now the only place the frame is defined: origin, axes,
heading, size, the RED frame and the BLUE rotation. Code and docs link to it. The numbers live in
`auto/Field` (`SIZE`, `CENTER`, `HIVE`). **The field is 144 in, centre (72, 72)**, which is
Pedro's documented system: its coordinates page and FTC conversion, and the 2.x
`PedroCoordinates` javadoc. The game manual's nominal size is also ≈ 144 in.

Pedro's visualizer disagrees: it uses 141.5 (centre 70.75). Our auto maps had taken that scale
for the BIOBUZZ image, so every map measurement was 1.8% small, and the map checker flagged
`Alliance` (rotating about 72) as "2.5 in off". `Alliance` was right. The map geometry is now
stored as image pixels and converted through `Field.fromImage`, so it follows `Field.SIZE`. The
"blue centre" check is gone. If our real field measures differently, change `Field.SIZE` alone.

The BIOBUZZ image confirms the field is rotationally symmetric, so BLUE is a 180° rotation
(`Alliance`), not the visualizer's left/right mirror export.

Consequence: Leave + Park's START (60, 132, −90) was placed to touch a 141.5 in far wall. At 144
the auto map now reports a 3 in gap. That's left for the route owner to settle (move START, or
measure the field).

## 2026-10-04 — TeleOp assists drive round the HIVE; TeleOp has its own start pose

**Avoidance.** `Steps.driveTo` drove one straight line, so a shooting-spot press from the other
end of the HIVE drove into it. It now takes `KeepOut` rectangles and, when the line is blocked,
follows the shortest way round them: straight legs via the rectangle's corners (a visibility
graph, joined with `Paths.path(...)` so the heading still turns evenly over the whole way).
`CompTeleOp` passes `Field.HIVE` (the structure's floor footprint, also what the auto maps
draw) grown by `HiveClearance.inches`. That is 15 in: half an 18 in robot's diagonal, since it
may be turned any way mid-path, plus 2.3 in for following error. It is `@Configurable`.
Considered and rejected:
- **Smooth curves through the corners (`Paths.through`):** they can bulge into the zone between
  points. With straight legs, overshoot at a corner carries the robot away from the HIVE.
- **A grid planner (A\*):** more code and tuning for one rectangle.

Autos are unchanged. Routes are explicit and the auto maps already flag a path through the HIVE.

**Start pose.** `leave_and_park.json`'s START moved to (60, 132, −90), but `CompTeleOp` still
said its (8, 60, 0) start "is the Leave + Park START". A robot set up for the auto and then run in
TeleOp without AUTO had its whole frame rotated 90°, and the shooting spots landed by the loading
zone and across the field. TeleOp keeps **its own** practice start, (8, 60, 0), now documented as
such (`docs/field.md`) and shown on the DS at INIT. After any AUTO, the pose handoff already
starts TeleOp where the robot is.
- **Rejected: reading Leave + Park's START at TeleOp INIT.** There will be many autos, and
  TeleOp shouldn't depend on one of them.

Also: the shooting spots' left/right were swapped (red drivers' left is +y), and their headings
now point the front, where the launcher fires, at the HIVE.

## 2026-10-04 — Route View is now called Jimothy

The tool's user-facing name is **Jimothy**: page, icon (`tools/routeview/.../jimothy.svg`, brand
notes in its readme), console and robot messages. Code names stay:
- the `tools/routeview` folder and the `RouteView` / `RouteViewRemote` classes;
- the robot's `/routeview/` endpoints and `X-RouteView` header.

Renaming them would mean a robot redeploy and churn across files for no visible gain. Entries
below say "Route View".

## 2026-10-04 — Route View can INIT/START/STOP autonomous OpModes (dev builds only)

So the route-tuning loop never leaves the laptop. `RouteViewRemote` (TeamCode dev source set)
registers handlers on the robot app's own web server (`@WebHandlerRegistrar`, port 8080) and
drives `OpModeManagerImpl` (`initOpMode` / `startActiveOpMode` / `stopActiveOpMode`, as Panels'
opmodecontrol plugin does). Route View reaches it through `adb forward`, which works over Wi-Fi
and USB. Safety (`RemoteRunGuard`, unit tested):
- An OpMode started remotely stops after 30 s, and 1.5 s after Route View's 0.5 s status polls
  stop.
- One started on the Driver Station is left alone.
- POSTs need an `X-RouteView` header, so another site's page can't drive the robot.
- Someone stays at the robot with the Driver Station.

Rejected:
- **Panels' OpMode control protocol:** private and version-coupled.
- **Calling 192.168.43.1:8080 directly:** wouldn't work over USB.

Not in comp builds (R704). It uses SDK internals, so it may need fixing after an SDK update.

## 2026-10-04 — Route View: push routes on save, from a desktop tool in its own Gradle build

Changing a route must be much faster than a full deploy, or the hub override loses its point.
`tools/routeview` watches `assets/autos` and checks each save with the robot's own
`RouteParser`/`RouteCompiler`. It draws the result with the `docs/autos.pdf` code (`FieldPainter`
now draws on a `Canvas`; `Pdf.Page` and `SvgCanvas` implement it), serves that on 127.0.0.1, and
`adb push`es valid files to the hub (save to hub in well under 1 s once Gradle is warm). The DS
shows the loaded file's CRC-32 (`RouteChecksum`) so a push can be confirmed at INIT.
Considered and rejected:
- **A bare file watcher running `adb push`:** pushes half-edited files and fails silently.
- **Gradle `pushAutos -t`:** ~1.5 s per save of Gradle overhead.
- **A JSON Schema for validation:** it can't check waypoint or action references and would
  duplicate the parser. The page's editor (added the same day) checks drafts with the parser on
  the server instead, and refuses to save what INIT would reject. Any other editor works too,
  because the tool reacts to the file.
- **A JavaScript path renderer:** would drift from Pedro's curves.
- **TeamCode's unit-test source set:** compiles against android.jar at Java 8, so no
  `com.sun.net.httpserver`.
- **A separate repo:** the tool must change in step with the route engine.

It's a separate Gradle build (own `settings.gradle`), so the SDK-owned root `settings.gradle` is
untouched. It compiles the shared TeamCode files from source (`TEAMCODE_SOURCES`), so they must
stay Android-free.

## 2026-10-01 — TeleOp assists run `Step`s; TeleOp start pose comes from AUTO or a waypoint

Autonomous actions triggered from TeleOp ("assists") reuse the auto `Step` engine instead of
hand-written state machines or a command library. A subsystem action is written once as a `Step`
factory and used by both autos and TeleOp. TeleOps define assists in `registerAssists` (name,
`Resource`s, `Step` factory); schemes request them by name with `press` / `hold`. Rules agreed:
assists that don't share a resource run together and a conflicting start interrupts the older one;
pressing again cancels; release cancels a held assist; any drive-stick movement cancels assists
using DRIVE (the driver always wins); manual control of an owned resource pauses. Each TeleOp
declares a start waypoint (RED frame), but a pose saved by `AutoOpMode.stop()` within the last
30 s wins (`PoseHandoff`). After a match AUTO the robot is wherever AUTO left it, not at the
TeleOp's start, so using the waypoint alone would put field-position assists off by the whole
auto path. 30 s covers the 8 s transition with margin; a longer window risks a stale pose in
practice after the robot is moved by hand.

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
