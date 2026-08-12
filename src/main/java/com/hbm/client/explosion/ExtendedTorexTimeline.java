package com.hbm.client.explosion;

/** Pure constants and timing equations from NTM Extended 3.0.3's EntityNukeTorex. */
final class ExtendedTorexTimeline {
    static final int MAX_CLOUDLETS = 20_000;
    static final double SHOCK_SPEED = 2D;
    static final int FLASH_BASE_DURATION = 30;
    static final int FLARE_BASE_DURATION = 100;

    static float scale(float sourceRadius) {
        return clamp(sourceRadius * 0.01F, 0.25F, 5F);
    }

    static int maxAge(float scale) {
        return Math.max(1, (int) (45 * 20 * scale));
    }

    static int cloudletLifetime(int age, int maxAge) {
        long earlyLife = (long) age * age + 200L;
        return (int) Math.min(earlyLife, (long) maxAge - age + 200L);
    }

    static int standardSpawnCount(int population, int lifetime, double simulationSpeed, int limit) {
        int remaining = Math.max(0, limit - population);
        double lifetimeFactor = Math.min(1D, 1200D / Math.max(1D, lifetime));
        double requested = Math.ceil(10D * simulationSpeed * simulationSpeed * lifetimeFactor);
        return (int) (0.6D * Math.min(remaining, requested));
    }

    static double simulationSpeed(int age, int maxAge) {
        int slowStart = Math.max(1, maxAge / 4);
        if (age > maxAge) {
            return 0D;
        }
        if (age > slowStart) {
            return 1D - (double) (age - slowStart) / (maxAge - slowStart);
        }
        return 1D;
    }

    static float parentAlpha(int age, int maxAge) {
        int fadeStart = maxAge * 3 / 4;
        if (age > fadeStart) {
            return 1F - (float) (age - fadeStart) / (maxAge - fadeStart);
        }
        return 1F;
    }

    static double heat(int age, int maxAge, float scale) {
        int maxHeat = (int) (50F * scale * scale);
        return maxHeat - Math.pow((double) maxHeat * age / maxAge, 0.6D);
    }

    static float flashDuration(float scale) {
        return Math.max(1F, scale * FLASH_BASE_DURATION);
    }

    static float flareDuration(float scale) {
        return Math.max(1F, scale * FLARE_BASE_DURATION);
    }

    private static float clamp(float value, float minimum, float maximum) {
        return Math.max(minimum, Math.min(maximum, value));
    }

    private ExtendedTorexTimeline() {
    }
}
