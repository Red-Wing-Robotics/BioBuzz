# Field Reference

Dimensions and rules: see [RULES.md](../RULES.md) §3. Field is ~144 in × 144 in; tiles are 24 in.
Positions below are in inches at `Field.SIZE` = 144 ([coordinates.md](coordinates.md#field-size-144-and-where-1415-comes-from)).

## Coordinates

Every pose here uses the frame in **[coordinates.md](coordinates.md)**, the one definition: origin,
axes, heading, field size, the RED frame and the BLUE rotation. Inches and degrees; poses marked
"RED frame" are mapped for BLUE.

## Start poses

| OpMode | Alliance | Position | Pose (x, y, heading) | Notes |
|---|---|---|---|---|
| | red | audience side | TODO | touching perimeter wall, columns A–C |
| Leave + Park | red | far side | (60, 132, −90°) | meant to touch the far wall, facing the audience. On the 144 in field an 18 in robot touches it at y = 135; the auto map flags the 3 in gap |
| TeleOp, no AUTO just before (practice) | red | red wall | (8, 60, 0°) | `CompTeleOp.START`: against the red alliance wall, facing the blue one. Not tied to any auto |
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
| HIVE outline, from above | x 47.3–96.7, y 46.4–97.6 | frame, CELLs, dampers. Runtime truth: `auto/Field.HIVE`. The auto maps flag an auto that crosses it |
| HIVE frame rails, on the floor | red x 47.5–49.3, y 52.0–91.7; blue x 94.7–96.5, y 52.3–92.0 | **Placeholder** (from the visualizer image). Runtime truth: `auto/Field.HIVE_FLOOR`. TeleOp assists keep `HiveClearance.margin` (8.3 in) from them and may use the ~45 in lane between them: overhead clearance not yet measured (docs/decisions.md, 2026-10-11) |
| Red HIVE CELLs | x 48.8–69.1; ends at y 52.4–70.1 (up at start) and y 79.3–97.3 | from the visualizer image (`automaps/BiobuzzField`) |
| Red HIVE CELL centres, aim points | left (58.9, 88.3), right (58.9, 61.3) RED frame | middles of the CELLs above. **Placeholder**; runtime truth is `opmodes/teleop/HiveCells.java`. Aim TeleOp gp1 X / B keeps the front pointed at one |
| Red LOADING ZONE | x 0–11.7, y 96.1–119.7 | on the red wall, drivers' left half |
| FLOWER ×4 | on perimeter wall, top opening 21.5 in above tiles | TODO exact positions |
| LOADING ZONE | 23 × 11 in at wall by alliance area | PARK target; NECTAR entry |
| GARDEN | 23 × 2 in tape strip, opposite corners | 1 pt per ball |
| Shooting spot, drivers' left | (58, 117, −90°) RED frame | off the HIVE's far end, front (launcher) facing it. **Placeholder**, tune; runtime truth is `opmodes/teleop/ShootingSpots.java` |
| Shooting spot, drivers' right | (58, 23, 90°) RED frame | off the HIVE's audience end, front facing it. **Placeholder**; TeleOp gp1 d-pad ← / → drives there (round the HIVE) and holds |
