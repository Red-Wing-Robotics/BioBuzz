package org.firstinspires.ftc.teamcode.auto.route;

import android.content.Context;

import org.firstinspires.ftc.robotcore.internal.system.AppUtil;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

/**
 * Finds a route file by name. An edited copy on the Control Hub wins over the one built into
 * the APK, so routes can be changed at an event without a rebuild:
 *
 *   1. /sdcard/FIRST/autos/NAME.json   (push with: adb connect 192.168.43.1:5555 &&
 *                                        adb push NAME.json /sdcard/FIRST/autos/)
 *   2. APK asset  TeamCode/src/main/assets/autos/NAME.json   (the version-controlled copy)
 */
public final class RouteFiles {

    public static final class Loaded {
        public final String json;
        public final String source;

        Loaded(String json, String source) {
            this.json = json;
            this.source = source;
        }
    }

    private RouteFiles() {}

    public static File overrideFile(String name) {
        return new File(new File(AppUtil.FIRST_FOLDER, "autos"), name + ".json");
    }

    public static Loaded read(String name, Context context) {
        File override = overrideFile(name);
        if (override.isFile()) {
            try (InputStream in = new FileInputStream(override)) {
                return new Loaded(readAll(in), override.getPath());
            } catch (IOException e) {
                throw new RouteException("route \"" + name + "\": cannot read " + override, e);
            }
        }
        String asset = "autos/" + name + ".json";
        try (InputStream in = context.getAssets().open(asset)) {
            return new Loaded(readAll(in), "APK asset " + asset);
        } catch (IOException e) {
            throw new RouteException("route \"" + name + "\" not found: no " + override + " and no APK asset " + asset, e);
        }
    }

    private static String readAll(InputStream in) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        byte[] buffer = new byte[4096];
        int n;
        while ((n = in.read(buffer)) != -1) {
            out.write(buffer, 0, n);
        }
        return new String(out.toByteArray(), StandardCharsets.UTF_8);
    }
}
