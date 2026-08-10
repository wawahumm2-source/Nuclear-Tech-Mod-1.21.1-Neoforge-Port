package com.hbm.client.explosion;

/** Bounded structural policy layered around the Tier 1 Torex motion field. */
final class NuclearCloudStructure {
    private NuclearCloudStructure() {
    }

    static double retentionScore(double radialDistance, double relativeY, double coreHeight,
            double torusWidth, double rollerSize, double alpha) {
        double safeCoreHeight = Math.max(0.001D, coreHeight);
        double safeTorusWidth = Math.max(0.001D, torusWidth);
        double safeRollerSize = Math.max(0.001D, rollerSize);
        double remaining = clamp(alpha, 0D, 1D);
        double heightRatio = relativeY / safeCoreHeight;

        double stemRadius = Math.max(1.5D, safeTorusWidth * 0.30D);
        double stemRadial = clamp(1D - radialDistance / stemRadius, 0D, 1D);
        double stemVertical = clamp(1D - Math.max(0D, heightRatio - 0.70D) / 0.30D, 0D, 1D);
        double stem = relativeY >= -safeRollerSize && heightRatio <= 1D
                ? stemRadial * stemVertical
                : 0D;

        double bridgeVerticalRadius = Math.max(safeRollerSize * 1.75D, safeCoreHeight * 0.24D);
        double bridgeVertical = clamp(
                1D - Math.abs(relativeY - safeCoreHeight * 0.86D) / bridgeVerticalRadius,
                0D,
                1D
        );
        double bridgeRadial = clamp(1D - radialDistance / (safeTorusWidth * 0.85D), 0D, 1D);
        double bridge = bridgeVertical * bridgeRadial;

        double capVertical = clamp((heightRatio - 0.68D) / 0.32D, 0D, 1D);
        double capRadial = clamp(1D - radialDistance / (safeTorusWidth + safeRollerSize * 2D), 0D, 1D);
        double cap = capVertical * capRadial;

        return remaining * (1D + stem * 4D + bridge * 3D + cap);
    }

    static Motion turbulence(double azimuth, double phase, int cloudletAge, int parentAge,
            double heightFactor) {
        double height = clamp(heightFactor, 0D, 1D);
        double firstWave = Math.sin(phase + cloudletAge * 0.055D + parentAge * 0.012D);
        double secondWave = Math.cos(phase * 0.73D + cloudletAge * 0.037D - parentAge * 0.009D);
        double tangential = (0.025D + height * 0.050D) * firstWave;
        double radial = (0.012D + (1D - height) * 0.028D) * secondWave;
        double vertical = height * 0.012D * Math.sin(phase * 1.31D + cloudletAge * 0.043D);

        double cos = Math.cos(azimuth);
        double sin = Math.sin(azimuth);
        return new Motion(
                cos * radial - sin * tangential,
                vertical,
                sin * radial + cos * tangential
        );
    }

    private static double clamp(double value, double minimum, double maximum) {
        return Math.max(minimum, Math.min(maximum, value));
    }

    record Motion(double x, double y, double z) {
    }
}
