# Third-party notices

## Hive-Vision

`TeamCode/src/main/java/org/firstinspires/ftc/teamcode/vision/BallTracker.java` and
`CameraMount.java` are adapted from [Hive-Vision](https://github.com/hivevisionftc/Hive-Vision)'s
`ftc_ball_chase_lib` (its tracking and ground-range code). The rest of `vision/` follows its
approach. Hive-Vision's licence:

```
MIT License

This license applies to the original Hive Vision source code, documentation,
and configuration authored for this repository. The YOLO model artifacts and
Ultralytics-dependent material are covered separately; see
`THIRD_PARTY_NOTICES.md`.

Copyright (c) 2026 Harjas Sidhu

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all
copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
SOFTWARE.
```

**The ball model** (`best_limelight3a_ssd_mobilenetv2_300x300.tflite`, release v1.1.0) runs on the
Limelight and is not in this repository or the APK. Hive-Vision's notices put its model folder
under AGPL-3.0 (Ultralytics). Treat the weights as AGPL-3.0 until the author says otherwise, and
don't redistribute them without that licence.
