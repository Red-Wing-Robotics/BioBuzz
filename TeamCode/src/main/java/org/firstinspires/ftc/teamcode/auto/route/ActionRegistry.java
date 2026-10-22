package org.firstinspires.ftc.teamcode.auto.route;

import com.google.gson.JsonObject;

import org.firstinspires.ftc.teamcode.auto.Step;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.BooleanSupplier;

/**
 * Names that route files can refer to. An OpMode registers its subsystem actions, sensor
 * conditions and seeks here in {@code registerActions}; the compiler resolves {@code "action"},
 * {@code "waitUntil"} and {@code "seek"} steps against it and fails at INIT on unknown names.
 */
public final class ActionRegistry {

    /**
     * Builds a Step from the optional {@code "args"} object of an action or seek step. Throw a
     * {@link RouteException} for bad args: it is shown at INIT with the step's location.
     */
    public interface ActionFactory {
        Step create(JsonObject args);
    }

    private final Map<String, ActionFactory> actions = new LinkedHashMap<>();
    private final Map<String, BooleanSupplier> conditions = new LinkedHashMap<>();
    private final Map<String, ActionFactory> seeks = new LinkedHashMap<>();

    public ActionRegistry action(String name, ActionFactory factory) {
        actions.put(name, factory);
        return this;
    }

    public ActionRegistry condition(String name, BooleanSupplier condition) {
        conditions.put(name, condition);
        return this;
    }

    /**
     * A {@code "seek"}: a drive to somewhere the robot decides as it runs, such as a ball the
     * camera sees. Unlike an action, the route file then says the robot moved to a place nobody
     * can draw in advance, and the auto maps show it that way.
     */
    public ActionRegistry seek(String name, ActionFactory factory) {
        seeks.put(name, factory);
        return this;
    }

    Step action(String name, JsonObject args, String at) {
        ActionFactory factory = actions.get(name);
        if (factory == null) {
            throw new RouteException(at + ": unknown action \"" + name + "\" (registered: " + actions.keySet() + ")");
        }
        return create(factory, args, at);
    }

    Step seek(String name, JsonObject args, String at) {
        ActionFactory factory = seeks.get(name);
        if (factory == null) {
            throw new RouteException(at + ": unknown seek \"" + name + "\" (registered: " + seeks.keySet() + ")");
        }
        return create(factory, args, at);
    }

    private static Step create(ActionFactory factory, JsonObject args, String at) {
        try {
            return factory.create(args);
        } catch (RouteException e) {
            throw new RouteException(at + ": " + e.getMessage(), e);
        }
    }

    BooleanSupplier condition(String name, String at) {
        BooleanSupplier condition = conditions.get(name);
        if (condition == null) {
            throw new RouteException(at + ": unknown condition \"" + name + "\" (registered: " + conditions.keySet() + ")");
        }
        return condition;
    }
}
