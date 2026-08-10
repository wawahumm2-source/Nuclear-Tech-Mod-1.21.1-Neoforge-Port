package com.hbm.client.explosion;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class NuclearCloudMotionTest {
    @Test
    void sourceConvectionRemainsFiniteAtTheTorusCenter() {
        NuclearCloudMotion.Motion motion = NuclearCloudMotion.convection(
                10D, 20D, 0D, 10D, 20D, 4D, 1F
        );

        assertTrue(Double.isFinite(motion.x()));
        assertTrue(Double.isFinite(motion.y()));
        assertTrue(Double.isFinite(motion.z()));
    }

    @Test
    void sourceConvectionNormalizesOrdinaryMotion() {
        NuclearCloudMotion.Motion motion = NuclearCloudMotion.convection(
                7D, 17D, 0D, 10D, 20D, 4D, 1F
        );

        double length = Math.sqrt(motion.x() * motion.x() + motion.y() * motion.y() + motion.z() * motion.z());
        assertEquals(1D, length, 0.000001D);
    }

    @Test
    void azimuthRotatesTheTwoDimensionalSourceFieldAroundTheStem() {
        NuclearCloudMotion.Motion xAxis = NuclearCloudMotion.convection(
                7D, 12D, 0D, 10D, 20D, 4D, 0.7F
        );
        NuclearCloudMotion.Motion zAxis = NuclearCloudMotion.convection(
                7D, 12D, Math.PI * 0.5D, 10D, 20D, 4D, 0.7F
        );

        assertEquals(Math.abs(xAxis.x()), Math.abs(zAxis.z()), 0.000001D);
        assertEquals(xAxis.y(), zAxis.y(), 0.000001D);
    }

    @Test
    void sourceRingStopsBeyondTwiceTheTorusWidth() {
        NuclearCloudMotion.Motion motion = NuclearCloudMotion.ring(
                21D, 10D, 0D, 10D, 20D, 4D, 1F
        );

        assertEquals(NuclearCloudMotion.Motion.ZERO, motion);
    }
}
