package org.firstinspires.ftc.teamcode.automaps;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.firstinspires.ftc.teamcode.automaps.Pdf.Align;
import org.firstinspires.ftc.teamcode.automaps.Pdf.Color;
import org.firstinspires.ftc.teamcode.automaps.Pdf.Font;
import org.junit.Test;

public class SvgCanvasTest {

    private static final Color RED = new Color(0xC41F33);

    @Test
    public void flipsYSoTheOriginIsBottomLeftAsInPdf() {
        SvgCanvas c = new SvgCanvas(100, 50);
        c.fill(RED).moveTo(0, 0).lineTo(10, 50).fillPath();
        assertTrue(c.toSvg(), c.toSvg().contains("<path d=\"M0 50 L10 0\" fill=\"#c41f33\"/>"));
    }

    @Test
    public void aPaintConsumesThePathAndStrokeCarriesTheGraphicsState() {
        SvgCanvas c = new SvgCanvas(100, 100);
        c.stroke(RED).lineWidth(1.5).dash(3, 2).roundCaps().moveTo(1, 1).lineTo(2, 2).strokePath();
        c.strokePath(); // nothing left to paint
        String svg = c.toSvg();
        assertTrue(svg, svg.contains("fill=\"none\" stroke=\"#c41f33\" stroke-width=\"1.5\" stroke-dasharray=\"3 2\""
                + " stroke-linecap=\"round\" stroke-linejoin=\"round\"/>"));
        assertEquals(1, svg.split("<path").length - 1);
    }

    @Test
    public void textIsEscapedAnchoredAndSetsTheFillLikePdf() {
        SvgCanvas c = new SvgCanvas(100, 100);
        c.text(Font.BOLD, 8, RED, 50, 10, Align.CENTER, "A → <B> & \"C\"");
        c.rect(0, 0, 1, 1).fillPath(); // filled with the text's colour, as in PDF
        String svg = c.toSvg();
        assertTrue(svg, svg.contains("<text x=\"50\" y=\"90\" font-size=\"8\" font-weight=\"bold\" fill=\"#c41f33\""
                + " text-anchor=\"middle\">A → &lt;B&gt; &amp; &quot;C&quot;</text>"));
        assertTrue(svg, svg.contains("fill=\"#c41f33\"/>"));
    }

    @Test
    public void numbersAreShort() {
        assertEquals("12.5", SvgCanvas.n(12.5));
        assertEquals("3", SvgCanvas.n(3.0));
        assertEquals("0.33", SvgCanvas.n(1 / 3.0));
        assertEquals("0", SvgCanvas.n(-0.001));
        assertFalse(SvgCanvas.n(1e7).contains("E"));
    }
}
