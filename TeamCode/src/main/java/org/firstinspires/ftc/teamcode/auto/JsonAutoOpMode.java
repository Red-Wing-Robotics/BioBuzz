package org.firstinspires.ftc.teamcode.auto;

import org.firstinspires.ftc.teamcode.auto.route.ActionRegistry;
import org.firstinspires.ftc.teamcode.auto.route.Route;
import org.firstinspires.ftc.teamcode.auto.route.RouteCompiler;
import org.firstinspires.ftc.teamcode.auto.route.RouteFiles;
import org.firstinspires.ftc.teamcode.auto.route.RouteParser;

import java.util.LinkedHashMap;

/**
 * An autonomous OpMode whose routine comes from a JSON route file (see auto/readme.md).
 * Subclasses name the route and register any subsystem actions the file refers to.
 * The file is read, validated and compiled at INIT; problems show on the Driver Station then.
 */
public abstract class JsonAutoOpMode extends AutoOpMode {

    private Route route;
    private RouteCompiler compiler;
    private String source;

    /** File name without extension: assets/autos/NAME.json or /sdcard/FIRST/autos/NAME.json. */
    protected abstract String routeName();

    /**
     * Register the actions, conditions and seeks this route may use, e.g.
     * {@code r.action("shooter.spinUp", args -> shooter.spinUp())}. Subsystems made in
     * {@code createSubsystems()} exist by now.
     */
    protected void registerActions(ActionRegistry registry) {}

    @Override
    protected final void onInit() {
        String name = routeName();
        RouteFiles.Loaded loaded = RouteFiles.read(name, hardwareMap.appContext);
        route = RouteParser.parse(loaded.json);
        source = loaded.source;

        if (AutoWaypoints.reloadFromFileOnInit || !name.equals(AutoWaypoints.route)) {
            AutoWaypoints.waypoints = new LinkedHashMap<>(route.waypoints);
            AutoWaypoints.route = name;
        }

        ActionRegistry registry = new ActionRegistry();
        registerActions(registry);
        compiler = new RouteCompiler(alliance(), AutoWaypoints.waypoints, registry);
        // The checksum matches the one tools/routeview shows for the file it pushed.
        initLine("route", name + " — " + source + " · " + loaded.checksum);
        initLine("waypoints", AutoWaypoints.reloadFromFileOnInit ? "from file" : "from Panels (reloadFromFileOnInit=false)");
    }

    @Override
    protected final Waypoint startWaypoint() {
        return AutoWaypoints.waypoints.get(route.start);
    }

    @Override
    protected final Step routine() {
        return compiler.compile(route);
    }
}
