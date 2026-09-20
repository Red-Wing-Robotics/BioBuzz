# util/

Hardware-free helpers: math, state machines, timers, lookup tables.

Nothing in here may import `com.qualcomm.*` hardware classes — that is what makes it unit-testable
(`./gradlew :TeamCode:testDebugUnitTest`, tests in `TeamCode/src/test`).
