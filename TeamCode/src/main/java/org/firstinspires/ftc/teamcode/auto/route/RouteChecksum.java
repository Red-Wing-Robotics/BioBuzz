package org.firstinspires.ftc.teamcode.auto.route;

import java.util.zip.CRC32;

/**
 * A short fingerprint of a route file's bytes. The Driver Station shows it at INIT and
 * tools/routeview shows it for the laptop's copy and the hub's, so one glance says whether the
 * robot is running the file you just saved.
 */
public final class RouteChecksum {

    private RouteChecksum() {}

    /** CRC-32 of the file's bytes as 8 lowercase hex digits, e.g. {@code "3610a686"}. */
    public static String of(byte[] bytes) {
        CRC32 crc = new CRC32();
        crc.update(bytes, 0, bytes.length);
        return String.format("%08x", crc.getValue());
    }
}
