package org.firstinspires.ftc.teamcode;

import com.qualcomm.hardware.lynx.LynxModule;
import com.qualcomm.robotcore.hardware.HardwareMap;

import java.util.List;

/**
 * Single source of truth for the competition robot's hardware configuration names and device
 * lookups.
 *
 * Config names must match the Robot Controller configuration exactly and are
 * documented in docs/hardware.md. Subsystems receive devices from here; nothing
 * else in TeamCode should call hardwareMap.get(...) with a string literal. The StarterBot, a
 * separate robot, has its own names in opmodes/starterbot/StarterBotHardware.
 */
public class RobotHardware {

    // ---- Control Hub ----------------------------------------------------------------
    public static final String FRONT_RIGHT_DRIVE = "frontRightMotor"; // motor 0
    public static final String BACK_RIGHT_DRIVE  = "backRightMotor";  // motor 1
    //                                                                   motor 2 unused
    public static final String RIGHT_SHOOTER     = "rightShooter";    // motor 3
    public static final String RIGHT_FEEDER      = "rightFeeder";     // servo 0
    public static final String DIVERTER          = "diverter";        // servo 1
    public static final String LIGHT             = "light";           // servo 2 (PWM light)
    public static final String LIMELIGHT         = "limelight";       // Ethernet

    // ---- Expansion Hub --------------------------------------------------------------
    public static final String FRONT_LEFT_DRIVE  = "frontLeftMotor";  // motor 0
    public static final String BACK_LEFT_DRIVE   = "backLeftMotor";   // motor 1
    public static final String LEFT_SHOOTER      = "leftShooter";     // motor 2
    public static final String INTAKE            = "intake";          // motor 3
    public static final String LEFT_FEEDER       = "leftFeeder";      // servo 0
    public static final String PINPOINT          = "pinpoint";        // I2C bus 2 (goBILDA odometry computer)

    private final HardwareMap hardwareMap;
    private final List<LynxModule> hubs;

    public RobotHardware(HardwareMap hardwareMap) {
        this.hardwareMap = hardwareMap;
        this.hubs = hardwareMap.getAll(LynxModule.class);
        // Bulk reads: one hardware transaction per hub per loop instead of one per read.
        // Callers must invoke clearBulkCache() exactly once at the top of every loop.
        for (LynxModule hub : hubs) {
            hub.setBulkCachingMode(LynxModule.BulkCachingMode.MANUAL);
        }
    }

    /** Call once at the start of every loop iteration so cached sensor reads refresh. */
    public void clearBulkCache() {
        for (LynxModule hub : hubs) {
            hub.clearBulkCache();
        }
    }

    public HardwareMap getHardwareMap() {
        return hardwareMap;
    }
}
