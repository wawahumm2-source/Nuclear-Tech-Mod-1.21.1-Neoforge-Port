package com.hbm.client.explosion;

/** Tier 1 Torex heat/distance color equation with finite-value guards. */
final class NuclearCloudPalette {
    private static final double SOURCE_MAX_HEAT = 75D;

    static Rgb colorAt(int age, int sourceLifetimeAge, double distance, boolean brightCollar) {
        int safeSourceLifetime = Math.max(1, sourceLifetimeAge);
        double sourceProgress = Math.max(0D, Math.min(1D, (double) age / safeSourceLifetime));
        double heat = SOURCE_MAX_HEAT * (1D - sourceProgress);
        double thermalDistance = heat <= 1.0E-6D
                ? Double.POSITIVE_INFINITY
                : Math.max(Math.max(0D, distance) / Math.sqrt(heat), 1D);
        double colorStrength = Double.isFinite(thermalDistance) ? 2D / thermalDistance : 0D;

        double greying = 1D;
        double greyingStart = safeSourceLifetime * 0.75D;
        if (age > greyingStart) {
            greying += Math.min(1D, (age - greyingStart)
                    / Math.max(1D, safeSourceLifetime - greyingStart));
        }
        if (brightCollar) {
            greying += 1D;
        }

        return new Rgb(
                (float) (Math.max(colorStrength * 2D, 0.25D) * greying),
                (float) (Math.max(colorStrength * 1.5D, 0.25D) * greying),
                (float) (Math.max(colorStrength * 0.5D, 0.25D) * greying)
        );
    }

    record Rgb(float red, float green, float blue) {
    }

    private NuclearCloudPalette() {
    }
}
