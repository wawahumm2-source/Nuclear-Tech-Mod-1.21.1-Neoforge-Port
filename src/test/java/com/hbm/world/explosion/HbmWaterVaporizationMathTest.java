package com.hbm.world.explosion;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class HbmWaterVaporizationMathTest {
    @Test
    void surveyRingExtendsPastTheMutationBoundary() {
        assertEquals(302, HbmWaterVaporizationMath.surveyRadius(300, 2));
        assertEquals(1, HbmWaterVaporizationMath.surveyRadius(0, 0));
    }

    @Test
    void onlyProvenContainedBodiesEvaporate() {
        assertTrue(HbmWaterVaporizationMath.shouldEvaporateComponent(false, 4096, 8192));
        assertFalse(HbmWaterVaporizationMath.shouldEvaporateComponent(true, 4096, 8192));
        assertFalse(HbmWaterVaporizationMath.shouldEvaporateComponent(false, 8193, 8192));
        assertFalse(HbmWaterVaporizationMath.shouldEvaporateComponent(false, 0, 8192));
    }

    @Test
    void craterRefillRequiresAnOpeningWhoseAirPathReachesTheCrater() {
        assertTrue(HbmWaterVaporizationMath.canRefillFromOpening(true, true, true));
        assertFalse(HbmWaterVaporizationMath.canRefillFromOpening(true, true, false));
        assertFalse(HbmWaterVaporizationMath.canRefillFromOpening(true, false, true));
        assertFalse(HbmWaterVaporizationMath.canRefillFromOpening(false, true, true));
    }
}
