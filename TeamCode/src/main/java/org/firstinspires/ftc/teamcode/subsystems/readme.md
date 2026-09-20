# subsystems/

One class per mechanism: `Drivetrain`, `Intake`, `Launcher`, etc.

- A subsystem receives its devices from `RobotHardware` in its constructor and owns them.
- Expose intent-level methods (`launch()`, `setIntakePower(double)`, `isBallDetected()`), not raw motors.
- No `sleep()`; long actions are state machines driven by a `periodic()`/`update()` call from the OpMode loop.
- Keep decision logic in `util/` where it can be unit tested without hardware.
