# opmodes/setup/

One-time Control Hub setup, group `"setup"`, in both builds. Nothing here moves the robot.

- `SetRobotId` — `Robot ID: Belteshazzar` / `Robot ID: Ashurbanipal`. Tells the hub which robot
  it is in by writing `/sdcard/FIRST/robot.txt` (`RobotId`). INIT shows the current ID; START
  writes the new one. Run it once per hub, and again after moving a hub to the other robot. Every
  comp OpMode refuses to INIT until it is set. By adb instead:
  `adb shell "echo ashurbanipal > /sdcard/FIRST/robot.txt"`.
