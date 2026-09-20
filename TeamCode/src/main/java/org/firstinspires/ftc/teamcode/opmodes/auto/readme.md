# opmodes/auto/

Autonomous OpModes. Keep them thin: build PedroPathing paths, sequence subsystem calls.

- `@Autonomous(name = "...", group = "comp", preselectTeleOp = "<TeleOp name>")` so TeleOp auto-loads after AUTO (G305).
- INIT must not move any actuator — the AUTO→TELEOP transition must be motionless (G403).
- AUTO is 30 s. Priorities: LEAVE (3) + PARK in LOADING ZONE (5) + HIVE TIPs (20 each). Stay on our side of the field (red A–C / blue D–F, G402).
- Start poses and field coordinates live in `docs/field.md`.
