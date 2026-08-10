package com.hbm.world.radiation;

public final class RadiationMath {
    public static final double TICKS_PER_SECOND = 20D;

    public static double resistanceMultiplier(double resistance) {
        return Math.pow(10D, -Math.max(0D, resistance));
    }

    public static double radiationForTicks(double ratePerSecond, int ticks, double resistance) {
        return Math.max(0D, ratePerSecond) * Math.max(0, ticks) / TICKS_PER_SECOND * resistanceMultiplier(resistance);
    }

    public static double localEmitterAttenuation(double distance, double radius, double exponent) {
        if (distance < 0D || radius <= 0D || distance >= radius) {
            return 0D;
        }
        double normalizedDistance = distance / radius;
        return Math.max(0D, 1D - Math.pow(normalizedDistance, Math.max(0.1D, exponent)));
    }

    public static double chanceAcrossTicks(int oneInChance, int elapsedTicks) {
        if (oneInChance <= 1) {
            return 1D;
        }
        int ticks = Math.max(1, elapsedTicks);
        return 1D - Math.pow(1D - 1D / oneInChance, ticks);
    }

    public static double distanceToUnitCube(double x, double y, double z, double blockX, double blockY, double blockZ) {
        double deltaX = distanceToRange(x, blockX, blockX + 1D);
        double deltaY = distanceToRange(y, blockY, blockY + 1D);
        double deltaZ = distanceToRange(z, blockZ, blockZ + 1D);
        return Math.sqrt(deltaX * deltaX + deltaY * deltaY + deltaZ * deltaZ);
    }

    private static double distanceToRange(double value, double minimum, double maximum) {
        if (value < minimum) {
            return minimum - value;
        }
        return value > maximum ? value - maximum : 0D;
    }

    public static double clamp(double value, double maximum) {
        return Math.max(0D, Math.min(value, Math.max(0D, maximum)));
    }

    /** Keeps a configured storage cap from making the configured fatal threshold unreachable. */
    public static double effectiveMaxExposure(double configuredMaximum, double fatalThreshold) {
        return Math.max(Math.max(0D, configuredMaximum), Math.max(0D, fatalThreshold));
    }

    private RadiationMath() {
    }
}
