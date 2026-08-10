package com.hbm.world.explosion;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class HbmExplosionExecutionTest {
    @Test
    void lowYieldProfilesExecuteImmediatelyButLargerProfilesStayBatched() {
        assertEquals(HbmExplosionExecution.IMMEDIATE, HbmExplosionExecution.forTerrainStrength(15F, 15));
        assertEquals(HbmExplosionExecution.BATCHED, HbmExplosionExecution.forTerrainStrength(16F, 15));
        assertEquals(HbmExplosionExecution.BATCHED, HbmExplosionExecution.forTerrainStrength(0F, 15));
    }
}
