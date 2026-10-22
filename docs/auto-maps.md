# Auto maps

Every autonomous route drawn on the field, for each alliance, worked out from the route file
without running it. They're in `docs/autos.pdf`, redrawn whenever the unit tests run
(`./gradlew :TeamCode:autoMaps` is the shortcut), and in Jimothy (`tools/routeview`), live as
you edit.

The code: `TeamCode/src/test/java/.../automaps/`. `AutoRun` walks a route; `FieldPainter` draws it
on a PDF page or Jimothy's SVG. Numbers are in `MapSettings`; change them there and here together.

## What's drawn

- **The field** (`BiobuzzField`, measured in the Pedro visualizer image): tiles, centre line,
  LOADING ZONES, GARDENS, FLOWERS, and the HIVE's outline, CELLs, rails and dampers.
- **The robot** at START (dashed), at the end of each path (thin) and at the end (filled),
  18 × 18 in (`MapSettings.ROBOT_LENGTH`/`WIDTH`). The thick edge is the front.
- **Paths** (`follow`): sampled from the same Pedro paths the robot follows, built by
  `RouteCompiler`, with an arrowhead.
- **`driveTo` from a known pose:** the plan the robot will make (`DrivePlan` round
  `AutoClearance.obstacles`: the HIVE outline and the other half), drawn and checked like a path.
- **`seek`:** a dashed circle round where it starts, as far as its `within` arg (default
  `MapSettings.SEEK_WITHIN`, VisionTuning's default), cut off at the centre line and the walls.
  After a seek the robot's position is unknown.
- **`driveTo` after a seek:** a dashed straight line from where the robot was last known, labelled
  "from wherever step N ended". Not checked against the field.
- **Waypoints**, labelled, with `(control)` for Bezier control points, and a numbered badge for
  every step.

## Time

Each `follow` accelerates at `ACCEL` (37 in/s²) to `SPEED` (50 in/s), decelerates the same way,
then settles for `SETTLE` (0.25 s). A known `driveTo` takes `DrivePlan`'s estimate plus the
settle. `wait` is its seconds. `action`, `waitUntil`, `hold` and `seek` take an unknown time ("?")
unless they have a `timeout`, which counts as the time they take. Groups: a sequence adds up, a
parallel takes the longest, a race the shortest. Rough by design: calibrate by timing real runs.

## Points

- **LEAVE (3):** the robot ends more than 0.25 in from every wall.
- **PARK (5):** the robot ends partly in our LOADING ZONE.
- **HIVE TIPs:** not estimated; no route has launch actions yet.

## Checks

- **START:** touching the wall (G304), not in the LOADING ZONE, not touching a FLOWER, entirely
  on our half (G303).
- **Every path and known `driveTo`:** stays on our half (G402), out of the HIVE structure, off
  the walls.
- **A `follow` straight after a `seek`:** flagged, because the path would start wherever the seek
  ended. Put a `driveTo` to its first waypoint before it.
- **A route that ends with a seek:** flagged, because where it ends, and so LEAVE and PARK, is a
  guess.
- **Total time** over the 30 s AUTO.
