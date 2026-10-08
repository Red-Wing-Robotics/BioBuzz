package org.firstinspires.ftc.teamcode.automaps;

import static org.firstinspires.ftc.teamcode.automaps.FieldPainter.BAD;
import static org.firstinspires.ftc.teamcode.automaps.FieldPainter.GOOD;
import static org.firstinspires.ftc.teamcode.automaps.FieldPainter.INK;
import static org.firstinspires.ftc.teamcode.automaps.FieldPainter.MUTED;
import static org.firstinspires.ftc.teamcode.automaps.FieldPainter.RULE;
import static org.firstinspires.ftc.teamcode.automaps.FieldPainter.WHITE;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import com.pedropathing.math.Pose;
import com.qualcomm.robotcore.eventloop.opmode.Autonomous;
import com.qualcomm.robotcore.eventloop.opmode.Disabled;

import org.firstinspires.ftc.teamcode.auto.Alliance;
import org.firstinspires.ftc.teamcode.auto.JsonAutoOpMode;
import org.firstinspires.ftc.teamcode.auto.route.Route;
import org.firstinspires.ftc.teamcode.auto.route.RouteParser;
import org.firstinspires.ftc.teamcode.automaps.Pdf.Align;
import org.firstinspires.ftc.teamcode.automaps.Pdf.Color;
import org.firstinspires.ftc.teamcode.automaps.Pdf.Font;
import org.firstinspires.ftc.teamcode.automaps.Pdf.Page;
import org.junit.Test;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;

/**
 * Draws every JSON autonomous OpMode into one PDF: a cover page, then a page per OpMode with its
 * path on the BIOBUZZ field, estimated points and time, routine and rule checks. The spec, and how
 * to change any of this, is docs/auto-maps.md.
 *
 * Runs with the unit tests. {@code ./gradlew :TeamCode:autoMaps} writes docs/autos.pdf (the
 * {@code autoMaps.output} system property, set in TeamCode/build.gradle); other runs write
 * TeamCode/build/auto-maps/autos.pdf. Paths are relative to the TeamCode module directory,
 * which is the working directory for unit tests.
 */
public class AutoMapsPdf {

    private static final String OPMODE_PACKAGE = "org.firstinspires.ftc.teamcode.opmodes.auto";
    private static final File OPMODE_SOURCES = new File("src/main/java/org/firstinspires/ftc/teamcode/opmodes/auto");
    private static final File ROUTE_FILES = new File("src/main/assets/autos");

    private static final double W = 792;  // US Letter, landscape
    private static final double H = 612;
    private static final double M = 30;

    /** One autonomous OpMode as the Driver Station lists it. */
    static final class AutoOpMode {
        String name;
        String group;
        String preselectTeleOp;
        String className;
        String routeName;
        Alliance alliance;
        AutoRun run;
    }

    @Test
    public void drawEveryAutonomous() throws Exception {
        assertTrue("run from the TeamCode module directory: " + new File(".").getAbsolutePath(), ROUTE_FILES.isDirectory());
        Map<String, Route> routes = loadRoutes();
        List<AutoOpMode> opModes = findAutoOpModes();
        assertFalse("no JSON autonomous OpModes found in " + OPMODE_SOURCES, opModes.isEmpty());

        List<String> unused = new ArrayList<>(routes.keySet());
        for (AutoOpMode op : opModes) {
            Route route = routes.get(op.routeName);
            assertTrue(op.className + " uses route \"" + op.routeName + "\" but " + ROUTE_FILES + " has no such file", route != null);
            op.run = new AutoRun(route, op.alliance);
            unused.remove(op.routeName);
        }

        Pdf pdf = new Pdf("BIOBUZZ autonomous routes");
        int pages = opModes.size() + 1;
        drawCover(pdf.newPage(W, H), opModes, unused, pages);
        for (int i = 0; i < opModes.size(); i++) {
            drawOpMode(pdf.newPage(W, H), opModes.get(i), i + 2, pages);
        }

        File out = new File(System.getProperty("autoMaps.output", "build/auto-maps/autos.pdf"));
        File dir = out.getAbsoluteFile().getParentFile();
        assertTrue("cannot create " + dir, dir.isDirectory() || dir.mkdirs());
        try (OutputStream stream = new FileOutputStream(out)) {
            stream.write(pdf.toBytes());
        }
        System.out.println("Autonomous maps: " + out.getCanonicalPath());
        for (AutoOpMode op : opModes) {
            System.out.printf(Locale.US, "  %-28s %2d pts  %s  %d problem(s)%n", op.name, op.run.points,
                    AutoRun.secondsText(op.run.seconds), op.run.problems.size());
        }
    }

    // ---- finding the OpModes and routes ---------------------------------------------------

    private static Map<String, Route> loadRoutes() throws IOException {
        Map<String, Route> routes = new TreeMap<>();
        File[] files = ROUTE_FILES.listFiles((d, n) -> n.endsWith(".json"));
        for (File f : files == null ? new File[0] : files) {
            String json = new String(Files.readAllBytes(f.toPath()), StandardCharsets.UTF_8);
            routes.put(f.getName().replace(".json", ""), RouteParser.parse(json));
        }
        return routes;
    }

    /** Every non-abstract, enabled {@link JsonAutoOpMode} in opmodes/auto, including nested Red/Blue classes. */
    private static List<AutoOpMode> findAutoOpModes() throws Exception {
        List<AutoOpMode> found = new ArrayList<>();
        File[] sources = OPMODE_SOURCES.listFiles((d, n) -> n.endsWith(".java"));
        for (File source : sources == null ? new File[0] : sources) {
            Class<?> outer = Class.forName(OPMODE_PACKAGE + "." + source.getName().replace(".java", ""));
            List<Class<?>> candidates = new ArrayList<>();
            candidates.add(outer);
            candidates.addAll(Arrays.asList(outer.getDeclaredClasses()));
            for (Class<?> c : candidates) {
                Autonomous annotation = c.getAnnotation(Autonomous.class);
                if (annotation == null || c.isAnnotationPresent(Disabled.class)
                        || !JsonAutoOpMode.class.isAssignableFrom(c) || Modifier.isAbstract(c.getModifiers())) {
                    continue;
                }
                Object opMode = withoutConstructor(c);
                AutoOpMode op = new AutoOpMode();
                op.name = annotation.name().isEmpty() ? c.getSimpleName() : annotation.name();
                op.group = annotation.group();
                op.preselectTeleOp = annotation.preselectTeleOp();
                op.className = c.getName().substring(OPMODE_PACKAGE.length() + 1).replace('$', '.');
                op.routeName = (String) call(opMode, "routeName");
                op.alliance = (Alliance) call(opMode, "alliance");
                found.add(op);
            }
        }
        Collections.sort(found, (a, b) -> {
            int byRoute = a.routeName.compareTo(b.routeName);
            return byRoute != 0 ? byRoute : a.alliance.compareTo(b.alliance);
        });
        return found;
    }

    /**
     * An OpMode's constructor creates SDK objects that need Android, which unit tests don't have,
     * so the instance is allocated without running it. That is safe here: routeName() and
     * alliance() only return constants.
     */
    private static Object withoutConstructor(Class<?> c) throws Exception {
        Class<?> unsafeClass = Class.forName("sun.misc.Unsafe");
        Field field = unsafeClass.getDeclaredField("theUnsafe");
        field.setAccessible(true);
        return unsafeClass.getMethod("allocateInstance", Class.class).invoke(field.get(null), c);
    }

    private static Object call(Object target, String method) throws Exception {
        for (Class<?> c = target.getClass(); c != null; c = c.getSuperclass()) {
            try {
                Method m = c.getDeclaredMethod(method);
                if (!Modifier.isAbstract(m.getModifiers())) {
                    m.setAccessible(true);
                    return m.invoke(target);
                }
            } catch (NoSuchMethodException ignored) {
                // keep looking up the hierarchy
            }
        }
        throw new NoSuchMethodException(target.getClass().getName() + "." + method + "()");
    }

    // ---- cover page -------------------------------------------------------------------------

    private static void drawCover(Page page, List<AutoOpMode> opModes, List<String> unusedRoutes, int pages) {
        double y = H - M - 24;
        page.text(Font.BOLD, 26, INK, M, y, Align.LEFT, "BIOBUZZ Autonomous Routes");
        y -= 18;
        y = paragraph(page, Font.REGULAR, 10, MUTED, M, y, W - 2 * M,
                "One page per autonomous OpMode, drawn from the JSON route files the robot runs and the Pedro paths it follows. "
                        + "Points and times are estimates; the checks catch rule and field problems before the robot does.");
        y -= 16;

        double[] cols = {M, M + 210, M + 360, M + 450, M + 530};
        String[] heads = {"OPMODE", "ROUTE FILE", "EST. POINTS", "EST. TIME", "CHECKS"};
        for (int i = 0; i < cols.length; i++) {
            page.text(Font.BOLD, 7.5, MUTED, cols[i], y, Align.LEFT, heads[i]);
        }
        y -= 5;
        rule(page, M, y, W - M);
        for (AutoOpMode op : opModes) {
            y -= 14;
            page.fill(FieldPainter.color(op.alliance)).rect(cols[0], y - 1, 6, 8).fillPath();
            page.text(Font.BOLD, 9.5, INK, cols[0] + 11, y, Align.LEFT, op.name);
            page.text(Font.REGULAR, 9, INK, cols[1], y, Align.LEFT, op.routeName + ".json");
            page.text(Font.BOLD, 9.5, INK, cols[2], y, Align.LEFT, op.run.points + " pts");
            page.text(Font.REGULAR, 9, INK, cols[3], y, Align.LEFT, AutoRun.secondsText(op.run.seconds) + (op.run.timeUnknown ? " + ?" : ""));
            int n = op.run.problems.size();
            page.text(Font.BOLD, 9, n == 0 ? GOOD : BAD, cols[4], y, Align.LEFT,
                    n == 0 ? "no problems" : n + (n == 1 ? " problem" : " problems") + " (see page)");
            y -= 5;
            rule(page, M, y, W - M);
        }
        if (!unusedRoutes.isEmpty()) {
            y -= 14;
            page.text(Font.REGULAR, 9, MUTED, M, y, Align.LEFT, "Route files no OpMode uses (not drawn): " + String.join(", ", unusedRoutes));
        }

        double top = y - 30;
        double colW = (W - 2 * M - 30) / 2;
        drawLegend(page, M, top, colW);
        drawAssumptions(page, M + colW + 30, top, colW);
        footer(page, "Regenerate: ./gradlew :TeamCode:autoMaps   ·   How this is made, and how to change it: docs/auto-maps.md", 1, pages);
    }

    private static void drawLegend(Page page, double x, double y, double width) {
        page.text(Font.BOLD, 7.5, MUTED, x, y, Align.LEFT, "HOW TO READ A PAGE");
        y -= 6;
        rule(page, x, y, x + width);
        Color red = FieldPainter.color(Alliance.RED);
        String[] labels = {
                "Path the robot follows, in step order; the chevron points the way it drives.",
                "Robot at START (18 x 18 in max, R102); the thick edge is its front.",
                "Robot after each path.",
                "Robot at the end of AUTO, where LEAVE and PARK are judged.",
                "Routine step number, matching the list on the page.",
                "Waypoint the robot passes through; hollow means a Bezier control point it doesn't reach."};
        for (int i = 0; i < labels.length; i++) {
            y -= 19;
            double sx = x + 4, sy = y + 2;
            switch (i) {
                case 0:
                    page.stroke(red).lineWidth(1.7).roundCaps().moveTo(sx, sy).lineTo(sx + 24, sy).strokePath();
                    page.moveTo(sx + 10, sy + 4).lineTo(sx + 15, sy).lineTo(sx + 10, sy - 4).strokePath().buttCaps();
                    break;
                case 1:
                    page.stroke(red).lineWidth(0.9).dash(3, 2).rect(sx + 5, sy - 6, 12, 12).strokePath().solid();
                    page.lineWidth(2.6).moveTo(sx + 17, sy - 6).lineTo(sx + 17, sy + 6).strokePath();
                    break;
                case 2:
                    page.stroke(red).lineWidth(0.5).rect(sx + 5, sy - 6, 12, 12).strokePath();
                    page.lineWidth(1.4).moveTo(sx + 17, sy - 6).lineTo(sx + 17, sy + 6).strokePath();
                    break;
                case 3:
                    page.fill(FieldPainter.tint(Alliance.RED)).stroke(red).lineWidth(1.1).rect(sx + 5, sy - 6, 12, 12).fillAndStroke();
                    page.lineWidth(2.6).moveTo(sx + 17, sy - 6).lineTo(sx + 17, sy + 6).strokePath();
                    break;
                case 4:
                    page.fill(red).stroke(WHITE).lineWidth(0.8).circle(sx + 11, sy, 5.6).fillAndStroke();
                    page.text(Font.BOLD, 6.5, WHITE, sx + 11, sy - 2.3, Align.CENTER, "2");
                    break;
                default:
                    page.fill(red).circle(sx + 6, sy, 2.2).fillPath();
                    page.stroke(red).lineWidth(0.9).fill(WHITE).circle(sx + 16, sy, 2.2).fillAndStroke();
            }
            paragraph(page, Font.REGULAR, 8.5, INK, x + 36, y, width - 36, labels[i]);
        }
    }

    private static void drawAssumptions(Page page, double x, double y, double width) {
        page.text(Font.BOLD, 7.5, MUTED, x, y, Align.LEFT, "WHAT THE NUMBERS ASSUME");
        y -= 6;
        rule(page, x, y, x + width);
        String[] lines = {
                String.format(Locale.US, "Field: docs/coordinates.md. %.0f in square, origin at the red GARDEN corner, "
                        + "x toward the blue wall, y away from the audience, heading 0° = facing +x.", BiobuzzField.SIZE),
                String.format(Locale.US, "Robot: %.0f x %.0f in. Time per path: accelerate at %.0f in/s² to %.0f in/s, decelerate, "
                                + "then %.2f s to settle. Action and waitUntil times aren't known (shown as ?).",
                        MapSettings.ROBOT_LENGTH, MapSettings.ROBOT_WIDTH, MapSettings.ACCEL, MapSettings.SPEED, MapSettings.SETTLE),
                "Points: LEAVE 3 if the robot ends clear of every wall; PARK 5 if it ends partly in its own LOADING ZONE; "
                        + "HIVE tips aren't estimated until routes have launch actions.",
                "Routes come from TeamCode/src/main/assets/autos. A copy pushed to the Control Hub (/sdcard/FIRST/autos) "
                        + "overrides it on the robot but isn't drawn here."};
        for (String line : lines) {
            y -= 13;
            y = paragraph(page, Font.REGULAR, 8.5, INK, x, y, width, line) - 3;
        }
    }

    // ---- OpMode page ------------------------------------------------------------------------

    private static void drawOpMode(Page page, AutoOpMode op, int pageNumber, int pages) {
        AutoRun run = op.run;
        double titleY = H - M - 20;
        page.fill(FieldPainter.color(op.alliance)).rect(M, titleY - 2, 9, 18).fillPath();
        page.text(Font.BOLD, 20, INK, M + 15, titleY, Align.LEFT, op.name);
        String sub = op.routeName + ".json  ·  route “" + run.route.name + "”  ·  " + op.className
                + (op.preselectTeleOp.isEmpty() ? "" : "  ·  then TeleOp “" + op.preselectTeleOp + "”")
                + (op.group.isEmpty() ? "" : "  ·  group " + op.group);
        page.text(Font.REGULAR, 8.5, MUTED, M + 15, titleY - 15, Align.LEFT, sub);

        double side = 468;
        double fx = M + 30;
        double fy = M + 32;
        FieldPainter painter = new FieldPainter(page, fx, fy, side);
        painter.drawField();
        painter.drawRun(run);

        double px = fx + side + 34;
        double pw = W - M - px;
        double y = fy + side;
        double floor = M + 6;

        y = heading(page, "ESTIMATED POINTS", px, y, pw);
        y -= 24;
        page.text(Font.BOLD, 26, INK, px, y, Align.LEFT, String.valueOf(run.points));
        page.text(Font.REGULAR, 10, MUTED, px + Font.BOLD.width(String.valueOf(run.points), 26) + 4, y, Align.LEFT, "points");
        for (AutoRun.Score s : run.scores) {
            y -= 13;
            page.text(Font.BOLD, 8.5, INK, px, y, Align.LEFT, s.label);
            page.text(Font.BOLD, 8.5, s.earned ? GOOD : MUTED, px + pw, y, Align.RIGHT, s.earned ? "+" + s.points : "0");
            y = paragraph(page, Font.REGULAR, 7.5, MUTED, px, y - 9.5, pw, s.reason);
        }

        y = heading(page, "ESTIMATED TIME", px, y - 14, pw);
        y -= 15;
        boolean over = run.seconds > MapSettings.AUTO_SECONDS;
        page.text(Font.BOLD, 11, over ? BAD : INK, px, y, Align.LEFT,
                AutoRun.secondsText(run.seconds) + (run.timeUnknown ? " + unknown" : "") + " of 30 s");
        y -= 8;
        page.fill(RULE).rect(px, y, pw, 4).fillPath();
        page.fill(over ? BAD : FieldPainter.color(op.alliance)).rect(px, y, pw * Math.min(1, run.seconds / MapSettings.AUTO_SECONDS), 4).fillPath();

        y = heading(page, "CHECKS", px, y - 14, pw);
        if (run.problems.isEmpty()) {
            y -= 11;
            page.text(Font.BOLD, 8.5, GOOD, px, y, Align.LEFT, "No problems found.");
        }
        for (String problem : run.problems) {
            y -= 10.5;
            page.fill(BAD).circle(px + 2, y + 2.6, 1.6).fillPath();
            y = paragraph(page, Font.REGULAR, 7.5, BAD, px + 8, y, pw - 8, problem);
        }

        y = heading(page, "ROUTINE", px, y - 14, pw);
        for (AutoRun.Row row : run.rows) {
            if (y < floor + 30) {
                page.text(Font.REGULAR, 7.5, MUTED, px, y - 10, Align.LEFT, "… more steps in " + op.routeName + ".json");
                y -= 10;
                break;
            }
            y -= 11;
            double indent = row.depth * 9;
            if (row.number > 0) {
                page.fill(FieldPainter.color(op.alliance)).circle(px + indent + 4.5, y + 2.6, 4.5).fillPath();
                page.text(Font.BOLD, 6, WHITE, px + indent + 4.5, y + 0.5, Align.CENTER, String.valueOf(row.number));
            }
            page.text(Font.REGULAR, 7.5, MUTED, px + pw, y, Align.RIGHT, row.time);
            double textX = px + indent + (row.number > 0 ? 12 : 0);
            y = paragraph(page, row.number > 0 ? Font.REGULAR : Font.BOLD, 7.5, INK, textX, y, pw - (textX - px) - 26, row.text);
        }

        if (y > floor + 40) {
            y = heading(page, "WAYPOINTS, " + op.alliance + " FRAME (x, y, heading)", px, y - 14, pw);
            for (Map.Entry<String, Pose> e : run.waypoints.entrySet()) {
                if (y < floor + 10) break;
                y -= 10.5;
                Pose p = e.getValue();
                page.text(Font.BOLD, 7.5, INK, px, y, Align.LEFT, e.getKey() + (run.controlPoints.contains(e.getKey()) ? " (control)" : ""));
                page.text(Font.REGULAR, 7.5, INK, px + pw, y, Align.RIGHT, String.format(Locale.US, "%.1f, %.1f, %.0f°",
                        p.x(), p.y(), (Math.toDegrees(p.heading()) % 360 + 360) % 360));
            }
        }

        footer(page, "Drawn from TeamCode/src/main/assets/autos/" + op.routeName + ".json   ·   rules and settings: docs/auto-maps.md"
                + "   ·   regenerate: ./gradlew :TeamCode:autoMaps", pageNumber, pages);
    }

    // ---- small layout helpers -----------------------------------------------------------------

    private static double heading(Page page, String text, double x, double y, double width) {
        page.text(Font.BOLD, 7.5, MUTED, x, y, Align.LEFT, text);
        rule(page, x, y - 4, x + width);
        return y - 4;
    }

    private static void rule(Page page, double x0, double y, double x1) {
        page.stroke(RULE).lineWidth(0.6).moveTo(x0, y).lineTo(x1, y).strokePath();
    }

    private static void footer(Page page, String text, int number, int pages) {
        page.text(Font.REGULAR, 6.5, MUTED, M, M - 14, Align.LEFT, text);
        page.text(Font.REGULAR, 6.5, MUTED, W - M, M - 14, Align.RIGHT, "page " + number + " of " + pages);
    }

    /** Word-wrapped text; the first baseline is {@code y}. Returns the last baseline. */
    private static double paragraph(Page page, Font font, double size, Color color, double x, double y, double width, String text) {
        StringBuilder line = new StringBuilder();
        double leading = size * 1.3;
        for (String word : text.split(" ")) {
            String candidate = line.length() == 0 ? word : line + " " + word;
            if (font.width(candidate, size) > width && line.length() > 0) {
                page.text(font, size, color, x, y, Align.LEFT, line.toString());
                y -= leading;
                line = new StringBuilder(word);
            } else {
                line = new StringBuilder(candidate);
            }
        }
        if (line.length() > 0) {
            page.text(font, size, color, x, y, Align.LEFT, line.toString());
        }
        return y;
    }
}
