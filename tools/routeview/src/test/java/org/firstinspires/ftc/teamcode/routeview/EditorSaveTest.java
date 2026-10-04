package org.firstinspires.ftc.teamcode.routeview;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.firstinspires.ftc.teamcode.auto.route.RouteChecksum;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

/** The page editor's save rules, on a throwaway repo (RouteView is never started, so no adb). */
public class EditorSaveTest {

    @Rule public TemporaryFolder tmp = new TemporaryFolder();

    private static final String GOOD = "{\n  \"waypoints\": { \"START\": [9, 72, 0], \"OUT\": [24, 72, 0] },\n"
            + "  \"start\": \"START\",\n  \"routine\": [ { \"follow\": { \"line\": [\"START\", \"OUT\"] } } ]\n}\n";

    private File file;
    private RouteView view;

    @Before
    public void setUp() throws IOException {
        File repo = tmp.newFolder("repo");
        File autos = new File(repo, RouteView.AUTOS);
        assertTrue(autos.mkdirs());
        file = new File(autos, "r.json");
        write(GOOD);
        view = new RouteView(repo, "test-serial", false);
    }

    private void write(String text) throws IOException {
        Files.write(file.toPath(), text.getBytes(StandardCharsets.UTF_8));
    }

    private String read() throws IOException {
        return new String(Files.readAllBytes(file.toPath()), StandardCharsets.UTF_8);
    }

    private String base() throws IOException {
        return RouteChecksum.of(Files.readAllBytes(file.toPath()));
    }

    @Test
    public void savesValidTextOverTheVersionItWasLoadedFrom() throws IOException {
        String edited = GOOD.replace("24, 72", "30, 72");
        RouteView.Reply r = view.save("r", edited, base(), false);
        assertEquals(200, r.status);
        assertEquals(edited, read());
        assertEquals(base(), r.body.get("checksum").getAsString());
    }

    @Test
    public void refusesTextTheRobotWouldReject() throws IOException {
        RouteView.Reply r = view.save("r", GOOD.replace("\"OUT\"] }", "\"NOPE\"] }"), base(), false);
        assertEquals(422, r.status);
        assertTrue(r.body.get("error").getAsString(), r.body.get("error").getAsString().contains("unknown waypoint \"NOPE\""));
        assertEquals(GOOD, read());
    }

    @Test
    public void refusesToOverwriteAChangeMadeElsewhereUnlessForced() throws IOException {
        String loaded = base();
        String otherEditor = GOOD.replace("9, 72", "8, 72");
        write(otherEditor);
        RouteView.Reply r = view.save("r", GOOD.replace("24, 72", "30, 72"), loaded, false);
        assertEquals(409, r.status);
        assertEquals(base(), r.body.get("checksum").getAsString());
        assertEquals(otherEditor, read());

        assertEquals(200, view.save("r", GOOD.replace("24, 72", "30, 72"), loaded, true).status);
        assertEquals(GOOD.replace("24, 72", "30, 72"), read());
    }

    @Test
    public void keepsTheFilesLineEndings() throws IOException {
        write(GOOD.replace("\n", "\r\n"));
        String edited = GOOD.replace("24, 72", "30, 72"); // a browser sends \n
        assertEquals(200, view.save("r", edited, base(), false).status);
        assertEquals(edited.replace("\n", "\r\n"), read());
    }

    @Test
    public void onlyWritesRouteFilesThatExist() throws IOException {
        assertEquals(404, view.save("../../evil", GOOD, "x", true).status);
        assertEquals(404, view.save("new_route", GOOD, "x", true).status);
        assertFalse(new File(file.getParentFile(), "new_route.json").exists());
        assertEquals(404, view.source("../r").status);
        assertEquals(GOOD, view.source("r").body.get("text").getAsString());
    }

    @Test
    public void checksDraftsWithoutSaving() throws IOException {
        assertTrue(view.checkDraft(GOOD).get("valid").getAsBoolean());
        assertFalse(view.checkDraft("{ \"waypoints\": ").get("valid").getAsBoolean());
        assertEquals(GOOD, read());
    }
}
