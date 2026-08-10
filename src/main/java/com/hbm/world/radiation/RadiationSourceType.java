package com.hbm.world.radiation;

/**
 * Identifies the system that produced a player's most recent radiation rate.
 * These are diagnostic channels, not separate radiation damage types.
 */
public enum RadiationSourceType {
    INVENTORY,
    BLOCK,
    FALLOUT,
    EXPLOSION,
    DIMENSION,
    SCRIPTED
}
