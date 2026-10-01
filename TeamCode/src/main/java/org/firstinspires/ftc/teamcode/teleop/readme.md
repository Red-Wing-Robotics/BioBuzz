# teleop/

Driver-controlled OpMode engine. Every gamepad assignment lives in a **control scheme**
(`schemes/`); every TeleOp using that scheme follows it.

```
gamepads → DriverInput → ControlScheme.map → DriverCommands → TeleOpBase (drive) + apply() (mechanisms)
           snapshot,      the button map      plain fields:    opmodes/teleop/CompTeleOp
           press/release  (schemes/)          what drivers want
```

| Class | Role |
|---|---|
| `schemes/TwoDriverScheme` | match layout: gamepad1 drives, gamepad2 runs mechanisms |
| `schemes/SoloScheme` | everything on gamepad1, for practice |
| `ControlScheme` | base: declare bindings with `button(...)` / `axis(...)`, implement `map` |
| `DriverCommands` | what the drivers want this loop: drive powers, intake, shooter, fire |
| `DriveFeel` | `@Configurable` stick feel: deadband, response curve, slow-mode scale |
| `TeleOpBase` | the loop; subclasses pick alliance + scheme and implement `apply` |
| `GamepadState`, `DriverInput`, `Binding` | hardware-free gamepad snapshot with press/release detection |
| `MatchClock` | time left; rumbles both gamepads at 1:00 left |

The Driver Station lists the active scheme's bindings during INIT, so drivers can check the layout
before START.

## Common changes

- **Move a control:** change the binding in the scheme. `ControlSchemesTest` fails if an input is
  bound twice.
- **Add a driver action:** add a field to `DriverCommands` (and reset it in `clear`), bind and set
  it in every scheme, act on it in `CompTeleOp.apply`, and add a test to `ControlSchemesTest`.
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
- INIT moves nothing (G403). No sleeps; anything that takes time is a state machine.
