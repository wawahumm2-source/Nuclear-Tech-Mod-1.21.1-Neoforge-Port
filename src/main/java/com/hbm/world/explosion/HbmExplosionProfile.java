package com.hbm.world.explosion;

import net.minecraft.resources.ResourceLocation;
public record HbmExplosionProfile(
        HbmExplosionMode mode,
        HbmExplosionExecution terrainExecution,
        HbmExplosionTerrainAlgorithm terrainAlgorithm,
        float terrainStrength,
        float maxTerrainDistance,
        float craterDepthMultiplier,
        int rayResolution,
        int rayCount,
        float killRadius,
        float maxDamage,
        int radiationLevel,
        int radiationBurstTicks,
        float radiationBurstBaseDose,
        float radiationBurstRange,
        boolean noBlockDrops,
        HbmFalloutProfile fallout,
        ResourceLocation sound,
        ResourceLocation clientEffect
) {
}
