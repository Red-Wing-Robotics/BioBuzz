# Ball vision on the Limelight 3A: status and debugging handoff

Written 2026-11-01 to carry context into a new agent thread. Read `CLAUDE.md`, `RULES.md` and
`TeamCode/src/main/java/org/firstinspires/ftc/teamcode/vision/readme.md` (the design reference)
alongside this. This file covers what the readme doesn't: where the work stands, what we found on
the field, and what's still open. It's a scratch note: delete it or fold it into `docs/` when done.

## Goal

In AUTO, the robot drives to a waypoint and then **seeks** balls with the Limelight 3A:
- It should go for the **largest group** of wanted balls, not just the nearest one.
- The route's `colors` arg says what's wanted:
  - `"pollen"`: POLLEN only.
  - `"nectar"`: our alliance's NECTAR only.
  - `"ours"`: POLLEN and our NECTAR.
  
  (I offered to rename `"ours"` to something clearer, e.g. `"pollen+nectar"`. Not decided.)
- Anything not wanted is **ignored and avoided**. Opponent NECTAR is always avoided (G408).
- There's no intake yet. The robot drives to a stop pose with its front edge touching the target.

The test route is `TeamCode/src/main/assets/autos/vision_seek.json`. It goes START → LEAVE → PARK
(75, 124, 0°), then runs `seek vision.ball {"colors": "pollen"}` with a 4 s timeout, then drives
PARK → START. The OpMode is `opmodes/auto/VisionSeekAuto`. Jimothy pushes the route on save and can
INIT/START/STOP it remotely.

## Architecture (short; details in vision/readme.md)

Camera → `subsystems/Vision` → `vision/BallTracker` → `vision/DriveToBall` (a `Step`) →
`BallApproach` / `PickupRules`.

- **The camera:** the Hive-Vision SSD model runs in a Neural Detector pipeline, slot 0. Classes:
  `yellow_pollen`, `red_nectar`, `blue_nectar`. About 12 fps. It runs even when no OpMode is
  running.
- **`subsystems/Vision`** is the only code that touches SDK Limelight types.
  - It turns each new frame (keyed by timestamp) into a `Frame`.
  - It logs to the DS and to logcat (tag `Vision`).
- **`CameraMount`** projects tx/ty onto the field at the ball's centre height (POLLEN 1.4 in,
  NECTAR 1.8 in).
  - Mount: 6.75 in forward, 6.125 in left, 7.75 in high, pitched 33.3° down.
  - The image covers ball centres from about **4.5 to 29 in** ahead of the lens. Steering only uses
    **5–25 in**; detections within 2° of an image edge are "edge" detections.
- **`BallTracker`** tracks balls as field positions, using the pose at capture time
  (`PoseHistory`).
  - A ball is confirmed after 3 consecutive frames and remembered for 3 s after it's lost.
  - Each ball has a **blob count**: the box area over the area one ball should have at that range,
    times `blobAreaScale`. A box the model draws round touching balls counts as several balls.
- **`DriveToBall`** works in stages:
  1. Looks for `settleSeconds` (0.5 s).
  2. Chooses via `BallApproach.choose`. A group within `clusterRadius` (6 in) beats the nearest
     ball if it's at most `similarSeconds` (0.5 s) slower to reach.
  3. Drives to a stop pose `standoff` (9 in) short of the target, re-planning as the estimate
     firms up.
  4. Freezes the target inside `commit` (18 in).
  
  Until it commits, it switches to a bigger group if one gets confirmed. If a NECTAR is confirmed in
  the way, it re-chooses or abandons.
  
  It finishes with one of these results: ARRIVED, NO_TARGET (nothing chosen), ABANDONED.
- **`VisionTuning`** (`@Configurable`, live in Panels in dev builds) holds every number.

## State of the code

- The last commit is `8de2182` ("Ball vision on the Limelight, seek/driveTo route steps, Jimothy
  telemetry"). **Everything below is uncommitted** on `master`. The user commits only when they
  ask.
- Uncommitted changes:
  - `vision/`: BallApproach, BallTracker, Balls, DriveToBall, PickupRules, ToBallArgs, readme.
  - `subsystems/Vision.java` and `VisionTuning.java`.
  - `auto/Steps.java`: a new `Steps.atSpeed(fraction, step)` that caps Pedro Foresight's
    `maxPathSpeed` for one step. It's used for `driveSpeed` / a route's `"speed"`. Its test is
    `TeamCode/src/test/.../auto/AtSpeedTest.java`, which is untracked.
  - Tests: `BallApproachTest`, `BallTrackerTest`, `DriveToBallTest`.
  - `docs/autos.pdf` (regenerated).
  - `.claude/launch.json`: untracked and local. Keep it out of commits.
- Last run: **209 unit tests, 0 failures**, and the comp variant compiles.
- The latest build is **installed on the robot** (devDebug, 2026-11-01) but **not yet run on the
  field**.

### Temporary settings, to revert after field testing

Both are in `subsystems/VisionTuning.java`:
- `driveSpeed = 0.5`, so the turn toward a ball is easy to watch. Set it back to 1.
- `autoOwnHalfOnly = false`, which lets AUTO seeks cross the midline (G402). Set it back to `true`.
  - The DS shows a "vision WARNING" line while it's off.
  - Comp builds enforce the rule regardless.

## Field debugging timeline (2026-10-29 → 11-01)

1. **"≈0.0 balls" on every detection.**
   - Cause: the 3A reports `ta` as a **fraction** (0–1), not the 0–100 percent the SDK documents.
     The raw REST feed confirmed it. Confidence is 0–1 too.
   - Fix: `Vision` multiplies `getTargetArea()` by 100. A single ball now reads about "≈1.1
     balls".
   - `blobAreaScale` still needs a bench calibration (one ball should read ≈1.0).
2. **Group choice.**
   - Added a **settle time** (0.5 s), so a group gets its 3 frames before the choice is made.
   - Added **switching** to a bigger group confirmed on the way.
   - "vision pick" now lists the balls **passed over**, each with a reason: `✗ KIND (x, y):
     reason`.
3. **"It went after the nectar" on a pollen seek.**
   - Added avoidance. On a `"pollen"` seek, NECTAR is ignored for grouping and avoided: every part
     of the robot keeps `avoidMargin` (3 in) clear of it, at the stop pose and on the straight line
     there (`BallApproach.whyInTheWay`).
   - A POLLEN touching a NECTAR is therefore passed over.
4. **But the camera never reported that NECTAR.**
   - The logs showed no NECTAR detection at all. The user confirmed the NECTAR is detected as
     `red_nectar`, but it sat cut off at the **image edge**, where detections were being dropped.
   - Fix: **edge-only balls.** An edge detection still creates or updates a tracked ball flagged
     `edgeOnly`.
     - An edge-only ball is never chosen and never counts toward a group, but it *is* kept clear of
       if it's an unwanted colour.
     - An edge sighting of an ordinary tracked ball only refreshes it; its position isn't moved.
     - A proper sighting later clears the flag.
   - This is the build that was just deployed and is still untested.
5. **Logging added for diagnosis** (logcat tag `Vision`; see "Reading the logs" below).
   - A NO_TARGET / ABANDONED result now also logs `vision tracked` and `vision seen`.
   - Detections with an unknown class are reported (label, class id, raw confidence).

## Open questions / suspects

- **Phantom POLLEN.** From START, a persistent `yellow_pollen` shows up projected at about **(71–73,
  78–82)**.
  - Readings: conf 0.36–0.46, area 0.2–0.3 %, ty 17–19, so near the top of the image and far away.
    Our `minConfidence` is 0.44, so some frames get through.
  - **I asked the user whether a real ball is there; there's no answer yet.**
  - If it's not real, raise `minConfidence`, or add a minimum box area / maximum range for far
    detections.
- **Seek waypoints.** Choose them so the expected balls sit mid-image, not at the edges. Check a
  spot by parking there in `opmodes/test/BallVisionBench`, which shows "(edge: not used)" for
  edge detections.
- **The `"ours"` name.** Rename it if the team wants.
- **No intake yet.** G407 capacity and confirming a pickup come later (phase 4 of the plan).

## What to look for on the next run

Test layout: 3 POLLEN with a NECTAR beside them, plus one lone POLLEN; `"colors": "pollen"`.

- **If the NECTAR is seen**, even only at the edge:
  - `vision pick` should show `✗ POLLEN … would touch the RED_NECTAR at (x, y)` (or `drive over`).
  - The robot should go for the lone POLLEN.
  - `vision tracked` should show the NECTAR, marked `edge only` if it was only seen at the edge.
- **If it still goes for the group:** `vision seen` shows whether any NECTAR detection reached the
  robot at all, and what happened to each detection.

### Reading the logs

The DS and logcat (tag `Vision`) show these lines:
- `vision frame`: what happened to each detection in the latest frame (used, at the edge, gated),
  plus `; not our classes: …`.
- `vision pick`: every ball weighed, with its time and group size, then the passed-over balls
  with `✗` and a reason. A prefix of `switched:` or `round a ball in the way:` means a re-choice.
- `vision tracked`: every tracked ball. Example: `#3 POLLEN×2 (108, 125) sure | #4 POLLEN (110, 119)
  2 in a row, edge only`.
- `vision seen`: each detection. Example: `POLLEN 83% ta 8.485 ≈1.1 balls → #6`, or `at the edge →
  #N (new, edge only)`.
- `vision drive`: the result (ARRIVED / NO_TARGET / ABANDONED) and why.

## How to build, deploy and inspect

Gradle needs to run outside the sandbox (`dangerouslyDisableSandbox`). `--offline` works.

```bash
export JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home"
./gradlew :TeamCode:testDevDebugUnitTest :TeamCode:compileCompDebugJavaWithJavac --offline -q
```

**Before deploying, check the robot is idle.** Installing kills a running OpMode.

```bash
ADB=/Users/david/Library/Android/sdk/platform-tools/adb
$ADB -s 192.168.43.1:5555 forward tcp:18080 tcp:8080
curl -s http://127.0.0.1:18080/routeview/status    # want "phase":"IDLE"
```

```bash
ANDROID_SERIAL=192.168.43.1:5555 ./gradlew :TeamCode:installDevDebug --offline -q
```

After an install, the app takes about 20–40 s to answer the status endpoint again. Deploy only when
the user says yes.

**Robot logs:**

```bash
$ADB -s 192.168.43.1:5555 logcat -d -s Vision
```

**The Limelight directly**, over the Control Hub's Wi-Fi:
- `http://192.168.43.1:5807/results` gives the raw JSON, e.g. `"Detector":[{class, classID, conf,
  ta, tx, ty}]`. `/status` also works.
- The web UI on 5801 loads but stays stuck on "Connecting…".
- `pipeline-atindex` returns 400/500.
- **Don't change the camera config** from here. The user hasn't asked for it, so it was
  deliberately left alone.

A sampler that polls `/results` for N seconds and summarizes each class:

```bash
cat > $TMPDIR/llsample.py <<'EOF'
import json, sys, time, urllib.request, collections
secs = float(sys.argv[1]) if len(sys.argv) > 1 else 3
seen = collections.defaultdict(list)
frames = 0
end = time.time() + secs
while time.time() < end:
    try:
        d = json.load(urllib.request.urlopen("http://192.168.43.1:5807/results", timeout=2))
    except Exception as e:
        print("error", e); time.sleep(0.2); continue
    frames += 1
    for r in d.get("Detector", []):
        seen[(r.get("class"), r.get("classID"))].append((r.get("conf"), r.get("ta") * 100, r.get("tx"), r.get("ty")))
    time.sleep(0.08)
print(f"{frames} polls in {secs:.0f} s")
if not seen:
    print("no detections")
for (cls, cid), hits in sorted(seen.items(), key=lambda kv: -len(kv[1])):
    confs = sorted(h[0] for h in hits)
    print(f"{cls!r} class {cid}: {len(hits)} hits, conf {confs[0]:.2f}-{confs[-1]:.2f}, "
          f"area {min(h[1] for h in hits):.1f}-{max(h[1] for h in hits):.1f} %, "
          f"tx {min(h[2] for h in hits):.0f}..{max(h[2] for h in hits):.0f}, ty {min(h[3] for h in hits):.0f}..{max(h[3] for h in hits):.0f}")
EOF
python3 $TMPDIR/llsample.py 3
```

## Background references

- `docs/decisions.md`, 2026-10-15: why we use Hive-Vision's model and tracking ideas but our own
  driving.
- The original plan, with phases 0–4: `/Users/david/.claude/plans/examine-this-library-plan-rippling-cook.md`.
  Phases 1–3 are built. Phase 4 (intake) waits on hardware.
- `docs/hardware.md`: the Limelight mount measurements.
- `opmodes/test/readme.md`: bench calibration with `BallVisionBench`.
- Robot size 18×18 in. Coordinates are in the RED frame (`docs/coordinates.md`).
