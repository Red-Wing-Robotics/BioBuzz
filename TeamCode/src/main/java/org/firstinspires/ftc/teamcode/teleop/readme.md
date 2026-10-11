# teleop/

Driver-controlled OpMode engine. Every gamepad assignment lives in a **control scheme**
(`schemes/`); every TeleOp using that scheme follows it. Buttons can also start **assists**:
autonomous actions built from the same `Step`s the autos use.

```
gamepads → DriverInput → ControlScheme.map → DriverCommands ─┬→ TeleOpBase: manual drive
           snapshot,      the button map      what drivers    ├→ Assists: Steps, by request
           press/release  (schemes/)          want            └→ apply(): mechanisms (CompTeleOp)
```

| Class | Role |
|---|---|
| `schemes/TwoDriverScheme` | match layout: gamepad1 drives, gamepad2 runs mechanisms |
| `schemes/SoloScheme` | everything on gamepad1, for practice |
| `ControlScheme` | base: declare bindings with `button(...)` / `axis(...)`, implement `map` |
| `DriverCommands` | what the drivers want this loop: drive powers, intake, shooter, fire, assist requests |
| `Assists`, `Assist`, `Resource` | assist definitions, the names schemes request, and what each assist takes over |
| `DriveFeel` | `@Configurable` stick feel: deadband, response curve, slow-mode scale |
| `TeleOpBase` | the loop; subclasses pick alliance, scheme and start waypoint, and implement `apply` |
| `GamepadState`, `DriverInput`, `Binding` | hardware-free gamepad snapshot with press/release detection |
| `MatchClock` | time left; rumbles both gamepads at 1:00 left |

During INIT the Driver Station shows the start pose and the active scheme's bindings, so drivers
can check both before START.

## Start position

Each TeleOp declares `startWaypoint()`: RED frame, inches and degrees, mapped for BLUE like
auto waypoints ([docs/coordinates.md](../../../../../../../../../docs/coordinates.md)). At INIT the
Follower is set to:

1. **where AUTO ended**, if an auto stopped within the last 30 s (`auto/PoseHandoff`), which is
   the normal match flow. Override `usePoseFromAuto()` to return false to skip this.
2. otherwise **the start waypoint**. For practice, put the robot there before INIT.

The DS says which one was used. Field-position assists are only as accurate as this pose (see
"How the robot knows where it is" in docs/coordinates.md).

## Assists

A TeleOp defines assists in `registerAssists`, with a name, the resources it takes over, and a
factory that builds a fresh `Step` each time it starts:

```java
assists.define(Assist.RETURN_TO_START, EnumSet.of(Resource.DRIVE),
        () -> Steps.driveTo("start", startPose(), Field.HIVE_FLOOR, HiveClearance.margin));  // clear of the HIVE, see CompTeleOp
assists.define(Assist.FIRE_ALL, EnumSet.of(Resource.SHOOTER, Resource.FEEDER),
        () -> shooter.fireAll());                       // a subsystem's Step factory
```

A scheme requests one from a binding: `out.press(Assist.X)` when its button is pressed, or
`out.hold(Assist.X)` every loop its button is held.

- **Pressed:** runs until its Step finishes; pressing again cancels it.
- **Held:** runs while held; release cancels it. If it finishes, or something else interrupts it
  (a drive stick, another assist taking a resource), while still held, it doesn't restart until
  the button is released and held again.
- **Resources:** starting an assist interrupts any running assist that shares a resource with it.
  Assists with nothing in common run together, e.g. the driver aiming while the operator fires.
- **The driver always wins:** moving a drive stick cancels every assist that uses DRIVE.
- **Manual control pauses** for a resource while an assist owns it. `TeleOpBase` skips manual
  driving; `apply` must check `assisting(Resource.X)` before writing a mechanism.
- **`.endgameOnly()`** refuses to start before 1:00 remains (G410).
- Running assists and any notice ("not available in this OpMode", "endgame only") show on the DS.

Write each action once, as a `Step` factory on its subsystem, and use it both from autos
(`registerActions`) and from TeleOp (`registerAssists`).

## Common changes

- **Move a control:** change the binding in the scheme. `ControlSchemesTest` fails if an input is
  bound twice.
- **Add a driver action:** add a field to `DriverCommands` (and reset it in `clear`), bind and set
  it in every scheme, act on it in `CompTeleOp.apply`, and add a test to `ControlSchemesTest`.
- **Add an assist:** add its name to `Assist`, define it in `CompTeleOp.registerAssists`, and bind
  it in a scheme with `out.press` / `out.hold`.
- **New layout** (e.g. a driver who wants different buttons): add a `ControlScheme` subclass in
  `schemes/`, add it to `ControlSchemesTest.allSchemes()`, and register a nested OpMode in
  `CompTeleOp` that returns it from `scheme()`.
- **Stick feel:** tune `DriveFeel` live in Panels (dev build), then copy the values into the file.

## Rules

- Schemes read input only through their bindings (`in.value / held / pressed / released`), never
  `gamepad1.x` directly, so every control appears on the control card and in the duplicate check.
- `pressed` / `released` are true for exactly one loop and may be read any number of times in that
  loop. The SDK's own `aWasPressed()` family reports a press only to its first caller, so don't use it.
- Schemes say what the drivers want. Subsystems decide whether it is safe: whether the shooter is
  up to speed, and the G407 four-element limit.
- The endgame rumble is a cue, not a lockout. The clock starts at START, so it can only run late,
  which is the safe direction for G410.
- INIT moves nothing (G403); assists only run after START. No sleeps; anything that takes time is
  a `Step` or a state machine.
