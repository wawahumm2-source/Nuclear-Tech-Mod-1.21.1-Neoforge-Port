package com.hbm.world.explosion;

/**
 * Source-backed, deterministic helpers for the EntityFalloutRain portion of a nuclear detonation.
 */
public final class HbmFalloutMath {
    public static final int SOURCE_WOOD_EFFECT_PERCENT = 65;
    public static final int SOURCE_SELLAFIELD_MAX_PERCENT = 50;
    public static final int SOURCE_GRASS_SELLAFIELD_MAX_PERCENT = 45;
    public static final int SOURCE_FIRE_CHANCE_DENOMINATOR = 5;
    public static final int SOURCE_FALLOUT_DEPTH = 3;

    private HbmFalloutMath() {
    }

    public static boolean isInsideRadius(int offsetX, int offsetZ, int radius) {
        return (long) offsetX * offsetX + (long) offsetZ * offsetZ <= (long) radius * radius;
    }

    public static double distancePercent(int offsetX, int offsetZ, int radius) {
        if (radius <= 0) {
            return 100D;
        }
        return Math.hypot(offsetX, offsetZ) * 100D / radius;
    }

    /**
     * FalloutConfigJSON maps each five-percent band to metadata 9 through 0.
     * A negative result means this distance is outside the source Sellafield conversion area.
     */
    public static int sellafieldStage(double distancePercent) {
        if (distancePercent < 0D || distancePercent > SOURCE_SELLAFIELD_MAX_PERCENT) {
            return -1;
        }
        // FalloutConfigJSON evaluates the i=1 (metadata 9) rule first and accepts its max distance.
        return Math.max(0, Math.min(9, 10 - (int) Math.ceil(distancePercent / 5D)));
    }

    public static double falloutDepositChance(double distancePercent) {
        double normalizedDistance = distancePercent / 100D;
        return Math.max(0D, 0.1D - Math.pow(normalizedDistance - 0.7D, 2D));
    }

    public static boolean shouldPlaceFire(double distancePercent, int roll) {
        return distancePercent < SOURCE_WOOD_EFFECT_PERCENT
                && Math.floorMod(roll, SOURCE_FIRE_CHANCE_DENOMINATOR) == 0;
    }

    /** Rebirth's persisted modern form of the source crater-biome radiation bands. */
    public static double craterRadiationRate(double distancePercent, int radius, double innerRate,
            double craterRate, double outerRate) {
        if (radius >= 150 && distancePercent < 15D) {
            return innerRate;
        }
        if (radius >= 100 && distancePercent < 55D) {
            return craterRate;
        }
        return radius >= 25 && distancePercent <= 100D ? outerRate : 0D;
    }

    /** Matches BlockSellafieldSlaked's coordinate texture selection. */
    public static int sourceTextureVariant(int x, int y, int z) {
        // Keep the original x-coordinate int overflow before widening to long.
        long value = (long) (x * 3_129_871) ^ (long) y * 116_129_781L ^ z;
        value = value * value * 42_317_861L + value * 11L;
        return (int) (value >>> 16 & 3L);
    }

    public static double deterministicUnit(long seed, int x, int y, int z, int salt) {
        long mixed = mix(seed
                ^ ((long) x * 0x9E3779B97F4A7C15L)
                ^ ((long) y * 0xC2B2AE3D27D4EB4FL)
                ^ ((long) z * 0x165667B19E3779F9L)
                ^ ((long) salt * 0x85EBCA77C2B2AE63L));
        return (mixed >>> 11) * 0x1.0p-53D;
    }

    public static int deterministicRoll(long seed, int x, int y, int z, int salt, int bound) {
        if (bound <= 0) {
            return 0;
        }
        long mixed = mix(seed
                ^ ((long) x * 0x9E3779B97F4A7C15L)
                ^ ((long) y * 0xC2B2AE3D27D4EB4FL)
                ^ ((long) z * 0x165667B19E3779F9L)
                ^ ((long) salt * 0x85EBCA77C2B2AE63L));
        return (int) Math.floorMod(mixed, bound);
    }

    private static long mix(long value) {
        value = (value ^ (value >>> 33)) * 0xff51afd7ed558ccdL;
        value = (value ^ (value >>> 33)) * 0xc4ceb9fe1a85ec53L;
        return value ^ value >>> 33;
    }
}
