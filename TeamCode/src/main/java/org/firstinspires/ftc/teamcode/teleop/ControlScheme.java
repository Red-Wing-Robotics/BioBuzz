package org.firstinspires.ftc.teamcode.teleop;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * A complete gamepad layout. A scheme's {@link Binding} fields are its control card, declared in
 * the order the Driver Station should list them:
 *
 * <pre>
 * private final Binding fire = button(GAMEPAD2, Button.RIGHT_BUMPER, "hold: fire");
 * </pre>
 *
 * {@link #map} turns this loop's {@link DriverInput} into {@link DriverCommands}. Schemes are
 * hardware-free, so their mappings are unit-tested, and ControlSchemesTest rejects any input
 * bound twice. {@link TeleOpBase} shows the control card on the Driver Station during INIT.
 */
public abstract class ControlScheme {

    private final List<Binding> bindings = new ArrayList<>();

    /** Short name for the Driver Station, e.g. "two drivers". */
    public abstract String name();

    /** Fill {@code out} (already cleared) from this loop's input. Called once per loop. */
    public abstract void map(DriverInput in, DriverCommands out);

    /** Every binding, in declaration order: the control card. */
    public final List<Binding> bindings() {
        return Collections.unmodifiableList(bindings);
    }

    protected final Binding button(Pad pad, Button button, String action) {
        return add(new Binding(pad, button, null, action));
    }

    protected final Binding axis(Pad pad, Axis axis, String action) {
        return add(new Binding(pad, null, axis, action));
    }

    /**
     * Robot-centric mecanum drive (driver preference, see docs/decisions.md): shapes the sticks
     * with {@link DriveFeel}, applies slow mode, and converts to Pedro's convention.
     */
    protected static void robotCentricDrive(DriverInput in, Binding forward, Binding strafe,
                                            Binding turn, Binding slow, DriverCommands out) {
        out.slow = in.held(slow);
        double scale = out.slow ? DriveFeel.slowModeScale() : 1.0;
        // Sticks read up/right as positive; Pedro's +strafe is left and +turn is counter-clockwise.
        out.forward = DriveFeel.shape(in.value(forward)) * scale;
        out.strafe = -DriveFeel.shape(in.value(strafe)) * scale;
        out.turn = -DriveFeel.shape(in.value(turn)) * scale;
    }

    private Binding add(Binding binding) {
        bindings.add(binding);
        return binding;
    }
}
