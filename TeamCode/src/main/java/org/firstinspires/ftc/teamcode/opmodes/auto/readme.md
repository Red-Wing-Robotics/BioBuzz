# opmodes/auto/

Autonomous OpModes. Each is a tiny `JsonAutoOpMode` subclass naming a route file in
`TeamCode/src/main/assets/autos/` and registering the subsystem actions the file uses — the
routine itself lives in the JSON (format: `auto/readme.md`).

- Register once per alliance with nested `Red`/`Blue` classes, as in `LeaveAndParkAuto`.
- `@Autonomous(name = "...", group = "comp", preselectTeleOp = "Red: TeleOp")` — or
  `"Blue: TeleOp"`, matching the class's alliance (G305).
- INIT must not move anything (G403); AUTO is 30 s. Priorities: LEAVE (3) + PARK (5) + HIVE TIPs
  (20 each). Stay on our side of the field (red A–C / blue D–F, G402).
- Waypoints are authored in the RED frame; `Alliance.BLUE` rotates them. The frame is defined in
  `docs/coordinates.md`; start poses and field positions are listed in `docs/field.md`.
