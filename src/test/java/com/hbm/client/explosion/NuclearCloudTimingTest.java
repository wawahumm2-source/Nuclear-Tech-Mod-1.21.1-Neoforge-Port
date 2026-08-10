package com.hbm.client.explosion;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class NuclearCloudTimingTest {
    @Test
    void reloadedLifetimeHasThreeHundredTickFloor() {
        assertEquals(300, NuclearCloudTiming.totalLifetimeTicks(200, 0F));
    }

    @Test
    void prototypeUsesReloadedRadiusLifetime() {
        assertEquals(528, NuclearCloudTiming.totalLifetimeTicks(200, 15F));
    }

    @Test
    void littleBoyUsesReloadedRadiusLifetimeAndModernFinalFade() {
        assertEquals(10172, NuclearCloudTiming.totalLifetimeTicks(1350, 120F));
        assertEquals(9155, NuclearCloudTiming.fadeStartTick(10172));
    }

    @Test
    void largerRadiusPersistsLongerWithoutShorteningFormation() {
        assertEquals(38915, NuclearCloudTiming.totalLifetimeTicks(4500, 250F));
        assertEquals(35023, NuclearCloudTiming.fadeStartTick(38915));
    }

    @Test
    void aftermathDriftStartsOnlyAfterSourceLifecycle() {
        assertEquals(0F, NuclearCloudTiming.aftermathProgress(1350, 1350, 10172));
        assertEquals(0F, NuclearCloudTiming.aftermathProgress(900, 1350, 10172));
        assertEquals(1F, NuclearCloudTiming.aftermathProgress(10172, 1350, 10172));
        assertEquals(0.5F, NuclearCloudTiming.aftermathProgress(5761, 1350, 10172), 0.0001F);
    }

    @Test
    void persistentCloudNeverFreezesBeforeItsFinalFade() {
        assertEquals(1D, NuclearCloudTiming.simulationSpeed(0, 1350, 10172), 0D);
        assertEquals(1D, NuclearCloudTiming.simulationSpeed(337, 1350, 10172), 0D);
        assertEquals(NuclearCloudTiming.PERSISTENT_FORMATION_TAIL_SPEED,
                NuclearCloudTiming.simulationSpeed(1350, 1350, 10172), 0.000001D);
        assertEquals(NuclearCloudTiming.PERSISTENT_END_SPEED,
                NuclearCloudTiming.simulationSpeed(10172, 1350, 10172), 0.000001D);
        assertEquals(NuclearCloudTiming.PERSISTENT_END_SPEED,
                NuclearCloudTiming.simulationSpeed(11000, 1350, 10172), 0.000001D);
    }

    @Test
    void persistentCloudTailDecaysWithoutReversing() {
        double formationEnd = NuclearCloudTiming.simulationSpeed(1350, 1350, 10172);
        double middle = NuclearCloudTiming.simulationSpeed(5761, 1350, 10172);
        double finalTick = NuclearCloudTiming.simulationSpeed(10172, 1350, 10172);

        org.junit.jupiter.api.Assertions.assertTrue(formationEnd > middle);
        org.junit.jupiter.api.Assertions.assertTrue(middle > finalTick);
        org.junit.jupiter.api.Assertions.assertTrue(finalTick > 0D);
    }

    @Test
    void sourceLengthCloudRetainsTierOneStop() {
        assertEquals(1D, NuclearCloudTiming.simulationSpeed(225, 900, 900), 0D);
        assertEquals(0D, NuclearCloudTiming.simulationSpeed(450, 900, 900), 0D);
    }

    @Test
    void pressureFrontUsesTierOneLifetimeRadiusAndDelayedMotion() {
        assertEquals(300, NuclearCloudTiming.shockLifetimeTicks(0));
        assertEquals(50, NuclearCloudTiming.shockLifetimeTicks(20));
        assertEquals(45D, NuclearCloudTiming.shockRadius(20, 0D), 0.0001D);
        assertEquals(46.5D, NuclearCloudTiming.shockRadius(20, 1D), 0.0001D);
        assertEquals(0D, NuclearCloudTiming.shockMotionMultiplier(15), 0D);
        assertEquals(0.75D, NuclearCloudTiming.shockMotionMultiplier(16), 0D);
    }
}
