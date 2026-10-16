package org.firstinspires.ftc.teamcode;

import org.firstinspires.ftc.robotcore.internal.system.AppUtil;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Locale;

/**
 * Which of our two competition robots this Control Hub is in. Both run the same APK and the same
 * OpModes; per-robot values (Pedro tuning, see pedro/Constants) are picked by this.
 *
 * The hub says which robot it is in a one-line file, {@code /sdcard/FIRST/robot.txt}, beside its
 * Robot Controller configurations. APK installs, SDK updates and route pushes leave it alone. Set
 * it once per hub with the {@code Robot ID: ...} OpModes (group "setup") or
 * {@code adb shell "echo belteshazzar > /sdcard/FIRST/robot.txt"}. The ID belongs to the hub, so
 * set it again after moving a hub to the other robot.
 */
public enum RobotId {
    /** The first robot. */
    BELTESHAZZAR("Belteshazzar"),
    /** The second robot. */
    ASHURBANIPAL("Ashurbanipal");

    public static final String FILE_NAME = "robot.txt";

    /** How the name is shown on the Driver Station. */
    public final String label;

    RobotId(String label) {
        this.label = label;
    }

    /** What goes in the file. */
    public String fileText() {
        return name().toLowerCase(Locale.US);
    }

    @Override
    public String toString() {
        return label;
    }

    /** The robot named by {@code text} (any case, surrounding whitespace ignored), or null. */
    public static RobotId parse(String text) {
        String name = text.trim();
        for (RobotId id : values()) {
            if (id.name().equalsIgnoreCase(name)) {
                return id;
            }
        }
        return null;
    }

    /** The ID file on this Control Hub. */
    public static File hubFile() {
        return new File(AppUtil.FIRST_FOLDER, FILE_NAME);
    }

    /** This hub's robot. Throws, with how to fix it, if the ID file is missing or unrecognised. */
    public static RobotId onHub() {
        return read(hubFile());
    }

    /** Sets this hub's robot. */
    public static void setOnHub(RobotId id) throws IOException {
        write(hubFile(), id);
    }

    static RobotId read(File file) {
        if (!file.isFile()) {
            throw new IllegalStateException("No robot ID: " + file + " is missing. " + HOW_TO_SET);
        }
        String text;
        try (InputStream in = new FileInputStream(file)) {
            ByteArrayOutputStream bytes = new ByteArrayOutputStream();
            byte[] buffer = new byte[64];
            int n;
            while ((n = in.read(buffer)) != -1) {
                bytes.write(buffer, 0, n);
            }
            text = new String(bytes.toByteArray(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new IllegalStateException("Cannot read robot ID " + file + ". " + HOW_TO_SET, e);
        }
        RobotId id = parse(text);
        if (id == null) {
            throw new IllegalStateException("Unknown robot ID \"" + text.trim() + "\" in " + file + ". " + HOW_TO_SET);
        }
        return id;
    }

    static void write(File file, RobotId id) throws IOException {
        try (OutputStream out = new FileOutputStream(file)) {
            out.write((id.fileText() + "\n").getBytes(StandardCharsets.UTF_8));
        }
    }

    private static final String HOW_TO_SET =
            "Run the \"Robot ID: Belteshazzar\" or \"Robot ID: Ashurbanipal\" OpMode (group setup) once on this robot.";
}
