package org.firstinspires.ftc.teamcode.auto.route;

/** A route file is missing, malformed, or references something unknown. Shown on the DS at INIT. */
public class RouteException extends RuntimeException {
    public RouteException(String message) {
        super(message);
    }

    public RouteException(String message, Throwable cause) {
        super(message, cause);
    }
}
