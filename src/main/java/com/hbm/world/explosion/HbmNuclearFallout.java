package com.hbm.world.explosion;

import java.util.LinkedHashMap;
import java.util.Map;

public final class HbmNuclearFallout {
    private static final int LOW_YIELD_CHUNK_RADIUS = 3;

    private HbmNuclearFallout() {
    }

    /**
     * Tier 1 ExplosionNukeSmall fallout: a 25-chunk Manhattan diamond with 50/(distance+1) RAD scaled by level/3.
     */
    public static Map<ChunkOffset, Double> lowYieldDistribution(int radiationLevel) {
        Map<ChunkOffset, Double> fallout = new LinkedHashMap<>();
        double modifier = Math.max(0, radiationLevel) / 3D;
        if (modifier <= 0D) {
            return fallout;
        }

        for (int x = -LOW_YIELD_CHUNK_RADIUS; x <= LOW_YIELD_CHUNK_RADIUS; x++) {
            for (int z = -LOW_YIELD_CHUNK_RADIUS; z <= LOW_YIELD_CHUNK_RADIUS; z++) {
                int distance = Math.abs(x) + Math.abs(z);
                if (distance <= LOW_YIELD_CHUNK_RADIUS) {
                    fallout.put(new ChunkOffset(x, z), 50D / (distance + 1D) * modifier);
                }
            }
        }
        return fallout;
    }

    public record ChunkOffset(int x, int z) {
    }
}
