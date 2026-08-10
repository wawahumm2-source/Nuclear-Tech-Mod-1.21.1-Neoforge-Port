package com.hbm.blockentity.machine;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BurnerPressTimingTest {
    @Test
    void continuousFuelDrainConsumesFourTicksPerServerTick() {
        assertEquals(1596, BurnerPressTiming.burnFuel(1600, true));
        assertEquals(0, BurnerPressTiming.burnFuel(2, true));
        assertEquals(1600, BurnerPressTiming.burnFuel(1600, false));
    }

    @Test
    void normalHeatRisesOnceEveryFourTicks() {
        assertEquals(0, BurnerPressTiming.advanceSpeed(0, true, false, 3));
        assertEquals(1, BurnerPressTiming.advanceSpeed(0, true, false, 4));
        assertEquals(400, BurnerPressTiming.advanceSpeed(400, true, false, 8));
    }

    @Test
    void preheaterKeepsFourTimesTheNormalHeatIncrease() {
        assertEquals(4, BurnerPressTiming.advanceSpeed(0, true, true, 4));
        assertEquals(400, BurnerPressTiming.advanceSpeed(398, true, true, 4));
    }

    @Test
    void coolingAndResidualHeatThresholdFollowTheApprovedHybridRule() {
        assertEquals(15, BurnerPressTiming.advanceSpeed(16, false, false, 5));
        assertTrue(BurnerPressTiming.canRunRecipe(false, 16));
        assertFalse(BurnerPressTiming.canRunRecipe(false, 15));
    }

    @Test
    void retractionAlwaysCompletesAtLowHeat() {
        assertEquals(0, BurnerPressTiming.getStampSpeed(15));
        assertEquals(1, BurnerPressTiming.getRetractionSpeed(15));
    }
}
