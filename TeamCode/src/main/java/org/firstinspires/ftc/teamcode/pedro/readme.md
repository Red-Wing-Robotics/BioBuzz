# pedro/

PedroPathing 3.0 configuration and tuning.

- `Constants.java` — `drivetrainConfig` (MecanumConfig), `localizerConfig` (PinpointConfig),
  `foresightConfig`, and `create(hardwareMap)` which builds the `Follower`. Tuned 2026-09-20.
- `Tuning.java` and `procedures/` (vendored from the Pedro Quickstart) live in the **dev source
  set**, `TeamCode/src/dev/java/.../pedro/`, because the tuning web server must not ship in
  competition builds (R704.D). They are only compiled into the `devDebug` variant.

## Running the tuners

The tuning UI is a **web page served by the Robot Controller**, not a Driver Station OpMode:

1. Deploy the **devDebug** variant and connect your laptop to the Control Hub's Wi-Fi.
2. Open **http://192.168.43.1:10158** (same address as the RC console, different port).
3. Run **Mecanum Tuner → Pinpoint Tuner → Foresight Tuner**, in that order — later tuners drive
   the robot using the earlier results. Each ends with a Java snippet: paste it into
   `Constants.java` (see the comments there for exactly where) and record the numbers in
   `docs/hardware.md`.
4. Run **Tests** (hold / line / path) to verify the tuned Follower before writing autos.

The UI launches its own OpModes through the DS; the Driver Station must be connected with the
robot enabled. Nothing from the tuning library runs during a normal match OpMode, but the
tuning web server is still active whenever the app is — this is telemetry infrastructure the
R704 Wi-Fi rule cares about, so do not open it during a match.

## API notes

The 3.0 API differs from the 2.x docs/examples still common online. When unsure, read the
library source: `~/.gradle/caches/modules-2/files-2.1/com.pedropathing/revhub/3.0.1/*/revhub-3.0.1-sources.jar`
(core has no sources jar — use `javap`).
