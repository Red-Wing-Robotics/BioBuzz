package org.firstinspires.ftc.teamcode.teleop;

import org.firstinspires.ftc.teamcode.auto.AutoContext;
import org.firstinspires.ftc.teamcode.auto.Step;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;

/**
 * Autonomous actions the drivers trigger from TeleOp. A TeleOp defines each assist in
 * {@code registerAssists}: a name from {@link Assist}, the {@link Resource}s it uses, and a factory
 * that builds a fresh {@link Step} each time (the same Steps autos use). Schemes request assists
 * by name; {@link TeleOpBase} runs them every loop.
 *
 * <ul>
 * <li>Pressed: starts the assist; pressing again cancels it. It ends when its Step finishes.</li>
 * <li>Held: runs while the button is held; release cancels it. If it finishes while still held,
 *     it doesn't restart until the button is released and held again.</li>
 * <li>Starting an assist interrupts any running assist that shares a resource with it. Assists
 *     with no resources in common run together.</li>
 * <li>An {@link Definition#endgameOnly() endgame-only} assist won't start before 1:00 remains.</li>
 * <li>Moving the drive sticks cancels assists that use DRIVE (done by TeleOpBase).</li>
 * </ul>
 *
 * Hardware-free: tests drive it with fake Steps and a fake clock.
 */
public final class Assists {

    /** How long a notice ("not available", "endgame only") stays on the Driver Station. */
    private static final double NOTICE_SECONDS = 3;

    /** One assist's definition. */
    public static final class Definition {
        final String name;
        final Set<Resource> uses;
        final Supplier<Step> factory;
        boolean endgameOnly;

        Definition(String name, Set<Resource> uses, Supplier<Step> factory) {
            this.name = name;
            this.uses = uses;
            this.factory = factory;
        }

        /** Refuse to start before the last 60 s, e.g. for NECTAR into FLOWERS (G410). */
        public Definition endgameOnly() {
            endgameOnly = true;
            return this;
        }
    }

    private static final class Running {
        final Definition definition;
        final Step step;
        final boolean held;

        Running(Definition definition, Step step, boolean held) {
            this.definition = definition;
            this.step = step;
            this.held = held;
        }
    }

    private final Map<String, Definition> definitions = new LinkedHashMap<>();
    private final List<Running> running = new ArrayList<>();
    private final Set<String> finishedWhileHeld = new HashSet<>();
    private String notice;
    private double noticeTime;

    /** Define what an assist does. {@code factory} runs each time the assist starts. */
    public Definition define(String name, Set<Resource> uses, Supplier<Step> factory) {
        if (definitions.containsKey(name)) {
            throw new IllegalArgumentException("assist \"" + name + "\" is defined twice");
        }
        Set<Resource> copy = uses.isEmpty() ? EnumSet.noneOf(Resource.class) : EnumSet.copyOf(uses);
        Definition definition = new Definition(name, copy, factory);
        definitions.put(name, definition);
        return definition;
    }

    /** True while a running assist uses {@code resource}; manual control of it should pause. */
    public boolean owns(Resource resource) {
        for (Running r : running) {
            if (r.definition.uses.contains(resource)) {
                return true;
            }
        }
        return false;
    }

    public boolean isRunning(String name) {
        return find(name) != null;
    }

    /** Running assists as "name › current step", for telemetry. */
    public List<String> describe() {
        List<String> lines = new ArrayList<>();
        for (Running r : running) {
            lines.add(r.definition.name + " › " + r.step.name());
        }
        return lines;
    }

    /** The latest notice if it is less than a few seconds old, else null. */
    public String notice(double now) {
        return notice != null && now - noticeTime <= NOTICE_SECONDS ? notice : null;
    }

    // ---- driven by TeleOpBase, once per loop ----------------------------------------

    /** Start, cancel, or release assists according to this loop's requests. */
    void request(DriverCommands commands, AutoContext ctx, boolean endgame) {
        List<String> held = commands.heldAssists();
        for (Iterator<Running> it = running.iterator(); it.hasNext(); ) {
            Running r = it.next();
            if (r.held && !held.contains(r.definition.name)) {
                it.remove();
                r.step.end(ctx, true);
            }
        }
        finishedWhileHeld.retainAll(held);

        for (String name : commands.pressedAssists()) {
            Running r = find(name);
            if (r != null) {
                running.remove(r);
                r.step.end(ctx, true);
            } else {
                start(name, false, ctx, endgame);
            }
        }
        for (String name : held) {
            if (find(name) == null && !finishedWhileHeld.contains(name)) {
                start(name, true, ctx, endgame);
            }
        }
    }

    /** Interrupt every running assist that uses {@code resource}. */
    void cancel(Resource resource, AutoContext ctx) {
        for (Iterator<Running> it = running.iterator(); it.hasNext(); ) {
            Running r = it.next();
            if (r.definition.uses.contains(resource)) {
                it.remove();
                r.step.end(ctx, true);
            }
        }
    }

    void cancelAll(AutoContext ctx) {
        for (Running r : new ArrayList<>(running)) {
            r.step.end(ctx, true);
        }
        running.clear();
    }

    /** Advance every running assist one loop; finished ones end and release their resources. */
    void update(AutoContext ctx) {
        for (Iterator<Running> it = running.iterator(); it.hasNext(); ) {
            Running r = it.next();
            if (r.step.update(ctx)) {
                it.remove();
                r.step.end(ctx, false);
                if (r.held) {
                    finishedWhileHeld.add(r.definition.name);
                }
            }
        }
    }

    private void start(String name, boolean held, AutoContext ctx, boolean endgame) {
        Definition definition = definitions.get(name);
        if (definition == null) {
            notice(name + ": not available in this OpMode", ctx);
            return;
        }
        if (definition.endgameOnly && !endgame) {
            notice(name + ": endgame only", ctx);
            return;
        }
        for (Iterator<Running> it = running.iterator(); it.hasNext(); ) {
            Running r = it.next();
            if (!Collections.disjoint(r.definition.uses, definition.uses)) {
                it.remove();
                r.step.end(ctx, true);
            }
        }
        Step step = definition.factory.get();
        if (step == null) {
            throw new IllegalStateException("assist \"" + name + "\": factory returned no Step");
        }
        step.start(ctx);
        running.add(new Running(definition, step, held));
    }

    private Running find(String name) {
        for (Running r : running) {
            if (r.definition.name.equals(name)) {
                return r;
            }
        }
        return null;
    }

    private void notice(String text, AutoContext ctx) {
        notice = text;
        noticeTime = ctx.time();
    }
}
