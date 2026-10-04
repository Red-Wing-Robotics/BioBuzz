package org.firstinspires.ftc.teamcode.automaps;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.nio.charset.StandardCharsets;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Guards the hand-written PDF structure: a broken cross-reference table makes viewers refuse the file. */
public class PdfTest {

    @Test
    public void crossReferenceTablePointsAtEveryObject() {
        Pdf pdf = new Pdf("test");
        for (int i = 0; i < 2; i++) {
            pdf.newPage(200, 100).text(Pdf.Font.BOLD, 12, FieldPainter.INK, 10, 50, Pdf.Align.LEFT, "Page " + i + " (40°)");
        }
        String file = new String(pdf.toBytes(), StandardCharsets.ISO_8859_1);
        assertTrue(file.startsWith("%PDF-1.4\n"));
        assertTrue(file.endsWith("%%EOF\n"));

        Matcher start = Pattern.compile("startxref\n(\\d+)\n").matcher(file);
        assertTrue(start.find());
        int xref = Integer.parseInt(start.group(1));
        assertTrue(file.startsWith("xref\n", xref));

        Matcher entry = Pattern.compile("(\\d{10}) 00000 n \n").matcher(file.substring(xref));
        int object = 0;
        while (entry.find()) {
            object++;
            int offset = Integer.parseInt(entry.group(1));
            assertTrue("object " + object + " offset", file.startsWith(object + " 0 obj\n", offset));
        }
        assertEquals(3 + Pdf.Font.values().length + 2 * 2, object);
    }

    @Test
    public void textIsMappedToTheStandardFontEncoding() {
        assertEquals("a->b", Pdf.winAnsi("a→b"));
        assertEquals("40°", Pdf.winAnsi("40°"));
        assertEquals("\u0096", Pdf.winAnsi("–"));
        assertEquals("?", Pdf.winAnsi("☃"));
    }

    @Test
    public void numbersAreShortAndStable() {
        assertEquals("1.5", Pdf.n(1.5));
        assertEquals("2", Pdf.n(2.0));
        assertEquals("0", Pdf.n(-0.001));
    }
}
