package org.firstinspires.ftc.teamcode.util;

import java.util.Locale;

/**
 * One row of the shot log: where the robot was, how fast the flywheels were spinning, and
 * whether the shot scored. Collected in TeleOp to fit flywheel speed against distance to the
 * HIVE. Hardware-free: the TeleOp fills the fields, this class turns them into CSV.
 */
public final class ShotRecord {

    /** The CSV header; {@link #csvRow()} writes the columns in this order. */
    public static final String HEADER = "time,match_time_left_s,robot,alliance,cell,aiming,"
            + "x_in,y_in,heading_deg,distance_in,target_velocity,top_velocity,bottom_velocity,"
            + "aim_error_deg,result";

    /** Wall-clock time of the press, as the hub reports it. */
    public String time;
    public double matchTimeLeftSeconds;
    public String robot;
    public String alliance;
    /** The CELL the distance is measured to: the aimed-at one, else the nearer one. */
    public String cell;
    /** The heading aim was engaged; if not, {@link #aimErrorDegrees} is left blank. */
    public boolean aiming;
    /** Robot pose, inches and degrees, in the Follower's frame. */
    public double x;
    public double y;
    public double headingDegrees;
    /** Straight-line floor distance from the robot's centre to {@link #cell}'s centre, inches. */
    public double distance;
    /** Flywheel velocities, encoder ticks/s. */
    public double targetVelocity;
    public double topVelocity;
    public double bottomVelocity;
    public double aimErrorDegrees;
    public String result;

    /** Floor distance between two points, inches. */
    public static double distance(double x1, double y1, double x2, double y2) {
        return Math.hypot(x2 - x1, y2 - y1);
    }

    /** This record as one CSV line (no line break), columns as in {@link #HEADER}. */
    public String csvRow() {
        return String.join(",",
                text(time),
                number(matchTimeLeftSeconds, 1),
                text(robot),
                text(alliance),
                text(cell),
                aiming ? "yes" : "no",
                number(x, 2),
                number(y, 2),
                number(headingDegrees, 1),
                number(distance, 2),
                number(targetVelocity, 0),
                number(topVelocity, 0),
                number(bottomVelocity, 0),
                aiming ? number(aimErrorDegrees, 1) : "",
                text(result));
    }

    private static String number(double value, int decimals) {
        return String.format(Locale.US, "%." + decimals + "f", value);
    }

    /** Quotes a text field if it holds a comma, quote or line break (RFC 4180). */
    static String text(String value) {
        if (value == null) {
            return "";
        }
        if (value.contains(",") || value.contains("\"") || value.contains("\n") || value.contains("\r")) {
            return "\"" + value.replace("\"", "\"\"") + "\"";
        }
        return value;
    }
}
