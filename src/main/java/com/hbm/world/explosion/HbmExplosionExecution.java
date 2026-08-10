package com.hbm.world.explosion;

/** Controls whether a source profile completes in one server tick or uses the safe terrain scheduler. */
public enum HbmExplosionExecution {
    IMMEDIATE,
    BATCHED;

    public static HbmExplosionExecution forTerrainStrength(float terrainStrength, int immediateStrengthLimit) {
        return terrainStrength > 0F && terrainStrength <= immediateStrengthLimit ? IMMEDIATE : BATCHED;
    }
}
