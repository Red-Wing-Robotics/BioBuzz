package org.firstinspires.ftc.teamcode;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.firstinspires.ftc.teamcode.opmodes.starterbot.StarterBotHardware;
import org.junit.Test;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.HashSet;
import java.util.Set;

/**
 * Guards the hardware config-name constants of each robot: every one must be non-blank and
 * unique. Catches copy-paste mistakes before they turn into "device not found" on the robot.
 */
public class RobotHardwareTest {

    @Test
    public void configNamesAreNonBlankAndUnique() throws IllegalAccessException {
        assertNamesNonBlankAndUnique(RobotHardware.class);
    }

    @Test
    public void starterBotConfigNamesAreNonBlankAndUnique() throws IllegalAccessException {
        assertNamesNonBlankAndUnique(StarterBotHardware.class);
    }

    private static void assertNamesNonBlankAndUnique(Class<?> names) throws IllegalAccessException {
        Set<String> seen = new HashSet<>();
        int count = 0;
        for (Field field : names.getDeclaredFields()) {
            int mods = field.getModifiers();
            if (Modifier.isStatic(mods) && Modifier.isFinal(mods) && field.getType() == String.class) {
                String name = (String) field.get(null);
                assertFalse(field.getName() + " is blank", name.trim().isEmpty());
                assertTrue(field.getName() + " duplicates config name '" + name + "'", seen.add(name));
                count++;
            }
        }
        assertTrue("expected at least one config-name constant", count > 0);
    }
}
