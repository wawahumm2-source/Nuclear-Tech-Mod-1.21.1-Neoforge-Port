package com.hbm.world.radiation;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class RadiationMathTest {
    @Test
    void oneRadPerSecondAddsOneRadAcrossTwentyTicks() {
        assertEquals(1D, RadiationMath.radiationForTicks(1D, 20, 0D), 0.000001D);
    }

    @Test
    void sourceResistanceUsesTheOriginalPowerOfTenCurve() {
        assertEquals(0.1D, RadiationMath.resistanceMultiplier(1D), 0.000001D);
        assertEquals(0.01D, RadiationMath.resistanceMultiplier(2D), 0.000001D);
        assertEquals(0.1D, RadiationMath.radiationForTicks(1D, 20, 1D), 0.000001D);
    }

    @Test
    void radiationCannotBecomeNegative() {
        assertEquals(0D, RadiationMath.clamp(-5D, 2500D), 0.000001D);
        assertEquals(2500D, RadiationMath.clamp(3000D, 2500D), 0.000001D);
    }

    @Test
    void storageCapCannotMakeTheFatalThresholdUnreachable() {
        assertEquals(1000D, RadiationMath.effectiveMaxExposure(250D, 1000D), 0.000001D);
        assertEquals(2500D, RadiationMath.effectiveMaxExposure(2500D, 1000D), 0.000001D);
    }

    @Test
    void localEmittersFadeWithDistanceAndStopAtTheConfiguredRadius() {
        assertEquals(1D, RadiationMath.localEmitterAttenuation(0D, 4D, 2D), 0.000001D);
        assertEquals(0.75D, RadiationMath.localEmitterAttenuation(2D, 4D, 2D), 0.000001D);
        assertEquals(0D, RadiationMath.localEmitterAttenuation(4D, 4D, 2D), 0.000001D);
        assertEquals(0D, RadiationMath.localEmitterAttenuation(5D, 4D, 2D), 0.000001D);
    }

    @Test
    void localEmitterDistanceIsMeasuredFromTheBlockVolumeInsteadOfItsCenter() {
        assertEquals(0.5D, RadiationMath.distanceToUnitCube(0.5D, 64.9D, 0.5D, 1D, 64D, 0D), 0.000001D);
        assertEquals(0.75D, RadiationMath.localEmitterAttenuation(0.5D, 1D, 2D), 0.000001D);
    }

    @Test
    void batchedMobTicksPreserveTheExpectedPerTickEffectChance() {
        assertEquals(1D / 300D, RadiationMath.chanceAcrossTicks(300, 1), 0.000001D);
        assertEquals(1D - Math.pow(299D / 300D, 5D), RadiationMath.chanceAcrossTicks(300, 5), 0.000001D);
    }
}
