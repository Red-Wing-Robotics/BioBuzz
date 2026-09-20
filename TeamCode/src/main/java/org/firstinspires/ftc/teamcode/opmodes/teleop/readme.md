# opmodes/teleop/

TeleOp OpModes. Keep them thin: map gamepad input → subsystem calls, call `robot.clearBulkCache()` once per loop.

- `@TeleOp(name = "...", group = "comp")` for match OpModes; `group = "test"` for experiments.
- No FTC Dashboard / Wi-Fi streaming in `comp` OpModes (R704).
- Track match time: NECTAR may enter FLOWERS only in the last 60 s (G410) — consider a driver cue.
