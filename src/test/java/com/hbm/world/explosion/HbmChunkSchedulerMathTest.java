package com.hbm.world.explosion;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class HbmChunkSchedulerMathTest {
    @Test
    void packedChunkCoordinatesRoundTripAcrossNegativeWorldCoordinates() {
        long key = HbmChunkSchedulerMath.packChunk(-12, 37);

        assertEquals(-12, HbmChunkSchedulerMath.unpackChunkX(key));
        assertEquals(37, HbmChunkSchedulerMath.unpackChunkZ(key));
    }

    @Test
    void horizontalRadiusIncludesBoundaryAndRejectsOutsidePositions() {
        assertTrue(HbmChunkSchedulerMath.isInsideHorizontalRadius(0, 0, 0, 0, 5));
        assertTrue(HbmChunkSchedulerMath.isInsideHorizontalRadius(0, 0, 5, 0, 5));
        assertFalse(HbmChunkSchedulerMath.isInsideHorizontalRadius(0, 0, 4, 4, 5));
    }

    @Test
    void localTargetIndexKeepsChunkCoordinatesSeparateFromBuildHeight() {
        assertEquals(0, HbmChunkSchedulerMath.localTargetBitIndex(0, 0, 0));
        assertEquals(98_303, HbmChunkSchedulerMath.localTargetBitIndex(15, 383, 15));
    }
}
