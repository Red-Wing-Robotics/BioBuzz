package org.firstinspires.ftc.teamcode.automaps;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * A minimal PDF 1.4 writer: vector shapes and text in the standard Helvetica fonts, just enough
 * for docs/autos.pdf without a third-party library. Coordinates are points (1/72 in) from the
 * bottom-left of the page. The output has no dates in it, so unchanged routes produce a
 * byte-identical file.
 */
final class Pdf {

    /** The standard Type 1 fonts the PDF uses; no font files are embedded. */
    enum Font {
        REGULAR("F1", "Helvetica", HELVETICA_WIDTHS),
        BOLD("F2", "Helvetica-Bold", HELVETICA_BOLD_WIDTHS);

        final String resource;
        final String baseFont;
        private final int[] widths;

        Font(String resource, String baseFont, int[] widths) {
            this.resource = resource;
            this.baseFont = baseFont;
            this.widths = widths;
        }

        /** Width of {@code text} in points at {@code size}. */
        double width(String text, double size) {
            String encoded = winAnsi(text);
            double units = 0;
            for (int i = 0; i < encoded.length(); i++) {
                units += glyphWidth(encoded.charAt(i));
            }
            return units * size / 1000.0;
        }

        private int glyphWidth(char c) {
            if (c >= 32 && c <= 126) {
                return widths[c - 32];
            }
            boolean bold = this == BOLD;
            switch (c) {
                case 0x85: case 0x97: return 1000;      // ellipsis, em dash
                case 0x91: case 0x92: return bold ? 278 : 222;
                case 0x93: case 0x94: return bold ? 500 : 333;
                case 0x95: return 350;                    // bullet
                case 0x96: return 556;                    // en dash
                case 0xB0: return 400;                    // degree
                case 0xB7: return 278;                    // middle dot
                case 0xD7: return 584;                    // multiplication sign
                default: return 556;
            }
        }
    }

    /** An RGB colour, components 0..1. */
    static final class Color {
        final double r, g, b;

        Color(int rgb) {
            this.r = ((rgb >> 16) & 0xFF) / 255.0;
            this.g = ((rgb >> 8) & 0xFF) / 255.0;
            this.b = (rgb & 0xFF) / 255.0;
        }
    }

    enum Align { LEFT, CENTER, RIGHT }

    /** One page's drawing operations, written as a PDF content stream. */
    static final class Page {
        final double width;
        final double height;
        private final StringBuilder ops = new StringBuilder();

        Page(double width, double height) {
            this.width = width;
            this.height = height;
        }

        Page fill(Color c) { return op(n(c.r) + " " + n(c.g) + " " + n(c.b) + " rg"); }
        Page stroke(Color c) { return op(n(c.r) + " " + n(c.g) + " " + n(c.b) + " RG"); }
        Page lineWidth(double w) { return op(n(w) + " w"); }
        Page dash(double on, double off) { return op("[" + n(on) + " " + n(off) + "] 0 d"); }
        Page solid() { return op("[] 0 d"); }
        Page roundCaps() { return op("1 J 1 j"); }
        Page buttCaps() { return op("0 J 0 j"); }
        Page save() { return op("q"); }
        Page restore() { return op("Q"); }

        Page moveTo(double x, double y) { return op(n(x) + " " + n(y) + " m"); }
        Page lineTo(double x, double y) { return op(n(x) + " " + n(y) + " l"); }
        Page close() { return op("h"); }
        Page rect(double x, double y, double w, double h) { return op(n(x) + " " + n(y) + " " + n(w) + " " + n(h) + " re"); }

        /** A circle, as four Bezier arcs. */
        Page circle(double cx, double cy, double r) {
            double k = 0.5523 * r;
            moveTo(cx + r, cy);
            op(n(cx + r) + " " + n(cy + k) + " " + n(cx + k) + " " + n(cy + r) + " " + n(cx) + " " + n(cy + r) + " c");
            op(n(cx - k) + " " + n(cy + r) + " " + n(cx - r) + " " + n(cy + k) + " " + n(cx - r) + " " + n(cy) + " c");
            op(n(cx - r) + " " + n(cy - k) + " " + n(cx - k) + " " + n(cy - r) + " " + n(cx) + " " + n(cy - r) + " c");
            op(n(cx + k) + " " + n(cy - r) + " " + n(cx + r) + " " + n(cy - k) + " " + n(cx + r) + " " + n(cy) + " c");
            return close();
        }

        Page fillPath() { return op("f"); }
        Page strokePath() { return op("S"); }
        Page fillAndStroke() { return op("B"); }

        /** Text with its baseline at {@code y}, aligned to {@code x}. */
        Page text(Font font, double size, Color color, double x, double y, Align align, String text) {
            double w = font.width(text, size);
            double left = align == Align.LEFT ? x : align == Align.CENTER ? x - w / 2 : x - w;
            fill(color);
            return op("BT /" + font.resource + " " + n(size) + " Tf " + n(left) + " " + n(y) + " Td ("
                    + escape(winAnsi(text)) + ") Tj ET");
        }

        /** Text rotated 90° counter-clockwise (reads bottom to top), centred on (x, y). */
        Page textUp(Font font, double size, Color color, double x, double y, String text) {
            double w = font.width(text, size);
            fill(color);
            return op("BT /" + font.resource + " " + n(size) + " Tf 0 1 -1 0 " + n(x) + " " + n(y - w / 2)
                    + " Tm (" + escape(winAnsi(text)) + ") Tj ET");
        }

        private Page op(String op) {
            ops.append(op).append('\n');
            return this;
        }
    }

    private final List<Page> pages = new ArrayList<>();
    private final String title;

    Pdf(String title) {
        this.title = title;
    }

    Page newPage(double width, double height) {
        Page page = new Page(width, height);
        pages.add(page);
        return page;
    }

    int pageCount() {
        return pages.size();
    }

    byte[] toBytes() {
        Font[] fonts = Font.values();
        int firstFont = 4;
        int firstPage = firstFont + fonts.length;
        int objectCount = firstPage + 2 * pages.size() - 1;
        long[] offsets = new long[objectCount + 1];
        ByteArrayOutputStream out = new ByteArrayOutputStream();

        write(out, "%PDF-1.4\n%âãÏÓ\n");
        offsets[1] = out.size();
        write(out, "1 0 obj\n<< /Type /Catalog /Pages 2 0 R >>\nendobj\n");

        StringBuilder kids = new StringBuilder();
        for (int i = 0; i < pages.size(); i++) {
            kids.append(firstPage + 2 * i).append(" 0 R ");
        }
        offsets[2] = out.size();
        write(out, "2 0 obj\n<< /Type /Pages /Kids [" + kids + "] /Count " + pages.size() + " >>\nendobj\n");

        offsets[3] = out.size();
        write(out, "3 0 obj\n<< /Title (" + escape(winAnsi(title)) + ") /Producer (BioBuzz TeamCode AutoMapsPdf) >>\nendobj\n");

        StringBuilder fontResources = new StringBuilder();
        for (int i = 0; i < fonts.length; i++) {
            int num = firstFont + i;
            offsets[num] = out.size();
            write(out, num + " 0 obj\n<< /Type /Font /Subtype /Type1 /BaseFont /" + fonts[i].baseFont
                    + " /Encoding /WinAnsiEncoding >>\nendobj\n");
            fontResources.append('/').append(fonts[i].resource).append(' ').append(num).append(" 0 R ");
        }

        for (int i = 0; i < pages.size(); i++) {
            Page page = pages.get(i);
            int pageNum = firstPage + 2 * i;
            int contentNum = pageNum + 1;
            offsets[pageNum] = out.size();
            write(out, pageNum + " 0 obj\n<< /Type /Page /Parent 2 0 R /MediaBox [0 0 " + n(page.width) + " "
                    + n(page.height) + "] /Resources << /Font << " + fontResources + ">> >> /Contents "
                    + contentNum + " 0 R >>\nendobj\n");
            byte[] content = page.ops.toString().getBytes(StandardCharsets.ISO_8859_1);
            offsets[contentNum] = out.size();
            write(out, contentNum + " 0 obj\n<< /Length " + content.length + " >>\nstream\n");
            out.write(content, 0, content.length);
            write(out, "endstream\nendobj\n");
        }

        long xref = out.size();
        StringBuilder table = new StringBuilder("xref\n0 " + (objectCount + 1) + "\n0000000000 65535 f \n");
        for (int i = 1; i <= objectCount; i++) {
            table.append(String.format(Locale.US, "%010d 00000 n \n", offsets[i]));
        }
        table.append("trailer\n<< /Size ").append(objectCount + 1).append(" /Root 1 0 R /Info 3 0 R >>\nstartxref\n")
                .append(xref).append("\n%%EOF\n");
        write(out, table.toString());
        return out.toByteArray();
    }

    private static void write(ByteArrayOutputStream out, String s) {
        byte[] bytes = s.getBytes(StandardCharsets.ISO_8859_1);
        out.write(bytes, 0, bytes.length);
    }

    /** Number for a content stream: at most 2 decimals, no trailing zeros, never "-0". */
    static String n(double v) {
        String s = String.format(Locale.US, "%.2f", v);
        if (s.contains(".")) {
            s = s.replaceAll("0+$", "").replaceAll("\\.$", "");
        }
        return s.equals("-0") ? "0" : s;
    }

    /** Maps text to the WinAnsi bytes the standard fonts use (as chars 0..255). */
    static String winAnsi(String s) {
        StringBuilder out = new StringBuilder(s.length());
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            switch (c) {
                case '–': out.append('\u0096'); break;   // en dash
                case '—': out.append('\u0097'); break;   // em dash
                case '•': out.append('\u0095'); break;   // bullet
                case '‘': out.append('\u0091'); break;
                case '’': out.append('\u0092'); break;
                case '“': out.append('\u0093'); break;
                case '”': out.append('\u0094'); break;
                case '…': out.append('\u0085'); break;
                case '→': out.append("->"); break;
                case '≤': out.append("<="); break;
                case '≥': out.append(">="); break;
                case '≈': out.append('~'); break;
                case '−': out.append('-'); break;
                default:
                    out.append((c >= 32 && c <= 126) || (c >= 0xA0 && c <= 0xFF) ? c : '?');
            }
        }
        return out.toString();
    }

    private static String escape(String s) {
        return s.replace("\\", "\\\\").replace("(", "\\(").replace(")", "\\)");
    }

    // Adobe font metrics for characters 32..126, in 1/1000 em.
    private static final int[] HELVETICA_WIDTHS = {
            278, 278, 355, 556, 556, 889, 667, 191, 333, 333, 389, 584, 278, 333, 278, 278,
            556, 556, 556, 556, 556, 556, 556, 556, 556, 556, 278, 278, 584, 584, 584, 556,
            1015, 667, 667, 722, 722, 667, 611, 778, 722, 278, 500, 667, 556, 833, 722, 778,
            667, 778, 722, 667, 611, 722, 667, 944, 667, 667, 611, 278, 278, 278, 469, 556,
            333, 556, 556, 500, 556, 556, 278, 556, 556, 222, 222, 500, 222, 833, 556, 556,
            556, 556, 333, 500, 278, 556, 500, 722, 500, 500, 500, 334, 260, 334, 584};
    private static final int[] HELVETICA_BOLD_WIDTHS = {
            278, 333, 474, 556, 556, 889, 722, 238, 333, 333, 389, 584, 278, 333, 278, 278,
            556, 556, 556, 556, 556, 556, 556, 556, 556, 556, 333, 333, 584, 584, 584, 611,
            975, 722, 722, 722, 722, 667, 611, 778, 722, 278, 556, 722, 611, 833, 722, 778,
            667, 778, 722, 667, 611, 722, 667, 944, 667, 667, 611, 333, 278, 333, 584, 556,
            333, 556, 611, 556, 611, 556, 333, 611, 611, 278, 278, 556, 278, 889, 611, 611,
            611, 611, 389, 556, 333, 611, 556, 778, 556, 556, 500, 389, 280, 389, 584};
}
