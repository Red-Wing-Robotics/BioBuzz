package org.firstinspires.ftc.teamcode.routeview;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** RobotLink against a stand-in for RouteViewRemote that follows the same contract. */
public class RobotLinkTest {

    private HttpServer robot;
    private RobotLink link;
    private final List<String> heard = Collections.synchronizedList(new ArrayList<>());
    private String phase = "IDLE";

    @Before
    public void setUp() throws IOException {
        robot = HttpServer.create(new InetSocketAddress(InetAddress.getLoopbackAddress(), 0), 0);
        robot.createContext("/routeview/", this::handle);
        robot.start();
        link = new RobotLink(robot.getAddress().getPort());
    }

    @After
    public void tearDown() {
        robot.stop(0);
    }

    private void handle(HttpExchange x) throws IOException {
        String path = x.getRequestURI().getPath();
        heard.add(x.getRequestMethod() + " " + path + (x.getRequestURI().getQuery() != null ? "?" + x.getRequestURI().getQuery() : ""));
        if (path.endsWith("/status")) {
            send(x, 200, "{\"active\":null,\"phase\":\"" + phase + "\",\"ms\":0,\"opModes\":[{\"name\":\"Red: Leave + Park\",\"route\":\"leave_and_park\",\"alliance\":\"RED\"}]}");
        } else if (!x.getRequestHeaders().containsKey("X-RouteView")) {
            send(x, 403, "{\"error\":\"missing X-RouteView header\"}");
        } else if (path.endsWith("/start") && phase.equals("IDLE")) {
            send(x, 409, "{\"error\":\"nothing is in INIT\"}");
        } else {
            phase = path.endsWith("/init") ? "INIT" : path.endsWith("/start") ? "RUNNING" : "IDLE";
            send(x, 200, "{\"phase\":\"" + phase + "\"}");
        }
    }

    private static void send(HttpExchange x, int status, String body) throws IOException {
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        x.sendResponseHeaders(status, bytes.length);
        try (OutputStream out = x.getResponseBody()) {
            out.write(bytes);
        }
    }

    @Test
    public void readsTheStatusAndItsOpModes() {
        RobotLink.Answer a = link.status();
        assertTrue(a.problem(), a.ok());
        assertEquals("leave_and_park", a.body.getAsJsonArray("opModes").get(0).getAsJsonObject().get("route").getAsString());
    }

    @Test
    public void runsInitStartStopWithTheHeaderTheRobotRequires() {
        assertTrue(link.init("Red: Leave + Park").ok());
        assertTrue(link.start().ok());
        assertEquals("RUNNING", phase);
        assertTrue(link.stop().ok());
        assertEquals("IDLE", phase);
        assertTrue(heard.toString(), heard.contains("POST /routeview/init?name=Red: Leave + Park"));
    }

    @Test
    public void passesOnTheRobotsReasonForRefusing() {
        RobotLink.Answer a = link.start();
        assertFalse(a.ok());
        assertEquals(409, a.status);
        assertEquals("nothing is in INIT", a.problem());
    }

    @Test
    public void explainsABuildWithoutRouteViewControl() {
        robot.removeContext("/routeview/");
        RobotLink.Answer a = link.status();
        assertEquals(404, a.status);
        assertTrue(a.problem(), a.problem().contains("dev build"));
    }

    @Test
    public void explainsARobotAppThatIsntAnswering() {
        robot.stop(0);
        RobotLink.Answer a = link.status();
        assertEquals(0, a.status);
        assertTrue(a.problem(), a.problem().contains("isn't answering"));
    }

    @Test
    public void acceptsRobotCodeOfItsOwnProtocol() {
        assertNull(RobotLink.protocolProblem(status("{\"protocol\":" + RobotLink.PROTOCOL + ",\"phase\":\"IDLE\"}")));
    }

    @Test
    public void saysToReinstallABuildFromBeforeTheProtocolField() {
        // Such a build reported the SDK's idle "$Stop$Robot$" as an active OpMode, locking out INIT.
        String problem = RobotLink.protocolProblem(status("{\"phase\":\"IDLE\",\"sdkActive\":\"$Stop$Robot$\"}"));
        assertNotNull(problem);
        assertTrue(problem, problem.contains("older") && problem.contains("devDebug"));
    }

    @Test
    public void saysToUpdateJimothyForNewerRobotCode() {
        String problem = RobotLink.protocolProblem(status("{\"protocol\":" + (RobotLink.PROTOCOL + 1) + "}"));
        assertNotNull(problem);
        assertTrue(problem, problem.contains("newer") && problem.contains("restart Jimothy"));
    }

    @Test
    public void protocolMatchesTheRobotsRouteViewRemote() throws IOException {
        String source = new String(Files.readAllBytes(Paths.get(
                "TeamCode/src/dev/java/org/firstinspires/ftc/teamcode/auto/RouteViewRemote.java")), StandardCharsets.UTF_8);
        Matcher m = Pattern.compile("static final int PROTOCOL = (\\d+);").matcher(source);
        assertTrue("RouteViewRemote declares PROTOCOL", m.find());
        assertEquals("bump RobotLink.PROTOCOL and RouteViewRemote.PROTOCOL together",
                RobotLink.PROTOCOL, Integer.parseInt(m.group(1)));
    }

    private static JsonObject status(String json) {
        return new JsonParser().parse(json).getAsJsonObject();
    }
}
