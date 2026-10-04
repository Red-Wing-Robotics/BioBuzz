# Field Reference

Dimensions and rules: see [RULES.md](../RULES.md) §3. Field is ~144 in × 144 in; tiles are 24 in.

## Coordinate frame

TODO: define the frame used for PedroPathing (origin, axis directions, units, heading zero).
Every pose in this file and in code must use it.

Autonomous waypoints are authored **once, in the RED frame** (`@Configurable` points classes in
`opmodes/auto/`). `Alliance.BLUE` rotates them 180° about the field centre (72, 72). TODO: confirm
the BIOBUZZ field is rotationally (not mirror) symmetric — GARDENs in opposite corners suggest it
is; if not, switch `Alliance.BLUE` to `mirrorX(72)`.

## Start poses

| OpMode | Alliance | Position | Pose (x, y, heading) | Notes |
|---|---|---|---|---|
| | red | audience side | TODO | touching perimeter wall, columns A–C |
| | red | far side | TODO | |
| | blue | audience side | TODO | columns D–F |
| | blue | far side | TODO | |

Start requirements (G304): touching perimeter wall, on own side, not in LOADING ZONE, not
touching a FLOWER, within 18 in cube, contacting exactly 4 pre-loaded POLLEN.

## AprilTags (36h11, 3.25 in, on the underside of each CELL, facing down)

| CELL | IDs |
|---|---|
| Red, far side (opposite audience) | 0, ?, ?, ? — TODO confirm from manual figure 9-17 |
| Red, audience side | ?, ?, ?, 7 — TODO confirm |
| Blue, audience side | 38, 39, 40, 41 |
| Blue, far side | TODO confirm |

## Key locations

| Feature | Pose / dimensions | Notes |
|---|---|---|
| HIVE structure | field center; pivot 43.95 in above tiles | CELL opening 20 × 14 × 12 in |
| FLOWER ×4 | on perimeter wall, top opening 21.5 in above tiles | TODO exact positions |
| LOADING ZONE | 23 × 11 in at wall by alliance area | PARK target; NECTAR entry |
| GARDEN | 23 × 2 in tape strip, opposite corners | 1 pt per ball |
| Shooting spot, drivers' left | (48, 96, −45°) RED frame | **placeholder** — tune; runtime truth is `opmodes/teleop/ShootingSpots.java` |
| Shooting spot, drivers' right | (48, 48, 45°) RED frame | **placeholder** — tune; TeleOp gp1 d-pad ← / → drives there and holds |
