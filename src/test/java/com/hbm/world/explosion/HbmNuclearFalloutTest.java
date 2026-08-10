package com.hbm.world.explosion;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Map;
import org.junit.jupiter.api.Test;

class HbmNuclearFalloutTest {
    @Test
    void lowYieldUsesTheSourceTwentyFiveChunkDiamond() {
        Map<HbmNuclearFallout.ChunkOffset, Double> fallout = HbmNuclearFallout.lowYieldDistribution(2);

        assertEquals(25, fallout.size());
        assertEquals(100D / 3D, fallout.get(new HbmNuclearFallout.ChunkOffset(0, 0)), 0.000001D);
        assertEquals(50D / 3D, fallout.get(new HbmNuclearFallout.ChunkOffset(1, 0)), 0.000001D);
        assertEquals(100D / 9D, fallout.get(new HbmNuclearFallout.ChunkOffset(1, 1)), 0.000001D);
        assertEquals(25D / 3D, fallout.get(new HbmNuclearFallout.ChunkOffset(3, 0)), 0.000001D);
        assertFalse(fallout.containsKey(new HbmNuclearFallout.ChunkOffset(3, 1)));
    }

    @Test
    void zeroRadiationLevelCreatesNoFallout() {
        assertTrue(HbmNuclearFallout.lowYieldDistribution(0).isEmpty());
    }
}
