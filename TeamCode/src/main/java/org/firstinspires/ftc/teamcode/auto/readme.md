# auto/

Declarative autonomous routines: a **JSON route file** per auto, executed by a small step engine
on top of PedroPathing.

## Route files

`TeamCode/src/main/assets/autos/NAME.json` (version-controlled, shipped in the APK). A copy at
`/sdcard/FIRST/autos/NAME.json` on the Control Hub **overrides** it, so a route can be changed at
an event without a rebuild.

**Fastest: Jimothy** ([tools/routeview](../../../../../../../../../tools/routeview/readme.md)).
Run `./gradlew -p tools/routeview run` and open http://127.0.0.1:8765. Each save is checked the way
INIT checks it, drawn like `docs/autos.pdf`, and pushed to the hub in well under a second. Files
that would fail INIT aren't pushed.

By hand:

```
adb connect 192.168.43.1:5555            # once, on the robot's Wi-Fi
adb push leave_and_park.json /sdcard/FIRST/autos/leave_and_park.json
```
Give the full file path, not just the folder. Nothing creates `/sdcard/FIRST/autos/` on the hub,
and pushing to a folder that doesn't exist fails with "remote couldn't create file: Is a
directory". With a full path, adb creates the folder.

(Or use Android Studio → Device Explorer.) Delete the override to go back to the APK copy:
`adb shell rm /sdcard/FIRST/autos/leave_and_park.json`. The DS telemetry says which one was
loaded, with a checksum of the file (`RouteChecksum`) that matches the one Jimothy shows.

```json
{
  "name": "Leave + Park",
  "waypoints": { "START": [8, 60, 0], "LEAVE": [20, 60, 0], "PARK": [20, 20, 0] },
  "start": "START",
  "routine": [
    { "follow": { "line": ["START", "LEAVE"] }, "name": "leave wall" },
    { "wait": 0.5 },
    { "follow": { "line": ["LEAVE", "PARK"] }, "name": "park", "timeout": 5 }
  ]
}
```

Waypoints are `[x, y, headingDeg]` in the **RED** frame; the OpMode's alliance maps them. One
file serves both alliances. Origin, axes, heading and the BLUE rotation are defined in
[docs/coordinates.md](../../../../../../../../../docs/coordinates.md).

### Steps

| Step | Meaning |
|---|---|
| `{ "sequence": [ … ] }` | one after another (the top-level `routine` is a sequence) |
| `{ "parallel": [ … ] }` | together; done when all are done |
| `{ "race": [ … ] }` | together; done when the first is, others interrupted |
| `{ "follow": { "line": ["A","B"] } }` | drive a straight line |
| `{ "follow": { "curve": ["A","C1","B"] } }` | Bezier — middle names are control points |
| `{ "follow": { "through": ["A","B","C"] } }` | smooth curve through every point |
| `{ "wait": 0.5 }` | seconds |
| `{ "waitUntil": "intake.hasBall" }` | a registered condition |
| `{ "action": "shooter.fire", "args": { "count": 3 } }` | a registered subsystem action |
| `{ "hold": "PARK", "timeout": 2 }` | hold a pose (timeout required) |
| `{ "seek": "vision.ball", "args": { "colors": "pollen" }, "timeout": 4 }` | a registered drive to somewhere the robot decides, e.g. a ball the camera sees (timeout required) |
| `{ "driveTo": "PARK" }` | drive to a waypoint from wherever the robot is, planned on the robot round the HIVE and off the other half (`AutoClearance`) |

Modifiers on any step: `"name"` (Driver Station telemetry) and `"timeout"` (seconds; interrupts).
`"args"` goes only with `"action"` or `"seek"`.

**After a `seek` the robot could be anywhere**, so get back onto the route with a `driveTo`: a
`follow` would start its path from wherever the seek ended. The auto maps draw a seek's search
area and the `driveTo` after it as dashed guesses, and flag a `follow` straight after a seek
(docs/auto-maps.md). `vision.ball`'s args are in `vision/ToBallArgs`: `colors` (`"pollen"`,
`"nectar"` for ours, `"ours"` for both), `within` and `standoff` in inches, `look` in seconds, and
`prefer` (`"most"`, the default: a bigger group of balls about as near wins; or `"nearest"`).

`follow` heading (default: linear from the first waypoint's heading to the last's):
`"heading": "tangent"` · `"reverseTangent"` · `{ "constant": 90 }` · `{ "linear": [0, 90] }` ·
`{ "facing": "HIVE" }`. Degrees are RED-frame and mirrored like waypoints.

Errors (unknown waypoint/action, bad key, missing timeout on hold…) are reported on the Driver
Station at INIT with the path to the offending step, e.g. `routine[2].follow.line[1]`. Files
must be strict JSON: no comments, every key quoted, and no key twice in one object, so a waypoint
defined twice is an error, not a silent override.

## Java side

- `JsonAutoOpMode` — subclass, return `routeName()`, create subsystems in `createSubsystems()`
  with `add(...)`, and register what the file may use in `registerActions`:
  `registry.action("shooter.fire", args -> shooter.fire(args.get("count").getAsInt()))`,
  `registry.condition("intake.hasBall", intake::hasBall)`, `registry.seek("vision.ball", vision::toBall)`.
  Use string literals: Jimothy finds registered names by searching the source. Nested
  `Red`/`Blue` classes register the two OpModes (see `opmodes/auto/LeaveAndParkAuto`, and
  `VisionSeekAuto` for vision).
- `Step` — what actions implement: `start` / non-blocking `update` returning done / `end`.
- `Steps` — the same combinators as the JSON (`sequence`, `parallel`, `race`, `withTimeout`,
  `waitSeconds`, `waitUntil`, `instant`, `follow`, `hold`) for routines written in Java
  directly on `AutoOpMode`, when a file can't express something. `driveTo` plans from wherever
  the robot is when the step starts, for TeleOp assists (`teleop/readme.md`), with `DrivePlan`.
- `DrivePlan` — the quickest way to a pose that keeps every part of the robot a margin from a
  set of obstacles, round them or between them: straight legs, a heading square to the field
  beside an obstacle, turns where there's room. Pure Java, unit-tested (`DrivePlanTest`).
- `AutoClearance` — what AUTO's planned drives (`driveTo`, vision seeks) keep clear of: the
  HIVE outline and the other alliance's half (G402), `MARGIN` inches. Jimothy compiles it, so the
  maps draw the same plans.
- `KeepOut` — a field rectangle, and the cheapest way round a set of them (straight lines via
  their corners, costed by distance or by `DrivePlan`'s driving time). Pure Java, unit-tested.
- `Field` — the field's numbers in code: `SIZE`, `CENTER`, `HIVE` (the HIVE structure's outline
  seen from above; the auto maps draw it and flag an auto that crosses it) and `HIVE_FLOOR` (its
  two frame rails, the only parts on the floor; what TeleOp assists avoid). The rails are
  placeholders until measured. What they mean:
  [docs/coordinates.md](../../../../../../../../../docs/coordinates.md).
- `RobotSize` — the robot's footprint, for `DrivePlan` and the auto maps. A placeholder (18 × 18)
  until the robot is measured.
- `PoseHandoff` — `AutoOpMode.stop()` saves the final pose; a TeleOp INIT'd within 30 s starts
  from it instead of its own start waypoint.
- `AutoWaypoints` — `@Configurable`: the loaded route's waypoints, live-editable in Panels (dev
  builds). Set `reloadFromFileOnInit = false` to keep edits across re-INITs; copy final values
  back into the JSON.
- `RouteViewRemote` (dev source set, `TeamCode/src/dev/java/.../auto`) — lets tools/routeview INIT,
  START and STOP OpModes over the robot app's web server; its safety rules are `RemoteRunGuard`
  (tests in `TeamCode/src/testDev`). Not in competition builds.
- `route/` — `RouteParser` (file → `Route`), `RouteCompiler` (`Route` → `Step` for an alliance),
  `ActionRegistry`, `RouteFiles` (hub override / APK asset). All but `RouteFiles` are pure Java
  and unit-tested.

Rules: `preselectTeleOp` so TeleOp auto-loads (G305); nothing moves during INIT (G403).
