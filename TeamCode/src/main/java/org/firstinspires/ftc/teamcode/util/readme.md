# util/

Hardware-free helpers: math, state machines, timers, lookup tables.

Nothing in here may import `com.qualcomm.*` hardware classes — that is what makes it unit-testable
(`./gradlew :TeamCode:testDebugUnitTest`, tests in `TeamCode/src/test`).

- `ShotRecord`: one row of the TeleOp shot log (distance vs flywheel speed), as CSV.
- `CsvLog`: appends rows to a CSV file, header first if new. Plain `java.io`, tested with a temp folder.
