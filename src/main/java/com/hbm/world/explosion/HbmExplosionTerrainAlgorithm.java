package com.hbm.world.explosion;

/** Terrain engines retained separately because Tier 1 uses different math for low-yield and MK5 nuclear blasts. */
public enum HbmExplosionTerrainAlgorithm {
    EXPLOSION_NT,
    MK5_GENERALIZED_SPIRAL,
    MK5_HYBRID_RADIAL
}
