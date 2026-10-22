package org.firstinspires.ftc.teamcode.auto;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import com.google.gson.JsonObject;

import org.junit.Test;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

public class TelemetryTapTest {

    /** A message as the SDK's TelemetryImpl builds it: each line under NUL + a character counting up from 384. */
    private static Map<String, String> message(String... lines) {
        Map<String, String> data = new HashMap<>(); // no order: the keys carry it
        for (int i = 0; i < lines.length; i++) {
            data.put(String.format("\0%c", 384 + i), lines[i]);
        }
        return data;
    }

    @Test
    public void linesComeOutInTheDriverStationsOrder() {
        Map<String, String> data = message("step : to a POLLEN", "time : 7.5 s", "vision frame : 3 seen, 3 used");
        assertEquals(Arrays.asList("step : to a POLLEN", "time : 7.5 s", "vision frame : 3 seen, 3 used"), TelemetryTap.lines(data));
    }

    @Test
    public void aKeyOfItsOwnShowsAsCaptionAndValue() {
        Map<String, String> data = message("first");
        data.put("battery", "12.9 V");
        assertEquals(Arrays.asList("first", "battery : 12.9 V"), TelemetryTap.lines(data));
    }

    @Test
    public void eachInitStartsAFreshSetAndTheLastRunStaysReadable() {
        TelemetryTap tap = new TelemetryTap();
        assertNull("nothing before the first OpMode", tap.toJson());

        tap.begin("Red: Vision Seek (test)");
        tap.record(message("vision drive : NO_TARGET: no ball confirmed in 1.0 s"));
        JsonObject json = tap.toJson();
        assertEquals("Red: Vision Seek (test)", json.get("opMode").getAsString());
        assertEquals("vision drive : NO_TARGET: no ball confirmed in 1.0 s", json.getAsJsonArray("lines").get(0).getAsString());

        tap.begin("Test: Ball Vision Bench");
        assertEquals(0, tap.toJson().getAsJsonArray("lines").size());
    }
}
