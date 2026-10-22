package org.firstinspires.ftc.teamcode.util;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;

/**
 * Appends rows to a CSV file, writing the header first if the file is new or empty. Each
 * {@link #append} opens, writes and closes the file, so every row is on disk as soon as it
 * returns and a crash or power loss loses nothing already logged. That costs a few
 * milliseconds per row: call it on a button press, not every loop.
 */
public final class CsvLog {

    private final File file;
    private final String header;

    public CsvLog(File file, String header) {
        this.file = file;
        this.header = header;
    }

    public File file() {
        return file;
    }

    /** Add one line (no line break) to the end of the file, creating the file and its folder if needed. */
    public void append(String row) throws IOException {
        File dir = file.getParentFile();
        if (dir != null && !dir.isDirectory() && !dir.mkdirs()) {
            throw new IOException("can't create " + dir);
        }
        boolean needsHeader = !file.isFile() || file.length() == 0;
        StringBuilder text = new StringBuilder();
        if (needsHeader) {
            text.append(header).append('\n');
        }
        text.append(row).append('\n');
        try (OutputStream out = new FileOutputStream(file, true)) {
            out.write(text.toString().getBytes(StandardCharsets.UTF_8));
        }
    }
}
