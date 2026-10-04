package org.firstinspires.ftc.teamcode.routeview;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.util.Arrays;
import java.util.Map;

public class HubAndWebTest {

    @Test
    public void hubListingKeepsOnlyRouteFiles() {
        assertEquals(Arrays.asList("leave_and_park", "far side"),
                Hub.routeNames("leave_and_park.json\r\nnotes.txt\n.json\nfar side.json\n"));
    }

    @Test
    public void readsAdbsStateForTheHub() {
        byte[] none = new byte[0];
        assertEquals("device", Hub.state(new Adb.Result(0, "device\n".getBytes(), "")));
        assertEquals("offline", Hub.state(new Adb.Result(1, none, "error: device offline\n")));
        assertEquals("unauthorized", Hub.state(new Adb.Result(1, none,
                "error: device unauthorized.\nThis adb server's $ADB_VENDOR_KEYS is not set\n")));
        assertEquals(null, Hub.state(new Adb.Result(1, none, "error: device '192.168.43.1:5555' not found\n")));
    }

    @Test
    public void onlyThisMachineMayAsk() {
        assertTrue(WebServer.allowed("GET", "127.0.0.1:8765", null, 8765));
        assertTrue(WebServer.allowed("GET", "localhost:8765", null, 8765));
        assertFalse("DNS rebinding", WebServer.allowed("GET", "evil.example:8765", null, 8765));
        assertFalse(WebServer.allowed("GET", null, null, 8765));
    }

    @Test
    public void onlyThePageItselfMayChangeThings() {
        assertTrue(WebServer.allowed("POST", "127.0.0.1:8765", "http://127.0.0.1:8765", 8765));
        assertTrue("curl sends no Origin", WebServer.allowed("POST", "127.0.0.1:8765", null, 8765));
        assertFalse(WebServer.allowed("POST", "127.0.0.1:8765", "https://evil.example", 8765));
    }

    @Test
    public void parsesQueries() {
        Map<String, String> q = WebServer.query("name=far%20side&on=true&flag");
        assertEquals("far side", q.get("name"));
        assertEquals("true", q.get("on"));
        assertEquals("", q.get("flag"));
    }
}
