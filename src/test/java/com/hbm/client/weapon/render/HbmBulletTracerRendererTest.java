package com.hbm.client.weapon.render;

import net.minecraft.world.phys.Vec3;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class HbmBulletTracerRendererTest {
    @Test
    void clipsFastTrajectoryToShortLeadingStreak() {
        HbmBulletTracerRenderer.Segment segment = HbmBulletTracerRenderer.clipSegment(
                new Vec3(0.0D, 0.0D, 0.45D),
                new Vec3(0.0D, 0.0D, 16.45D),
                Vec3.ZERO);

        assertEquals(0.90D, segment.start().distanceTo(segment.end()), 1.0E-6D);
        assertEquals(16.45D, segment.end().z, 1.0E-6D);
    }

    @Test
    void suppressesTrajectoryEntirelyInsideMuzzleHideDistance() {
        assertNull(HbmBulletTracerRenderer.clipSegment(
                new Vec3(0.0D, 0.0D, 0.45D),
                new Vec3(0.0D, 0.0D, 0.85D),
                Vec3.ZERO));
    }

    @Test
    void preservesShortImpactStreakAfterMuzzleClearance() {
        HbmBulletTracerRenderer.Segment segment = HbmBulletTracerRenderer.clipSegment(
                new Vec3(0.0D, 0.0D, 0.45D),
                new Vec3(0.0D, 0.0D, 1.40D),
                Vec3.ZERO);

        assertEquals(1.0D, segment.start().z, 1.0E-6D);
        assertEquals(1.40D, segment.end().z, 1.0E-6D);
    }

    @Test
    void cameraFacingRibbonHasStableWidthAndIsPerpendicularToFlight() {
        Vec3 start = new Vec3(0.0D, 0.0D, 8.0D);
        Vec3 end = new Vec3(0.0D, 0.0D, 8.9D);
        Vec3 side = HbmBulletTracerRenderer.ribbonSide(start, end, Vec3.ZERO, 0.026D);

        assertEquals(0.026D, side.length(), 1.0E-6D);
        assertEquals(0.0D, side.dot(end.subtract(start)), 1.0E-6D);
        assertTrue(side.lengthSqr() > 0.0D);
    }
}
