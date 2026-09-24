package org.firstinspires.ftc.teamcode.auto.route;

import com.google.gson.JsonObject;

import org.firstinspires.ftc.teamcode.auto.Step;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.BooleanSupplier;

/**
 * Names that route files can refer to. An OpMode registers its subsystem actions and sensor
 * conditions here in {@code registerActions}; the compiler resolves {@code "action"} and
 * {@code "waitUntil"} steps against it and fails at INIT on unknown names.
 */
public final class ActionRegistry {

    /** Builds a Step from the optional {@code "args"} object of an action step. */
    public interface ActionFactory {
        Step create(JsonObject args);
    }

    private final Map<String, ActionFactory> actions = new LinkedHashMap<>();
    private final Map<String, BooleanSupplier> conditions = new LinkedHashMap<>();

    public ActionRegistry action(String name, ActionFactory factory) {
        actions.put(name, factory);
        return this;
    }

    public ActionRegistry condition(String name, BooleanSupplier condition) {
        conditions.put(name, condition);
        return this;
    }

    Step action(String name, JsonObject args, String at) {
        ActionFactory factory = actions.get(name);
        if (factory == null) {
            throw new RouteException(at + ": unknown action \"" + name + "\" (registered: " + actions.keySet() + ")");
        }
        return factory.create(args);
    }

    BooleanSupplier condition(String name, String at) {
        BooleanSupplier condition = conditions.get(name);
        if (condition == null) {
            throw new RouteException(at + ": unknown condition \"" + name + "\" (registered: " + conditions.keySet() + ")");
        }
        return condition;
    }
}
