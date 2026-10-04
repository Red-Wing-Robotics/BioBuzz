# auto/

Declarative autonomous routines: a **JSON route file** per auto, executed by a small step engine
on top of PedroPathing.

## Route files

`TeamCode/src/main/assets/autos/NAME.json` (version-controlled, shipped in the APK). A copy at
`/sdcard/FIRST/autos/NAME.json` on the Control Hub **overrides** it, so a route can be changed at
an event without a rebuild:

```
adb connect 192.168.43.1:5555            # once, on the robot's Wi-Fi
adb push leave_and_park.json /sdcard/FIRST/autos/leave_and_park.json
```
Give the full file path, not just the folder. Nothing creates `/sdcard/FIRST/autos/` on the hub,
and pushing to a folder that doesn't exist fails with "remote couldn't create file: Is a
directory". With a full path, adb creates the folder.

(Or use Android Studio → Device Explorer.) Delete the override to go back to the APK copy:
`adb shell rm /sdcard/FIRST/autos/leave_and_park.json`. The DS telemetry says which one was loaded.

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

Waypoints are `[x, y, headingDeg]` in the **RED** frame; the OpMode's alliance maps them
(`Alliance.BLUE` = 180° rotation about the field centre). One file serves both alliances.

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

Modifiers on any step: `"name"` (Driver Station telemetry) and `"timeout"` (seconds; interrupts).

`follow` heading (default: linear from the first waypoint's heading to the last's):
`"heading": "tangent"` · `"reverseTangent"` · `{ "constant": 90 }` · `{ "linear": [0, 90] }` ·
`{ "facing": "HIVE" }`. Degrees are RED-frame and mirrored like waypoints.

Errors (unknown waypoint/action, bad key, missing timeout on hold…) are reported on the Driver
Station at INIT with the path to the offending step, e.g. `routine[2].follow.line[1]`.

## Java side

- `JsonAutoOpMode` — subclass, return `routeName()`, and register actions the file may use:
  `registry.action("shooter.fire", args -> shooter.fire(args.get("count").getAsInt()))`,
  `registry.condition("intake.hasBall", intake::hasBall)`. Nested `Red`/`Blue` classes register
  the two OpModes (see `opmodes/auto/LeaveAndParkAuto`).
- `Step` — what actions implement: `start` / non-blocking `update` returning done / `end`.
- `Steps` — the same combinators as the JSON (`sequence`, `parallel`, `race`, `withTimeout`,
  `waitSeconds`, `waitUntil`, `instant`, `follow`, `hold`) for routines written in Java
  directly on `AutoOpMode`, when a file can't express something. `driveTo` builds its path from
  wherever the robot is when the step starts, for TeleOp assists (`teleop/readme.md`).
- `PoseHandoff` — `AutoOpMode.stop()` saves the final pose; a TeleOp INIT'd within 30 s starts
  from it instead of its own start waypoint.
- `AutoWaypoints` — `@Configurable`: the loaded route's waypoints, live-editable in Panels (dev
  builds). Set `reloadFromFileOnInit = false` to keep edits across re-INITs; copy final values
  back into the JSON.
- `route/` — `RouteParser` (file → `Route`), `RouteCompiler` (`Route` → `Step` for an alliance),
  `ActionRegistry`, `RouteFiles` (hub override / APK asset). All but `RouteFiles` are pure Java
  and unit-tested.

Rules: `preselectTeleOp` so TeleOp auto-loads (G305); nothing moves during INIT (G403).
