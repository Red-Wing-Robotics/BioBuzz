package org.firstinspires.ftc.teamcode.automaps;

import org.firstinspires.ftc.teamcode.automaps.Pdf.Align;
import org.firstinspires.ftc.teamcode.automaps.Pdf.Color;
import org.firstinspires.ftc.teamcode.automaps.Pdf.Font;

/**
 * What {@link FieldPainter} draws on: a PDF page for docs/autos.pdf ({@link Pdf.Page}) or an SVG
 * for the live route viewer (tools/routeview). Coordinates are points from the bottom-left, as in
 * PDF. Paths are built with moveTo/lineTo/rect/circle and painted with fillPath/strokePath/
 * fillAndStroke; {@code text} also sets the fill colour, as it does in PDF.
 */
interface Canvas {
    Canvas fill(Color c);
    Canvas stroke(Color c);
    Canvas lineWidth(double w);
    Canvas dash(double on, double off);
    Canvas solid();
    Canvas roundCaps();
    Canvas buttCaps();

    Canvas moveTo(double x, double y);
    Canvas lineTo(double x, double y);
    Canvas close();
    Canvas rect(double x, double y, double w, double h);
    Canvas circle(double cx, double cy, double r);

    Canvas fillPath();
    Canvas strokePath();
    Canvas fillAndStroke();

    /** Text with its baseline at {@code y}, aligned to {@code x}. */
    Canvas text(Font font, double size, Color color, double x, double y, Align align, String text);

    /** Text rotated 90° counter-clockwise (reads bottom to top), centred on (x, y). */
    Canvas textUp(Font font, double size, Color color, double x, double y, String text);
}
