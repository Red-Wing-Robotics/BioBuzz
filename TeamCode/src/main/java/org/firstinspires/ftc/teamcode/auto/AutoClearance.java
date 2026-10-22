package org.firstinspires.ftc.teamcode.auto;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * What AUTO's planned drives keep clear of: a route's {@code "driveTo"} steps and its vision
 * drives ({@code "seek"}), whose paths the robot works out with {@link DrivePlan} instead of
 * reading them from the route. Pure Java: tools/routeview compiles it too, so the auto maps draw
 * the same plans.
 */
public final class AutoClearance {

    /**
     * Least gap, inches, between any part of the robot and the HIVE outline on a planned drive.
     * PLACEHOLDER: autos have only been drawn, not driven, near the HIVE. Raise it if one clips.
     */
    public static final double MARGIN = 3;

    /**
     * In {@code alliance}'s frame: the HIVE's whole outline ({@link Field#HIVE}; unlike TeleOp's
     * assists, autos stay out from under it) and the other alliance's half (G402). That half's
     * edge is moved {@link #MARGIN} into it, so the robot may drive right up to the centre line
     * but never over it.
     */
    public static List<KeepOut> obstacles(Alliance alliance) {
        KeepOut other = Field.opponentHalf(alliance);
        KeepOut edge = alliance == Alliance.RED
                ? new KeepOut(other.minX + MARGIN, other.minY, other.maxX, other.maxY)
                : new KeepOut(other.minX, other.minY, other.maxX - MARGIN, other.maxY);
        return Collections.unmodifiableList(Arrays.asList(Field.HIVE, edge));
    }

    private AutoClearance() {}
}
