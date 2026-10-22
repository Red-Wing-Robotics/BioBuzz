package org.firstinspires.ftc.teamcode.subsystems;

import com.qualcomm.robotcore.util.RobotLog;

import org.firstinspires.ftc.robotcore.external.Telemetry;

import java.util.ArrayList;
import java.util.List;

/** The subsystems an OpMode has registered, run in the order they were added. */
public final class Subsystems {

    private final List<Subsystem> all = new ArrayList<>();

    public <T extends Subsystem> T add(T subsystem) {
        all.add(subsystem);
        return subsystem;
    }

    public void periodic() {
        for (Subsystem s : all) {
            s.periodic();
        }
    }

    public void status(Telemetry telemetry) {
        for (Subsystem s : all) {
            s.status(telemetry);
        }
    }

    /** Stops every subsystem. One that throws is logged and the rest still stop, so the OpMode's own stop work still runs. */
    public void stop() {
        for (Subsystem s : all) {
            try {
                s.stop();
            } catch (RuntimeException e) {
                RobotLog.ee("Subsystems", e, "%s.stop() failed", s.getClass().getSimpleName());
            }
        }
    }
}
