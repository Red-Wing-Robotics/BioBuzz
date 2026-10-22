# subsystems/

One class per mechanism or sensor: `Vision` today; `Intake`, `Launcher` and the rest as they're
built.

- **Implement `Subsystem`** and register it with `add(...)`: in `createSubsystems()` for an
  auto, in `onInit()` for a TeleOp. The OpMode then calls `periodic()` once per loop after
  START, `status(telemetry)` every loop from INIT on, and `stop()` when it ends. Nothing may
  move during INIT (G403): only `status` runs then.
- A subsystem receives its devices from `RobotHardware` in its constructor and owns them.
- Expose intent-level methods (`launch()`, `setIntakePower(double)`, `isBallDetected()`), not raw
  motors. Write each action once as a `Step` factory, used by routes (`registerActions`) and
  TeleOp assists (`registerAssists`) alike.
- No `sleep()` and no blocking calls in `periodic()`; long actions are `Step`s or state machines.
- Keep decision logic hardware-free (`util/`, `vision/`) where it can be unit tested.
- Tunables go in a `@Configurable` class beside the subsystem (`VisionTuning`).

| Class | What |
|---|---|
| `Subsystem`, `Subsystems` | the contract, and the list an OpMode runs |
| `Vision` | the Limelight 3A with Hive-Vision's ball model: balls on the field, drives to them, route seeks and conditions. See `vision/readme.md` |
| `VisionTuning` | `@Configurable`: the camera mount (docs/hardware.md), pipelines, tracking gates, approach |
