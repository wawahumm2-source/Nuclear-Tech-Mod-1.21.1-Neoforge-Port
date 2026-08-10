package com.hbm.world.explosion;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class HbmExplosionMathTest {
    @Test
    void sourceRayPowerAndResistanceLossUseTheVerifiedExplosionNtFormula() {
        assertEquals(10.5D, HbmExplosionMath.initialRayPower(15F, 0F), 0.000001D);
        assertEquals(19.5D, HbmExplosionMath.initialRayPower(15F, 1F), 0.000001D);
        assertEquals(9.81D, HbmExplosionMath.remainingPowerAfterBlock(10.5D, 2F), 0.000001D);
        assertEquals(9.585D, HbmExplosionMath.remainingPowerAfterTravel(9.81D), 0.000001D);
    }

    @Test
    void mk5TerrainStrengthDoublesTheConfiguredSourceRadius() {
        assertEquals(240F, HbmExplosionMath.mk5TerrainStrength(120));
        assertEquals(452389, HbmExplosionMath.mk5SourceRayCount(240F));
    }

    @Test
    void mk5SpiralAdvanceUsesTheOriginalGeneralizedSpiralFormula() {
        int count = 100;
        double height = HbmExplosionMath.mk5SpiralHeight(1, count);
        assertEquals(-1D + 2D / 99D, height, 0.000001D);
        assertEquals(3.6D / Math.sqrt(count) / Math.sqrt(1D - height * height),
                HbmExplosionMath.mk5SpiralAzimuthIncrement(1, count), 0.000001D);
    }

    @Test
    void mk5ResistanceUsesTheSourceDistanceExponentAndMasqueradeRules() {
        assertEquals(Math.pow(2D, 0.5D), HbmExplosionMath.mk5ResistanceLoss(2D, 0, 120), 0.000001D);
        assertEquals(Math.pow(2D, 7.5D - (100D - 119D / 120D * 100D) * 0.07D),
                HbmExplosionMath.mk5ResistanceLoss(2D, 119, 120), 0.000001D);
        assertEquals(6F, HbmExplosionMath.mk5MasqueradeResistance(20F, false, true, 2F));
        assertEquals(2F, HbmExplosionMath.mk5MasqueradeResistance(20F, true, false, 2F));
    }

    @Test
    void littleBoyUsesDoubledStrengthForFalloffAndOnlyBiasesDownwardRays() {
        assertEquals(1D, HbmExplosionMath.mk5DepthBias(0D, 2.5D), 0.000001D);
        assertEquals(1D, HbmExplosionMath.mk5DepthBias(0.5D, 2.5D), 0.000001D);
        assertEquals(1.375D, HbmExplosionMath.mk5DepthBias(-0.5D, 2.5D), 0.000001D);
        assertEquals(2.5D, HbmExplosionMath.mk5DepthBias(-1D, 2.5D), 0.000001D);

        double correctedMidRayLoss = HbmExplosionMath.mk5ResistanceLoss(6D, 60, 240);
        double oldIncorrectLoss = HbmExplosionMath.mk5ResistanceLoss(6D, 60, 120);
        assertTrue(correctedMidRayLoss < oldIncorrectLoss / 20D);
    }

    @Test
    void defaultLittleBoyDepthBiasProducesADeepContinuousStoneCenterline() {
        int sourceDepth = solidPenetrationDepth(6D, 240, 120, -1D, 1D);
        int adjustedDepth = solidPenetrationDepth(6D, 240, 120, -1D, 2.5D);
        assertEquals(35, sourceDepth);
        assertEquals(50, adjustedDepth);
        assertTrue(adjustedDepth > sourceDepth + 10);
    }

    @Test
    void nuclearDamageUsesTheSourceLinearKillRadiusFalloff() {
        assertEquals(250D, HbmExplosionMath.nuclearDamage(0D, 45D, 250D), 0.000001D);
        assertEquals(125D, HbmExplosionMath.nuclearDamage(22.5D, 45D, 250D), 0.000001D);
        assertEquals(0D, HbmExplosionMath.nuclearDamage(45D, 45D, 250D), 0.000001D);
        assertEquals(0D, HbmExplosionMath.nuclearDamage(45.01D, 45D, 250D), 0.000001D);
    }

    @Test
    void boundaryRayCountMatchesTheResolutionCubeSurface() {
        assertEquals(23816, HbmExplosionMath.boundaryRayCount(64));
    }

    @Test
    void sourceFireRollOnlyAcceptsOneOutOfThreeRolls() {
        assertTrue(HbmExplosionMath.sourceFireRoll(0));
        assertFalse(HbmExplosionMath.sourceFireRoll(1));
        assertFalse(HbmExplosionMath.sourceFireRoll(2));
        assertTrue(HbmExplosionMath.sourceFireRoll(3));
    }

    @Test
    void deterministicJobRandomnessIsStableAcrossReloads() {
        assertEquals(
                HbmExplosionMath.deterministicRayRandom(42L, 99),
                HbmExplosionMath.deterministicRayRandom(42L, 99),
                0F
        );
        assertEquals(
                HbmExplosionMath.deterministicFireRoll(42L, 99),
                HbmExplosionMath.deterministicFireRoll(42L, 99)
        );
    }

    @Test
    void sourceTntChainFuseUsesTheTenThroughTwentyNineWindow() {
        assertEquals(10, HbmExplosionMath.chainFuse(20, 0));
        assertEquals(29, HbmExplosionMath.chainFuse(20, 19));
    }

    private static int solidPenetrationDepth(double resistance, int strength, int maxDistance,
            double directionY, double depthMultiplier) {
        double remaining = strength;
        for (int step = 0; step < maxDistance; step++) {
            remaining -= HbmExplosionMath.mk5AdjustedResistanceLoss(
                    resistance, step, strength, directionY, depthMultiplier
            );
            if (remaining <= 0D) {
                return step;
            }
        }
        return maxDistance;
    }
}
