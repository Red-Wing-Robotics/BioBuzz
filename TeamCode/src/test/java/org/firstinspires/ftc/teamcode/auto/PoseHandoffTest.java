package org.firstinspires.ftc.teamcode.auto;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;

import com.pedropathing.api.PoseFactory;
import com.pedropathing.math.Pose;

import org.junit.After;
import org.junit.Test;

public class PoseHandoffTest {

    private static final long SECOND = 1_000_000_000L;

    private final Pose parked = PoseFactory.degrees().of(20, 20, 0);

    @After
    public void forget() {
        PoseHandoff.clear();
    }

    @Test
    public void recentPoseIsHandedOver() {
        PoseHandoff.save(parked, 0);
        PoseHandoff.Saved saved = PoseHandoff.recent(10 * SECOND);
        assertSame(parked, saved.pose);
        assertEquals(10.0, saved.ageSeconds, 1e-9);
    }

    @Test
    public void stalePoseIsIgnored() {
        PoseHandoff.save(parked, 0);
        assertNull(PoseHandoff.recent(31 * SECOND));
    }

    @Test
    public void nothingSavedMeansNoHandoff() {
        assertNull(PoseHandoff.recent(0));
    }
}
