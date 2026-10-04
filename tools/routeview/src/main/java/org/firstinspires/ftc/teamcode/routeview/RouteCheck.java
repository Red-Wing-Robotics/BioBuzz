package org.firstinspires.ftc.teamcode.routeview;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import org.firstinspires.ftc.teamcode.auto.Alliance;
import org.firstinspires.ftc.teamcode.auto.Steps;
import org.firstinspires.ftc.teamcode.auto.route.ActionRegistry;
import org.firstinspires.ftc.teamcode.auto.route.Route;
import org.firstinspires.ftc.teamcode.auto.route.RouteCompiler;
import org.firstinspires.ftc.teamcode.auto.route.RouteException;
import org.firstinspires.ftc.teamcode.auto.route.RouteParser;
import org.firstinspires.ftc.teamcode.automaps.LiveMap;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

/**
 * Checks a route file the way the robot does at INIT ({@link RouteParser}, then
 * {@link RouteCompiler} for both alliances), and draws it ({@link LiveMap}) if it passes.
 *
 * Action and condition names are registered by OpModes at runtime, which needs the robot, so
 * here every name compiles and a name no TeamCode source registers is a warning, not an error.
 */
final class RouteCheck {

    /** The outcome for one version of a file. {@code error == null} means the robot would load it. */
    static final class Result {
        final String error;
        final List<String> warnings;
        final String routeName;
        final JsonObject red;
        final JsonObject blue;

        private Result(String error, List<String> warnings, String routeName, JsonObject red, JsonObject blue) {
            this.error = error;
            this.warnings = warnings;
            this.routeName = routeName;
            this.red = red;
            this.blue = blue;
        }

        boolean valid() {
            return error == null;
        }

        static Result invalid(String error) {
            return new Result(error, Collections.<String>emptyList(), null, null, null);
        }
    }

    private RouteCheck() {}

    static Result check(String json, Registrations registered) {
        Route route;
        Set<String> actions = new TreeSet<>();
        Set<String> conditions = new TreeSet<>();
        try {
            route = RouteParser.parse(json);
            collectNames(route.routine, actions, conditions);
            ActionRegistry anyName = new ActionRegistry();
            for (String a : actions) {
                anyName.action(a, args -> Steps.instant(a, () -> {}));
            }
            for (String c : conditions) {
                anyName.condition(c, () -> true);
            }
            for (Alliance alliance : Alliance.values()) {
                RouteCompiler compiler = new RouteCompiler(alliance, route.waypoints, anyName);
                compiler.startPose(route);
                compiler.compile(route);
            }
        } catch (RouteException e) {
            return Result.invalid(e.getMessage());
        } catch (RuntimeException e) {
            // Gson's getAs* on the wrong JSON type, e.g. a heading of {"linear": 90}.
            return Result.invalid("route: " + e);
        }

        List<String> warnings = new ArrayList<>();
        for (String a : actions) {
            if (!registered.actions.contains(a)) {
                warnings.add("action \"" + a + "\" isn't registered anywhere in TeamCode (no .action(\"" + a
                        + "\", …)), so INIT will fail on the robot.");
            }
        }
        for (String c : conditions) {
            if (!registered.conditions.contains(c)) {
                warnings.add("condition \"" + c + "\" isn't registered anywhere in TeamCode (no .condition(\"" + c
                        + "\", …)), so INIT will fail on the robot.");
            }
        }

        try {
            return new Result(null, warnings, route.name,
                    LiveMap.report(route, Alliance.RED), LiveMap.report(route, Alliance.BLUE));
        } catch (RuntimeException e) {
            return Result.invalid("compiles, but drawing it failed: " + e);
        }
    }

    /** Every {@code "action"} and {@code "waitUntil"} name anywhere in the routine. */
    private static void collectNames(JsonElement e, Set<String> actions, Set<String> conditions) {
        if (e.isJsonArray()) {
            for (JsonElement child : e.getAsJsonArray()) {
                collectNames(child, actions, conditions);
            }
        } else if (e.isJsonObject()) {
            for (Map.Entry<String, JsonElement> entry : e.getAsJsonObject().entrySet()) {
                JsonElement v = entry.getValue();
                boolean name = v.isJsonPrimitive() && v.getAsJsonPrimitive().isString();
                if (name && entry.getKey().equals("action")) {
                    actions.add(v.getAsString());
                } else if (name && entry.getKey().equals("waitUntil")) {
                    conditions.add(v.getAsString());
                } else {
                    collectNames(v, actions, conditions);
                }
            }
        }
    }

    /**
     * Action and condition names TeamCode registers, found by searching its sources for
     * {@code .action("name"} and {@code .condition("name"}. A text search, so a name built at
     * runtime isn't seen; that only costs a false warning.
     */
    static final class Registrations {
        private static final Pattern ACTION = Pattern.compile("\\.action\\(\\s*\"([^\"]+)\"");
        private static final Pattern CONDITION = Pattern.compile("\\.condition\\(\\s*\"([^\"]+)\"");

        final Set<String> actions;
        final Set<String> conditions;

        Registrations(Set<String> actions, Set<String> conditions) {
            this.actions = actions;
            this.conditions = conditions;
        }

        static Registrations scan(File sourceRoot) {
            Set<String> actions = new TreeSet<>();
            Set<String> conditions = new TreeSet<>();
            try (Stream<java.nio.file.Path> files = Files.walk(sourceRoot.toPath())) {
                files.filter(p -> p.toString().endsWith(".java")).forEach(p -> {
                    try {
                        String text = new String(Files.readAllBytes(p), StandardCharsets.UTF_8);
                        find(ACTION, text, actions);
                        find(CONDITION, text, conditions);
                    } catch (IOException ignored) {
                        // mid-save; the next scan sees it
                    }
                });
            } catch (IOException | RuntimeException e) {
                // no sources (moved repo?): every name will warn, which is the safe direction
            }
            return new Registrations(actions, conditions);
        }

        private static void find(Pattern pattern, String text, Set<String> into) {
            Matcher m = pattern.matcher(text);
            while (m.find()) {
                into.add(m.group(1));
            }
        }

        @Override
        public boolean equals(Object o) {
            return o instanceof Registrations && ((Registrations) o).actions.equals(actions)
                    && ((Registrations) o).conditions.equals(conditions);
        }

        @Override
        public int hashCode() {
            return actions.hashCode() * 31 + conditions.hashCode();
        }
    }
}
