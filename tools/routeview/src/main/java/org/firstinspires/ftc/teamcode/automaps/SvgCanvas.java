package org.firstinspires.ftc.teamcode.automaps;

import org.firstinspires.ftc.teamcode.automaps.Pdf.Align;
import org.firstinspires.ftc.teamcode.automaps.Pdf.Color;
import org.firstinspires.ftc.teamcode.automaps.Pdf.Font;

import java.util.Locale;

/**
 * {@link Canvas} that writes SVG, so {@link FieldPainter} draws the same picture in the browser as
 * in docs/autos.pdf. Takes PDF-style coordinates (points, origin bottom-left) and flips y.
 * Graphics state works as in PDF: colours, width, dash and caps apply to the next paint, and a
 * paint consumes the current path.
 */
final class SvgCanvas implements Canvas {

    private static final Color BLACK = new Color(0x000000);

    private final double width;
    private final double height;
    private final StringBuilder body = new StringBuilder();
    private final StringBuilder path = new StringBuilder();

    private Color fill = BLACK;
    private Color stroke = BLACK;
    private double lineWidth = 1;
    private String dash;
    private boolean round;

    SvgCanvas(double width, double height) {
        this.width = width;
        this.height = height;
    }

    String toSvg() {
        return "<svg xmlns=\"http://www.w3.org/2000/svg\" viewBox=\"0 0 " + n(width) + " " + n(height) + "\""
                + " font-family=\"Helvetica, Arial, sans-serif\">" + body + "</svg>";
    }

    // ---- graphics state ---------------------------------------------------------------------

    @Override public SvgCanvas fill(Color c) { fill = c; return this; }
    @Override public SvgCanvas stroke(Color c) { stroke = c; return this; }
    @Override public SvgCanvas lineWidth(double w) { lineWidth = w; return this; }
    @Override public SvgCanvas dash(double on, double off) { dash = n(on) + " " + n(off); return this; }
    @Override public SvgCanvas solid() { dash = null; return this; }
    @Override public SvgCanvas roundCaps() { round = true; return this; }
    @Override public SvgCanvas buttCaps() { round = false; return this; }

    // ---- paths ------------------------------------------------------------------------------

    @Override public SvgCanvas moveTo(double x, double y) { return seg("M", x, y); }
    @Override public SvgCanvas lineTo(double x, double y) { return seg("L", x, y); }
    @Override public SvgCanvas close() { path.append("Z "); return this; }

    @Override
    public SvgCanvas rect(double x, double y, double w, double h) {
        return moveTo(x, y).lineTo(x + w, y).lineTo(x + w, y + h).lineTo(x, y + h).close();
    }

    @Override
    public SvgCanvas circle(double cx, double cy, double r) {
        moveTo(cx + r, cy);
        arc(r, cx - r, cy);
        arc(r, cx + r, cy);
        return close();
    }

    @Override public SvgCanvas fillPath() { return paint(true, false); }
    @Override public SvgCanvas strokePath() { return paint(false, true); }
    @Override public SvgCanvas fillAndStroke() { return paint(true, true); }

    // ---- text -------------------------------------------------------------------------------

    @Override
    public SvgCanvas text(Font font, double size, Color color, double x, double y, Align align, String text) {
        fill(color);
        String anchor = align == Align.LEFT ? "start" : align == Align.CENTER ? "middle" : "end";
        body.append("<text x=\"").append(n(x)).append("\" y=\"").append(n(flip(y))).append('"');
        textAttributes(font, size, color, anchor);
        body.append('>').append(escape(text)).append("</text>");
        return this;
    }

    @Override
    public SvgCanvas textUp(Font font, double size, Color color, double x, double y, String text) {
        fill(color);
        body.append("<text transform=\"translate(").append(n(x)).append(' ').append(n(flip(y))).append(") rotate(-90)\"");
        textAttributes(font, size, color, "middle");
        body.append('>').append(escape(text)).append("</text>");
        return this;
    }

    // ---- internals --------------------------------------------------------------------------

    private SvgCanvas seg(String op, double x, double y) {
        path.append(op).append(n(x)).append(' ').append(n(flip(y))).append(' ');
        return this;
    }

    /** Half-circle arc of radius r to (x, y); two make a circle. */
    private void arc(double r, double x, double y) {
        path.append('A').append(n(r)).append(' ').append(n(r)).append(" 0 1 0 ")
                .append(n(x)).append(' ').append(n(flip(y))).append(' ');
    }

    private SvgCanvas paint(boolean doFill, boolean doStroke) {
        if (path.length() == 0) {
            return this;
        }
        body.append("<path d=\"").append(path.toString().trim()).append('"');
        body.append(" fill=\"").append(doFill ? hex(fill) : "none").append('"');
        if (doStroke) {
            body.append(" stroke=\"").append(hex(stroke)).append("\" stroke-width=\"").append(n(lineWidth)).append('"');
            if (dash != null) {
                body.append(" stroke-dasharray=\"").append(dash).append('"');
            }
            if (round) {
                body.append(" stroke-linecap=\"round\" stroke-linejoin=\"round\"");
            }
        }
        body.append("/>");
        path.setLength(0);
        return this;
    }

    private void textAttributes(Font font, double size, Color color, String anchor) {
        body.append(" font-size=\"").append(n(size)).append('"');
        if (font == Font.BOLD) {
            body.append(" font-weight=\"bold\"");
        }
        body.append(" fill=\"").append(hex(color)).append('"');
        if (!anchor.equals("start")) {
            body.append(" text-anchor=\"").append(anchor).append('"');
        }
    }

    private double flip(double y) {
        return height - y;
    }

    static String hex(Color c) {
        return String.format(Locale.US, "#%02x%02x%02x",
                Math.round(c.r * 255), Math.round(c.g * 255), Math.round(c.b * 255));
    }

    /** Two decimals at most, no trailing zeros: 12.5, 3, -0.25. */
    static String n(double v) {
        String s = String.format(Locale.US, "%.2f", v);
        if (s.indexOf('.') >= 0) {
            s = s.replaceAll("0+$", "").replaceAll("\\.$", "");
        }
        return s.equals("-0") ? "0" : s;
    }

    static String escape(String s) {
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;");
    }
}
