package org.firstinspires.ftc.teamcode.auto.route;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;

import org.junit.Test;

import java.nio.charset.StandardCharsets;

public class RouteChecksumTest {

    @Test
    public void isTheStandardCrc32InEightHexDigits() {
        // CRC-32 check value from the standard ("123456789" → CBF43926).
        assertEquals("cbf43926", RouteChecksum.of("123456789".getBytes(StandardCharsets.US_ASCII)));
        assertEquals("00000000", RouteChecksum.of(new byte[0]));
    }

    @Test
    public void changesWhenOneByteChanges() {
        assertNotEquals(RouteChecksum.of("{\"x\": 1}".getBytes(StandardCharsets.UTF_8)),
                RouteChecksum.of("{\"x\": 2}".getBytes(StandardCharsets.UTF_8)));
    }
}
