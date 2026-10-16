package org.firstinspires.ftc.teamcode;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;

public class RobotIdTest {

    @Rule
    public final TemporaryFolder folder = new TemporaryFolder();

    @Test
    public void parseIgnoresCaseAndWhitespace() {
        assertEquals(RobotId.BELTESHAZZAR, RobotId.parse("belteshazzar"));
        assertEquals(RobotId.ASHURBANIPAL, RobotId.parse("  Ashurbanipal\r\n"));
    }

    @Test
    public void parseRejectsUnknownNames() {
        assertNull(RobotId.parse(""));
        assertNull(RobotId.parse("nebuchadnezzar"));
    }

    @Test
    public void writtenIdReadsBack() throws IOException {
        File file = new File(folder.getRoot(), RobotId.FILE_NAME);
        for (RobotId id : RobotId.values()) {
            RobotId.write(file, id);
            assertEquals(id, RobotId.read(file));
        }
    }

    @Test
    public void missingFileSaysHowToFixIt() {
        assertReadFails(new File(folder.getRoot(), RobotId.FILE_NAME), "missing");
    }

    @Test
    public void unknownNameSaysHowToFixIt() throws IOException {
        File file = folder.newFile(RobotId.FILE_NAME);
        try (OutputStream out = new FileOutputStream(file)) {
            out.write("robot2\n".getBytes(StandardCharsets.UTF_8));
        }
        assertReadFails(file, "\"robot2\"");
    }

    private static void assertReadFails(File file, String reason) {
        try {
            RobotId.read(file);
            fail("expected no robot ID");
        } catch (IllegalStateException e) {
            assertTrue(e.getMessage(), e.getMessage().contains(reason));
            assertTrue(e.getMessage(), e.getMessage().contains("Robot ID: Belteshazzar"));
        }
    }
}
