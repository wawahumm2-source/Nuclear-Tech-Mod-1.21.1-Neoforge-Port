package com.hbm.world.explosion;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class HbmFalloutMathTest {
    @Test
    void sellafieldStagesMatchTheSourceFivePercentBands() {
        assertEquals(9, HbmFalloutMath.sellafieldStage(0D));
        assertEquals(9, HbmFalloutMath.sellafieldStage(4.999D));
        assertEquals(9, HbmFalloutMath.sellafieldStage(5D));
        assertEquals(8, HbmFalloutMath.sellafieldStage(5.001D));
        assertEquals(1, HbmFalloutMath.sellafieldStage(44.999D));
        assertEquals(0, HbmFalloutMath.sellafieldStage(50D));
        assertEquals(-1, HbmFalloutMath.sellafieldStage(50.001D));
    }

    @Test
    void falloutDepositChanceUsesTheSourceSeventyPercentPeak() {
        assertEquals(0D, HbmFalloutMath.falloutDepositChance(0D), 0.000001D);
        assertEquals(0.1D, HbmFalloutMath.falloutDepositChance(70D), 0.000001D);
        assertEquals(0.01D, HbmFalloutMath.falloutDepositChance(100D), 0.000001D);
    }

    @Test
    void sourceFireRuleIsInnerSixtyFivePercentAndOneInFive() {
        assertTrue(HbmFalloutMath.shouldPlaceFire(64.999D, 0));
        assertFalse(HbmFalloutMath.shouldPlaceFire(64.999D, 1));
        assertFalse(HbmFalloutMath.shouldPlaceFire(65D, 0));
    }

    @Test
    void coordinateTextureVariantAndRandomnessAreStable() {
        int variant = HbmFalloutMath.sourceTextureVariant(123, 64, -456);
        assertTrue(variant >= 0 && variant <= 3);
        assertEquals(variant, HbmFalloutMath.sourceTextureVariant(123, 64, -456));
        assertEquals(
                HbmFalloutMath.deterministicUnit(42L, 1, 2, 3, 4),
                HbmFalloutMath.deterministicUnit(42L, 1, 2, 3, 4),
                0D
        );
    }

    @Test
    void circularFalloutBoundaryDoesNotProcessOutsideColumns() {
        assertTrue(HbmFalloutMath.isInsideRadius(3, 4, 5));
        assertFalse(HbmFalloutMath.isInsideRadius(4, 4, 5));
    }

    @Test
    void sourceWoodDistanceCanBeRetainedAsAConfigurableProfileValue() {
        assertTrue(64.999D < HbmFalloutMath.SOURCE_WOOD_EFFECT_PERCENT);
        assertFalse(65D < HbmFalloutMath.SOURCE_WOOD_EFFECT_PERCENT);
    }

    @Test
    void rebirthCraterRadiationBandsRemainSourceFamilyDefaults() {
        assertEquals(25D, HbmFalloutMath.craterRadiationRate(14.999D, 300, 25D, 5D, 0.5D));
        assertEquals(5D, HbmFalloutMath.craterRadiationRate(15D, 300, 25D, 5D, 0.5D));
        assertEquals(5D, HbmFalloutMath.craterRadiationRate(54.999D, 300, 25D, 5D, 0.5D));
        assertEquals(0.5D, HbmFalloutMath.craterRadiationRate(55D, 300, 25D, 5D, 0.5D));
        assertEquals(0D, HbmFalloutMath.craterRadiationRate(100.001D, 300, 25D, 5D, 0.5D));
    }
}
