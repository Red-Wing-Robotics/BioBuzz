package org.firstinspires.ftc.teamcode.opmodes.setup;

import com.qualcomm.robotcore.eventloop.opmode.OpMode;
import com.qualcomm.robotcore.eventloop.opmode.TeleOp;

import org.firstinspires.ftc.teamcode.RobotId;

import java.io.IOException;

/**
 * Tells this Control Hub which robot it is in ({@link RobotId}), from the Driver Station. Run the
 * right one once per hub; the setting survives APK installs. INIT shows the current ID, START
 * writes the new one. Touches no hardware, so it is in both builds.
 */
public abstract class SetRobotId extends OpMode {

    private final RobotId target;
    private String current;
    private String result = "not changed — press START to set it";

    SetRobotId(RobotId target) {
        this.target = target;
    }

    @Override
    public void init() {
        current = current();
    }

    @Override
    public void init_loop() {
        telemetry.addData("this hub is", current);
        telemetry.addData("START sets it to", target);
    }

    @Override
    public void start() {
        try {
            RobotId.setOnHub(target);
            result = "set to " + RobotId.onHub();
        } catch (IOException | IllegalStateException e) {
            result = "FAILED: " + e.getMessage();
        }
    }

    @Override
    public void loop() {
        telemetry.addData("robot ID", result);
        telemetry.addData("file", RobotId.hubFile());
    }

    private static String current() {
        try {
            return RobotId.onHub().label;
        } catch (IllegalStateException e) {
            return "not set";
        }
    }

    @TeleOp(name = "Robot ID: Belteshazzar", group = "setup")
    public static class Belteshazzar extends SetRobotId {
        public Belteshazzar() {
            super(RobotId.BELTESHAZZAR);
        }
    }

    @TeleOp(name = "Robot ID: Ashurbanipal", group = "setup")
    public static class Ashurbanipal extends SetRobotId {
        public Ashurbanipal() {
            super(RobotId.ASHURBANIPAL);
        }
    }
}
