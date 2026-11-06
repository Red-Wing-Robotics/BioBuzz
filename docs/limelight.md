# Limelight 3A

Our vision coprocessor, and the only legal programmable one (R702). Config name `limelight`
(`RobotHardware.LIMELIGHT`); port and wiring are in [hardware.md](hardware.md). AprilTag IDs and
where they sit on the field are in [field.md](field.md#apriltags-36h11-325-in-on-the-underside-of-each-cell-facing-down).

Code: `subsystems/Vision` and `subsystems/VisionTuning` (pipeline slots, mount), ball tracking in
`vision/` ([its readme](../TeamCode/src/main/java/org/firstinspires/ftc/teamcode/vision/readme.md)).

## Pipelines

The Limelight holds up to 10 pipelines (index 0–9), set up in its web UI. Code picks one with
`pipelineSwitch(index)`.

| Index | Name | Type | Detects | Used by |
|---|---|---|---|---|
| 0 | HiveVision | Color blob | game elements by color (below) | — |
| 1 | RedHives | AprilTag (fiducial) | red goal tags, IDs **30–37** | — |
| 2 | BlueHives | AprilTag (fiducial) | blue goal tags, IDs **38–45** | — |

Pipeline 1 is for the red alliance and 2 for blue, so an OpMode picks the pipeline from its
`Alliance`. Tags are 36h11, 3.25 in.

**HiveVision** looks for any color by default. It can be configured to look for only:

- POLLEN (yellow), or
- POLLEN and the NECTAR of our alliance's color (red or blue).

## Mounting

The camera's pose on the robot (the "robot space" camera pose in the Limelight web UI) is what
turns a sighting into a position on the field. Offsets are from the robot's tracking centre (the
point the Pinpoint pod offsets use); forward is the launcher side, left is positive.

### Belteshazzar

Measured 2026-10-15. Runtime values: `subsystems/VisionTuning` (on `master`).

| | Value |
|---|---|
| Forward | 6 3⁄4 in (6.75) |
| Left | 6 1⁄8 in (6.125) |
| Lens height above the tiles | 7 3⁄4 in (7.75) |
| Pitch | 33.3° below horizontal |
| Yaw / roll | 0° / 0° |
| Nearest visible floor | 4 in |
| Field of view (spec) | 54.5° × 42° |

Tilted down at the floor balls, this mount can't see the AprilTags under the CELLs.

### Ashurbanipal

| | Value |
|---|---|
| Forward | |
| Left | |
| Lens height above the tiles | |
| Pitch | |
| Yaw / roll | |

## SDK API (FTC SDK 12.0)

Pattern from the sample `SensorLimelight3A`; method list checked against the SDK's `Hardware-12.0.0`.
Package `com.qualcomm.hardware.limelightvision`.

**`Limelight3A`** (the device)

| Method | What it does |
|---|---|
| `hardwareMap.get(Limelight3A.class, RobotHardware.LIMELIGHT)` | look it up |
| `pipelineSwitch(int)` | select a pipeline |
| `start()` / `pause()` / `stop()` | start / stop polling. Without `start()` there is no data |
| `setPollRateHz(int)` | how often the hub polls it |
| `getLatestResult()` | latest `LLResult`. Never `null` since SDK 10.x: check `isValid()` |
| `getStatus()` | `LLStatus`: name, temperature, CPU, FPS, pipeline index and type |
| `isConnected()`, `getTimeSinceLastUpdate()` | health checks |
| `updateRobotOrientation(double yaw)` | feed IMU / Pinpoint heading for MegaTag2 |
| `uploadPipeline(String, Integer)`, `uploadFieldmap(LLFieldMap, Integer)`, `reloadPipeline()` | push config from code |
| `captureSnapshot(String)` | save an image for tuning |

**`LLResult`** (one frame)

| Method | What it gives |
|---|---|
| `isValid()` | false before the first data or with no target |
| `getTx()`, `getTy()` (and `getTxNC()`, `getTyNC()`) | target angle from the crosshair (NC: from the camera centre), degrees |
| `getBotpose()`, `getBotpose_MT2()` | robot pose from tags (`Pose3D`), MegaTag 1 / 2 |
| `getBotposeTagCount()`, `getBotposeAvgDist()` | how many tags and how far, to judge trust |
| `getStaleness()`, `getCaptureLatency()`, `getTargetingLatency()` | how old the frame is |
| `getPipelineIndex()` | which pipeline produced it. Check it after a switch |
| `getColorResults()` | pipeline 0: `ColorResult`s, `getTargetXDegrees()` / `getTargetYDegrees()` |
| `getFiducialResults()` | pipelines 1–2: `FiducialResult`s, `getFiducialId()`, `getTargetXDegrees()` |

## Frames

`getBotpose()` is in the FTC field frame (origin at field centre). Convert it into ours as
[coordinates.md](coordinates.md#converting-from-other-frames) describes. Check the `Pose3D`'s
distance unit before converting: Limelight reports metres.

## Our code

`subsystems/Vision` drives the Limelight; every number it uses is in `subsystems/VisionTuning`
(`ballPipeline`, `tagPipeline`, the mount). How the ball tracking works is in
[vision/readme.md](../TeamCode/src/main/java/org/firstinspires/ftc/teamcode/vision/readme.md).
