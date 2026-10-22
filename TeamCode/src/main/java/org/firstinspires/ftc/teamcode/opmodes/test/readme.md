# opmodes/test/

Standalone checks for the competition robot (group `"test"`). Each is a plain `OpMode` that reads
the gamepad directly, like the StarterBot OpMode, rather than using the TeleOp engine and control
schemes. Config names still come from `RobotHardware`. Not for matches.

| OpMode | What it checks | How |
|---|---|---|
| `Test: Drive Motors` | each drive motor is in the port and config name docs/hardware.md says, and spins forward | robot on blocks; hold gp1 X = front left, Y = front right, A = back left, B = back right (power 0.3). Only that wheel should spin, forward. Telemetry shows each motor's power, encoder count, and the port the SDK found next to the expected one |
