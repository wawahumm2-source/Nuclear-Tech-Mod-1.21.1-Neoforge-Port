package com.hbm.world.explosion;

public final class HbmExplosionMath {
    public static final double RAY_STEP = 0.3D;
    public static final double AIR_TRAVEL_LOSS = RAY_STEP * 0.75D;

    private HbmExplosionMath() {
    }

    public static double initialRayPower(float terrainStrength, float randomUnit) {
        return terrainStrength * (0.7D + randomUnit * 0.6D);
    }

    /** EntityNukeExplosionMK5 doubles the configured bomb radius before tracing terrain rays. */
    public static float mk5TerrainStrength(int radius) {
        return radius * 2F;
    }

    /**
     * Tier 1 ExplosionNukeRayBatched uses a generalized spiral with this many points.
     * The modern scheduler may use a lower configured count, but this remains the parity target.
     */
    public static int mk5SourceRayCount(float terrainStrength) {
        if (terrainStrength <= 0F) {
            return 0;
        }
        return (int) (2.5D * Math.PI * terrainStrength * terrainStrength);
    }

    /** Source generalized-spiral height for the point after the current one. */
    public static double mk5SpiralHeight(int sourcePoint, int pointCount) {
        if (pointCount <= 1) {
            return 1D;
        }
        return -1D + 2D * sourcePoint / (pointCount - 1D);
    }

    /** Source generalized-spiral azimuth increment for the point after the current one. */
    public static double mk5SpiralAzimuthIncrement(int sourcePoint, int pointCount) {
        double height = mk5SpiralHeight(sourcePoint, pointCount);
        double denominator = Math.sqrt(Math.max(1.0E-12D, 1D - height * height));
        return 3.6D / Math.sqrt(Math.max(1, pointCount)) / denominator;
    }

    /**
     * Tier 1 ExplosionNukeRayBatched lowers its remaining strength by resistance raised to a distance-shaped power.
     */
    public static double mk5ResistanceLoss(double resistance, int step, int length) {
        if (length <= 0 || resistance <= 0D) {
            return 0D;
        }
        double falloff = (100D - (double) step / length * 100D) * 0.07D;
        return Math.pow(resistance, 7.5D - falloff);
    }

    /**
     * Modernized's strongest useful crater idea is an angle-aware downward bias. Keeping it quadratic preserves the
     * source horizontal envelope while smoothly increasing penetration toward the crater center.
     */
    public static double mk5DepthBias(double directionY, double depthMultiplier) {
        if (directionY >= 0D || depthMultiplier <= 1D) {
            return 1D;
        }
        double downward = Math.min(1D, -directionY);
        return 1D + (depthMultiplier - 1D) * downward * downward;
    }

    public static double mk5AdjustedResistanceLoss(double resistance, int step, int strengthLength,
            double directionY, double depthMultiplier) {
        return mk5ResistanceLoss(resistance, step, strengthLength) / mk5DepthBias(directionY, depthMultiplier);
    }

    /** ExplosionNukeRayBatched treats sandstone as stone and obsidian as triple stone resistance. */
    public static float mk5MasqueradeResistance(float resistance, boolean sandstone, boolean obsidian, float stoneResistance) {
        if (sandstone) {
            return stoneResistance;
        }
        if (obsidian) {
            return stoneResistance * 3F;
        }
        return resistance;
    }

    public static double remainingPowerAfterBlock(double remainingPower, float resistance) {
        return remainingPower - (resistance + 0.3D) * RAY_STEP;
    }

    public static double remainingPowerAfterTravel(double remainingPower) {
        return remainingPower - AIR_TRAVEL_LOSS;
    }

    public static double nuclearDamage(double distance, double killRadius, double maxDamage) {
        if (distance < 0D || killRadius <= 0D || distance > killRadius) {
            return 0D;
        }
        return maxDamage * (killRadius - distance) / killRadius;
    }

    public static int boundaryRayCount(int resolution) {
        if (resolution < 2) {
            return 0;
        }
        return 6 * resolution * resolution - 12 * resolution + 8;
    }

    public static boolean sourceFireRoll(int roll) {
        return Math.floorMod(roll, 3) == 0;
    }

    public static float deterministicRayRandom(long seed, int rayIndex) {
        long mixed = mix(seed + 0x9E3779B97F4A7C15L * rayIndex);
        return (mixed >>> 40) / 16_777_216F;
    }

    public static int deterministicFireRoll(long seed, int targetIndex) {
        return Math.floorMod((int) mix(seed ^ (0xC2B2AE3D27D4EB4FL * targetIndex)), 3);
    }

    public static int chainFuse(int baseFuse, int randomOffset) {
        return Math.floorMod(randomOffset, baseFuse) + baseFuse / 2;
    }

    private static long mix(long value) {
        value = (value ^ (value >>> 33)) * 0xff51afd7ed558ccdL;
        value = (value ^ (value >>> 33)) * 0xc4ceb9fe1a85ec53L;
        return value ^ (value >>> 33);
    }
}
