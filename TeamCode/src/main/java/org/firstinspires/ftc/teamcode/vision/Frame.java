package org.firstinspires.ftc.teamcode.vision;

import java.util.Collections;
import java.util.List;

/**
 * One camera frame's detections. The SDK hands back the same frame several times (it polls the
 * camera faster than the model runs), so {@link #id}, the camera's own timestamp, lets each frame
 * be used once.
 */
public final class Frame {

    /** The camera's timestamp for the frame: equal ids are the same frame. */
    public final double id;
    /** When the image was taken, in ms on the clock the robot's poses are recorded on. */
    public final double captureMs;
    public final List<Detection> detections;

    public Frame(double id, double captureMs, List<Detection> detections) {
        this.id = id;
        this.captureMs = captureMs;
        this.detections = Collections.unmodifiableList(detections);
    }
}
