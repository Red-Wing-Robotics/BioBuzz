package org.firstinspires.ftc.teamcode.routeview;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.TimeUnit;

/** Runs the Android SDK's {@code adb} with a timeout. */
final class Adb {

    static final class Result {
        final int exit;
        final byte[] out;
        final String err;
        final boolean timedOut;

        Result(int exit, byte[] out, String err) {
            this(exit, out, err, false);
        }

        private Result(int exit, byte[] out, String err, boolean timedOut) {
            this.exit = exit;
            this.out = out;
            this.err = err;
            this.timedOut = timedOut;
        }

        boolean ok() {
            return exit == 0;
        }

        String text() {
            return new String(out, StandardCharsets.UTF_8).trim();
        }

        /** What went wrong, for the log: adb's own words if it gave any. */
        String problem() {
            String s = !err.trim().isEmpty() ? err.trim() : text();
            return s.isEmpty() ? "adb exited with " + exit : s.split("\n")[0];
        }
    }

    final String path;

    private Adb(String path) {
        this.path = path;
    }

    /**
     * adb from the SDK Android Studio uses ({@code sdk.dir} in local.properties), then
     * ANDROID_HOME / ANDROID_SDK_ROOT, then the PATH. Null if none is found.
     */
    static Adb find(File repoRoot) {
        String exe = System.getProperty("os.name", "").toLowerCase().startsWith("windows") ? "adb.exe" : "adb";
        List<String> sdks = new ArrayList<>();
        File local = new File(repoRoot, "local.properties");
        if (local.isFile()) {
            Properties p = new Properties();
            try (InputStream in = new FileInputStream(local)) {
                p.load(in);
                if (p.getProperty("sdk.dir") != null) {
                    sdks.add(p.getProperty("sdk.dir"));
                }
            } catch (IOException ignored) {
                // fall through to the environment
            }
        }
        for (String env : Arrays.asList("ANDROID_HOME", "ANDROID_SDK_ROOT")) {
            if (System.getenv(env) != null) {
                sdks.add(System.getenv(env));
            }
        }
        for (String sdk : sdks) {
            File adb = new File(new File(sdk, "platform-tools"), exe);
            if (adb.canExecute()) {
                return new Adb(adb.getPath());
            }
        }
        String pathVar = System.getenv("PATH");
        for (String dir : pathVar == null ? new String[0] : pathVar.split(File.pathSeparator)) {
            File adb = new File(dir, exe);
            if (adb.canExecute()) {
                return new Adb(adb.getPath());
            }
        }
        return null;
    }

    Result run(long timeoutMillis, String... args) {
        List<String> command = new ArrayList<>();
        command.add(path);
        command.addAll(Arrays.asList(args));
        try {
            Process p = new ProcessBuilder(command).start();
            p.getOutputStream().close();
            // Read both streams on their own threads so a chatty adb can't block on a full pipe.
            Drain out = new Drain(p.getInputStream());
            Drain err = new Drain(p.getErrorStream());
            if (!p.waitFor(timeoutMillis, TimeUnit.MILLISECONDS)) {
                p.destroyForcibly();
                return new Result(-1, new byte[0], "adb timed out after " + timeoutMillis / 1000.0 + " s", true);
            }
            out.join();
            err.join();
            return new Result(p.exitValue(), out.bytes(), new String(err.bytes(), StandardCharsets.UTF_8));
        } catch (IOException e) {
            return new Result(-1, new byte[0], "cannot run " + path + ": " + e.getMessage());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return new Result(-1, new byte[0], "interrupted");
        }
    }

    private static final class Drain extends Thread {
        private final InputStream in;
        private final ByteArrayOutputStream buffer = new ByteArrayOutputStream();

        Drain(InputStream in) {
            this.in = in;
            setDaemon(true);
            start();
        }

        @Override
        public void run() {
            byte[] chunk = new byte[8192];
            try {
                int n;
                while ((n = in.read(chunk)) != -1) {
                    buffer.write(chunk, 0, n);
                }
            } catch (IOException ignored) {
                // process killed
            }
        }

        byte[] bytes() {
            return buffer.toByteArray();
        }
    }
}
