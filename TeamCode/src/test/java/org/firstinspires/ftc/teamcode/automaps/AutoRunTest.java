package org.firstinspires.ftc.teamcode.automaps;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.firstinspires.ftc.teamcode.auto.Alliance;
import org.firstinspires.ftc.teamcode.auto.route.RouteParser;
import org.junit.Test;

/** The scoring and rule checks docs/auto-maps.md describes, on small made-up routes (18 in robot). */
public class AutoRunTest {

    private static AutoRun run(Alliance alliance, String waypoints, String routine) {
        String json = "{ \"waypoints\": {" + waypoints + "}, \"start\": \"START\", \"routine\": [" + routine + "] }";
        return new AutoRun(RouteParser.parse(json), alliance);
    }

    private static boolean mentions(AutoRun run, String text) {
        for (String p : run.problems) {
            if (p.contains(text)) return true;
        }
        return false;
    }

    /** Starts against the red wall, drives out, parks in the red LOADING ZONE. */
    private static final String GOOD_WAYPOINTS =
            "\"START\": [9, 72, 0], \"OUT\": [24, 72, 0], \"PARK\": [16, 106, 0]";
    private static final String GOOD_ROUTINE =
            "{ \"follow\": { \"line\": [\"START\", \"OUT\"] } }, { \"follow\": { \"line\": [\"OUT\", \"PARK\"] } }";

    @Test
    public void leaveAndParkInTheZoneScoresEightWithNoProblems() {
        AutoRun r = run(Alliance.RED, GOOD_WAYPOINTS, GOOD_ROUTINE);
        assertEquals(MapSettings.LEAVE_POINTS + MapSettings.PARK_POINTS, r.points);
        assertTrue(r.problems.toString(), r.problems.isEmpty());
        assertEquals(2, r.legs.size());
    }

    @Test
    public void endingOutsideTheZoneLosesPark() {
        AutoRun r = run(Alliance.RED, "\"START\": [9, 72, 0], \"OUT\": [24, 30, 0]",
                "{ \"follow\": { \"line\": [\"START\", \"OUT\"] } }");
        assertEquals(MapSettings.LEAVE_POINTS, r.points);
    }

    @Test
    public void startingIntoTheWallIsFlagged() {
        AutoRun r = run(Alliance.RED, "\"START\": [8, 72, 0], \"OUT\": [24, 72, 0]",
                "{ \"follow\": { \"line\": [\"START\", \"OUT\"] } }");
        assertTrue(r.problems.toString(), mentions(r, "into the wall"));
    }

    @Test
    public void crossingTheMidlineIsFlagged() {
        AutoRun r = run(Alliance.RED, "\"START\": [9, 20, 0], \"FAR\": [100, 20, 0]",
                "{ \"follow\": { \"line\": [\"START\", \"FAR\"] } }");
        assertTrue(r.problems.toString(), mentions(r, "G402"));
    }

    @Test
    public void drivingIntoTheHiveIsFlagged() {
        AutoRun r = run(Alliance.RED, "\"START\": [9, 72, 0], \"IN\": [60, 72, 0]",
                "{ \"follow\": { \"line\": [\"START\", \"IN\"] } }");
        assertTrue(r.problems.toString(), mentions(r, "HIVE"));
    }

    private static final String SEEK = "{ \"seek\": \"vision.ball\", \"args\": { \"within\": 30 }, \"timeout\": 3 }";

    @Test
    public void aSeekSearchesThenADriveToGuessesFromWhereItWas() {
        AutoRun r = run(Alliance.RED, GOOD_WAYPOINTS,
                "{ \"follow\": { \"line\": [\"START\", \"OUT\"] } }, " + SEEK + ", { \"driveTo\": \"PARK\", \"timeout\": 4 }");
        assertEquals(1, r.searches.size());
        assertEquals(30, r.searches.get(0).radius, 0);
        assertEquals(24, r.searches.get(0).x, 1e-6);
        assertEquals("only the follow is a known leg", 1, r.legs.size());
        assertEquals(1, r.guesses.size());
        assertTrue(r.problems.toString(), r.problems.isEmpty());
        assertEquals(AutoRun.moveSeconds(r.legs.get(0).length) + 3 + 4, r.seconds, 1e-9);
        assertTrue(r.rows.get(2).text, r.rows.get(2).text.contains("from wherever step 2 ended"));
    }

    @Test
    public void aFollowAfterASeekIsFlagged() {
        AutoRun r = run(Alliance.RED, GOOD_WAYPOINTS,
                "{ \"follow\": { \"line\": [\"START\", \"OUT\"] } }, " + SEEK + ", { \"follow\": { \"line\": [\"OUT\", \"PARK\"] } }");
        assertTrue(r.problems.toString(), mentions(r, "\"driveTo\": \"OUT\""));
    }

    @Test
    public void endingOnASeekIsFlagged() {
        AutoRun r = run(Alliance.RED, GOOD_WAYPOINTS, "{ \"follow\": { \"line\": [\"START\", \"OUT\"] } }, " + SEEK);
        assertTrue(r.problems.toString(), mentions(r, "End with a driveTo"));
    }

    @Test
    public void aDriveToFromAKnownPoseIsPlannedRoundTheHiveOnOurHalf() {
        // A straight line from START to FAR would clip the HIVE's corner.
        AutoRun r = run(Alliance.RED, "\"START\": [9, 72, 0], \"FAR\": [58, 122, 90]", "{ \"driveTo\": \"FAR\" }");
        assertEquals(1, r.legs.size());
        assertTrue(r.problems.toString(), r.problems.isEmpty());
        assertEquals(58, r.end.x(), 1e-6);
        assertEquals(122, r.end.y(), 1e-6);
        for (AutoRun r2 : new AutoRun[] {run(Alliance.BLUE, "\"START\": [9, 72, 0], \"FAR\": [58, 122, 90]", "{ \"driveTo\": \"FAR\" }")}) {
            assertTrue("BLUE too: " + r2.problems, r2.problems.isEmpty());
        }
    }

    @Test
    public void timeModelAndStepTimesAddUp() {
        // 12 in never reaches cruise speed: 2·sqrt(d/a) plus settle.
        assertEquals(2 * Math.sqrt(12 / MapSettings.ACCEL) + MapSettings.SETTLE, AutoRun.moveSeconds(12), 1e-9);
        AutoRun r = run(Alliance.RED, GOOD_WAYPOINTS,
                GOOD_ROUTINE + ", { \"wait\": 1.5 }, { \"action\": \"shooter.fire\", \"timeout\": 2 }");
        double legs = AutoRun.moveSeconds(r.legs.get(0).length) + AutoRun.moveSeconds(r.legs.get(1).length);
        assertEquals(legs + 1.5 + 2, r.seconds, 1e-9);
        assertEquals(4, r.rows.size());
    }
}
