package org.firstinspires.ftc.teamcode.automaps;

import static org.firstinspires.ftc.teamcode.automaps.BiobuzzField.CENTER;
import static org.firstinspires.ftc.teamcode.automaps.BiobuzzField.SIZE;
import static org.firstinspires.ftc.teamcode.automaps.BiobuzzField.TILE;

import com.pedropathing.math.Pose;

import org.firstinspires.ftc.teamcode.auto.Alliance;
import org.firstinspires.ftc.teamcode.automaps.Geometry.Poly;
import org.firstinspires.ftc.teamcode.automaps.Pdf.Align;
import org.firstinspires.ftc.teamcode.automaps.Pdf.Color;
import org.firstinspires.ftc.teamcode.automaps.Pdf.Font;
import org.firstinspires.ftc.teamcode.automaps.Pdf.Page;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Draws the BIOBUZZ field and one auto's run onto a PDF page, to scale. Field inches map to page
 * points with the origin at the field's bottom-left (the corner by the red GARDEN), audience at
 * the bottom, red alliance on the left, as in the Pedro Pathing visualizer.
 */
final class FieldPainter {

    static final Color INK = new Color(0x17212B);
    static final Color MUTED = new Color(0x5B6672);
    static final Color RULE = new Color(0xC9CFD5);
    static final Color WHITE = new Color(0xFFFFFF);
    static final Color GOOD = new Color(0x1E7A3C);
    static final Color BAD = new Color(0xB3261E);
    private static final Color TILE_A = new Color(0xE7EAED);
    private static final Color TILE_B = new Color(0xDDE1E5);
    private static final Color SEAM = new Color(0xC3C9CF);
    private static final Color WALL = new Color(0x2A3540);
    private static final Color RED = new Color(0xC41F33);
    private static final Color RED_TINT = new Color(0xF3D2D6);
    private static final Color RED_PALE = new Color(0xF9E9EB);
    private static final Color BLUE = new Color(0x1C5CC0);
    private static final Color BLUE_TINT = new Color(0xD2DEF3);
    private static final Color BLUE_PALE = new Color(0xE9EFF9);
    private static final Color AMBER = new Color(0x8A6410);
    private static final Color AMBER_TINT = new Color(0xF6DCA0);
    private static final Color HIVE_FLOOR = new Color(0xF1F3F5);
    private static final Color FRAME = new Color(0x7C858E);
    private static final Color BEAM = new Color(0x4F5964);

    static Color color(Alliance a) { return a == Alliance.RED ? RED : BLUE; }
    static Color tint(Alliance a) { return a == Alliance.RED ? RED_TINT : BLUE_TINT; }

    private final Page page;
    private final double ox;
    private final double oy;
    private final double k;

    /** Field of {@code side} points with its bottom-left corner at (ox, oy). */
    FieldPainter(Page page, double ox, double oy, double side) {
        this.page = page;
        this.ox = ox;
        this.oy = oy;
        this.k = side / SIZE;
    }

    double px(double x) { return ox + x * k; }
    double py(double y) { return oy + y * k; }

    // ---- field ---------------------------------------------------------------------------

    void drawField() {
        for (int i = 0; i < 6; i++) {
            for (int j = 0; j < 6; j++) {
                page.fill((i + j) % 2 == 0 ? TILE_A : TILE_B).rect(px(i * TILE), py(j * TILE), TILE * k, TILE * k).fillPath();
            }
        }
        page.stroke(SEAM).lineWidth(0.4);
        for (int i = 1; i < 6; i++) {
            page.moveTo(px(i * TILE), py(0)).lineTo(px(i * TILE), py(SIZE)).strokePath();
            page.moveTo(px(0), py(i * TILE)).lineTo(px(SIZE), py(i * TILE)).strokePath();
        }
        page.stroke(MUTED).lineWidth(0.7).dash(4, 3);
        page.moveTo(px(CENTER), py(0)).lineTo(px(CENTER), py(SIZE)).strokePath();
        page.solid();

        zone(BiobuzzField.RED_LOADING_ZONE, RED_TINT, RED, "LOADING ZONE");
        zone(BiobuzzField.BLUE_LOADING_ZONE, BLUE_TINT, BLUE, "LOADING ZONE");
        page.fill(RED);
        poly(BiobuzzField.RED_GARDEN).fillPath();
        page.fill(BLUE);
        poly(BiobuzzField.BLUE_GARDEN).fillPath();
        page.text(Font.BOLD, 5.5, RED, px(BiobuzzField.RED_GARDEN.maxX()) + 3, py(0.6), Align.LEFT, "RED GARDEN");
        page.text(Font.BOLD, 5.5, BLUE, px(BiobuzzField.BLUE_GARDEN.minX()) - 3, py(SIZE - 2.4), Align.RIGHT, "BLUE GARDEN");
        for (Poly flower : BiobuzzField.FLOWERS) {
            page.fill(AMBER_TINT).stroke(AMBER).lineWidth(0.8);
            poly(flower).fillAndStroke();
        }
        drawHive();

        page.stroke(WALL).lineWidth(1.8).rect(px(0), py(0), SIZE * k, SIZE * k).strokePath();
        drawFrameLabels();
    }

    private void zone(Poly zone, Color fill, Color line, String label) {
        page.fill(fill).stroke(line).lineWidth(1.1);
        poly(zone).fillAndStroke();
        double cx = px((zone.minX() + zone.maxX()) / 2) + 2;
        double cy = py((zone.minY() + zone.maxY()) / 2);
        page.textUp(Font.BOLD, 5.5, line, cx, cy, label);
    }

    private void drawHive() {
        page.fill(HIVE_FLOOR).stroke(FRAME).lineWidth(0.6).dash(2, 1.5);
        poly(BiobuzzField.HIVE_STRUCTURE).fillAndStroke();
        page.solid();
        cell(BiobuzzField.RED_CELL_DOWN_AT_START, RED_PALE, RED);
        cell(BiobuzzField.RED_CELL_UP_AT_START, RED_TINT, RED);
        cell(BiobuzzField.BLUE_CELL_DOWN_AT_START, BLUE_PALE, BLUE);
        cell(BiobuzzField.BLUE_CELL_UP_AT_START, BLUE_TINT, BLUE);
        page.fill(FRAME);
        poly(BiobuzzField.FRAME_RAIL_RED).fillPath();
        poly(BiobuzzField.FRAME_RAIL_BLUE).fillPath();
        page.fill(AMBER_TINT).stroke(AMBER).lineWidth(0.5);
        poly(BiobuzzField.DAMPER_HIGH).fillAndStroke();
        poly(BiobuzzField.DAMPER_LOW).fillAndStroke();
        page.fill(BEAM);
        poly(BiobuzzField.PIVOT_BEAM).fillPath();
        page.text(Font.BOLD, 5, WHITE, px(CENTER), py(70.1), Align.CENTER, "HIVE");
        for (double[] n : BiobuzzField.RED_START_NECTAR) {
            page.fill(RED).circle(px(n[0]), py(n[1]), 1.3 * k).fillPath();
            page.fill(BLUE).circle(px(SIZE - n[0]), py(SIZE - n[1]), 1.3 * k).fillPath();
        }
        page.text(Font.REGULAR, 5, RED, px(58), py(64.6), Align.CENTER, "CELL up at start");
        page.text(Font.REGULAR, 5, BLUE, px(SIZE - 58), py(SIZE - 64.6) - 3.5, Align.CENTER, "CELL up at start");
    }

    private void cell(Poly cell, Color fill, Color line) {
        page.fill(fill).stroke(line).lineWidth(0.9);
        poly(cell).fillAndStroke();
    }

    private void drawFrameLabels() {
        page.stroke(MUTED).lineWidth(0.5);
        double[] ticks = {0, 24, 48, 72, 96, 120, SIZE};
        for (double t : ticks) {
            page.moveTo(px(t), py(0)).lineTo(px(t), py(0) - 3).strokePath();
            page.moveTo(px(0), py(t)).lineTo(px(0) - 3, py(t)).strokePath();
            String label = t == SIZE ? "141.5" : String.valueOf((int) t);
            page.text(Font.REGULAR, 6, MUTED, px(t), py(0) - 10, Align.CENTER, t == SIZE ? label + " in" : label);
            page.text(Font.REGULAR, 6, MUTED, px(0) - 5, py(t) - 2, Align.RIGHT, label);
        }
        String columns = "ABCDEF";
        for (int i = 0; i < 6; i++) {
            page.text(Font.BOLD, 8, i < 3 ? RED : BLUE, px((i + 0.5) * TILE), py(SIZE) + 5, Align.CENTER,
                    String.valueOf(columns.charAt(i)));
        }
        page.text(Font.BOLD, 7, MUTED, px(CENTER), py(0) - 20, Align.CENTER, "AUDIENCE");
        // Far enough out to clear a robot outline drawn past the wall.
        page.textUp(Font.BOLD, 7, RED, px(0) - 24, py(CENTER), "RED ALLIANCE AREA");
        page.textUp(Font.BOLD, 7, BLUE, px(SIZE) + 24, py(CENTER), "BLUE ALLIANCE AREA");
    }

    // ---- one auto ------------------------------------------------------------------------

    void drawRun(AutoRun run) {
        Color c = color(run.alliance);
        robot(run.start, null, c, true);
        for (int i = 0; i < run.legs.size() - 1; i++) {
            List<Pose> s = run.legs.get(i).samples;
            robot(s.get(s.size() - 1), null, c, false);
        }
        robot(run.end, tint(run.alliance), c, false);

        page.stroke(c).lineWidth(1.7).roundCaps();
        for (AutoRun.Leg leg : run.legs) {
            for (int i = 0; i < leg.samples.size(); i++) {
                Pose p = leg.samples.get(i);
                if (i == 0) page.moveTo(px(p.x()), py(p.y())); else page.lineTo(px(p.x()), py(p.y()));
            }
            page.strokePath();
            chevron(leg, c);
        }
        page.buttCaps();

        List<double[]> taken = new ArrayList<>();
        for (Map.Entry<String, Pose> e : run.waypoints.entrySet()) {
            Pose p = e.getValue();
            boolean control = run.controlPoints.contains(e.getKey());
            page.stroke(c).lineWidth(0.9);
            if (control) {
                page.fill(WHITE).circle(px(p.x()), py(p.y()), 2.2).fillAndStroke();
            } else {
                page.fill(c).circle(px(p.x()), py(p.y()), 2.2).fillPath();
            }
            // Above the point, clear of the step badges that sit on the path.
            boolean right = p.x() < SIZE - 26;
            page.text(Font.BOLD, 6.5, INK, px(p.x()) + (right ? 3 : -3), py(p.y()) + 8, right ? Align.LEFT : Align.RIGHT,
                    e.getKey() + (control ? " (control)" : ""));
        }
        for (AutoRun.Badge b : run.badges) {
            double x = px(b.x), y = py(b.y);
            for (int tries = 0; tries < 6 && near(taken, x, y); tries++) {
                y -= 12;
            }
            taken.add(new double[] {x, y});
            page.fill(c).stroke(WHITE).lineWidth(0.8).circle(x, y, 5.6).fillAndStroke();
            page.text(Font.BOLD, 6.5, WHITE, x, y - 2.3, Align.CENTER, String.valueOf(b.number));
        }
    }

    /** The robot's outline at a pose; the thick edge is its front. */
    private void robot(Pose pose, Color fill, Color line, boolean dashed) {
        Poly f = AutoRun.foot(pose);
        page.stroke(line).lineWidth(dashed ? 0.9 : fill != null ? 1.1 : 0.5);
        if (dashed) page.dash(3, 2);
        if (fill != null) {
            page.fill(fill);
            poly(f).fillAndStroke();
        } else {
            poly(f).strokePath();
        }
        page.solid().lineWidth(fill != null || dashed ? 2.6 : 1.4);
        page.moveTo(px(f.x[0]), py(f.y[0])).lineTo(px(f.x[3]), py(f.y[3])).strokePath();
    }

    private void chevron(AutoRun.Leg leg, Color c) {
        int i = (int) (leg.samples.size() * 0.62);
        if (leg.length < 4 || i < 1 || i >= leg.samples.size()) return;
        Pose a = leg.samples.get(i - 1), b = leg.samples.get(i);
        double dx = px(b.x()) - px(a.x()), dy = py(b.y()) - py(a.y());
        double len = Math.hypot(dx, dy);
        if (len == 0) return;
        dx /= len;
        dy /= len;
        double x = px(b.x()), y = py(b.y());
        page.stroke(c).lineWidth(1.6).roundCaps();
        page.moveTo(x - dx * 4 - dy * 4, y - dy * 4 + dx * 4).lineTo(x + dx * 2, y + dy * 2)
                .lineTo(x - dx * 4 + dy * 4, y - dy * 4 - dx * 4).strokePath();
        page.buttCaps();
    }

    private static boolean near(List<double[]> taken, double x, double y) {
        for (double[] t : taken) {
            if (Math.hypot(t[0] - x, t[1] - y) < 11.5) return true;
        }
        return false;
    }

    private Page poly(Poly p) {
        page.moveTo(px(p.x[0]), py(p.y[0]));
        for (int i = 1; i < p.x.length; i++) {
            page.lineTo(px(p.x[i]), py(p.y[i]));
        }
        return page.close();
    }
}
