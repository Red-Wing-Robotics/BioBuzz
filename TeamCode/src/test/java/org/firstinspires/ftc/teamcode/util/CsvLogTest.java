package org.firstinspires.ftc.teamcode.util;

import static org.junit.Assert.assertEquals;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Arrays;

public class CsvLogTest {

    @Rule
    public final TemporaryFolder folder = new TemporaryFolder();

    @Test
    public void writesTheHeaderOnceThenAppends() throws IOException {
        File file = new File(folder.getRoot(), "data/shots.csv"); // folder doesn't exist yet
        new CsvLog(file, "a,b").append("1,2");
        new CsvLog(file, "a,b").append("3,4"); // a later session keeps the rows
        assertEquals(Arrays.asList("a,b", "1,2", "3,4"), Files.readAllLines(file.toPath(), StandardCharsets.UTF_8));
    }

    @Test
    public void emptyFileGetsTheHeader() throws IOException {
        File file = folder.newFile("shots.csv");
        new CsvLog(file, "a,b").append("1,2");
        assertEquals(Arrays.asList("a,b", "1,2"), Files.readAllLines(file.toPath(), StandardCharsets.UTF_8));
    }
}
