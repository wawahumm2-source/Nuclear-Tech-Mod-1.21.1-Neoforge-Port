package com.hbm.client.explosion;

/** Finite-value port of the Tier 1 Torex two-dimensional convection field. */
final class NuclearCloudMotion {
    private static final double EPSILON = 1.0E-5D;

    static Motion convection(double radialDistance, double relativeY, double azimuth,
            double torusWidth, double coreHeight, double rollerSize, double rangeModifier) {
        return toroidal(radialDistance, relativeY, azimuth, torusWidth, coreHeight,
                rollerSize * rangeModifier);
    }

    static Motion ring(double radialDistance, double relativeY, double azimuth,
            double torusWidth, double coreHeight, double rollerSize, double rangeModifier) {
        if (radialDistance > torusWidth * 2D) {
            return Motion.ZERO;
        }
        return toroidal(radialDistance, relativeY, azimuth, torusWidth, coreHeight * 0.5D,
                rollerSize * rangeModifier * 0.25D);
    }

    private static Motion toroidal(double radialDistance, double relativeY, double azimuth,
            double torusWidth, double centerHeight, double desiredRadius) {
        double safeRadius = Math.max(EPSILON, desiredRadius);
        double deltaX = torusWidth - radialDistance;
        double deltaY = centerHeight - relativeY;
        double normalizedDistance = Math.hypot(deltaX, deltaY) / safeRadius - 1D;
        double safeDistance = signedEpsilon(normalizedDistance);

        // Tier 1 bends a vector around the torus with this Euler-shaped angle.
        double turn = (1D - Math.exp(-safeDistance)) * Math.PI * 0.5D;
        double sourceX = -deltaX / safeDistance;
        double sourceY = -deltaY / safeDistance;
        double cosTurn = Math.cos(turn);
        double sinTurn = Math.sin(turn);
        double rotatedX = sourceX * cosTurn + sourceY * sinTurn;
        double rotatedY = sourceY * cosTurn - sourceX * sinTurn;

        double planeX = torusWidth + rotatedX - radialDistance;
        double planeY = centerHeight + rotatedY - relativeY;
        double length = Math.hypot(planeX, planeY);
        if (!Double.isFinite(length) || length < EPSILON) {
            return Motion.ZERO;
        }

        planeX /= length;
        planeY /= length;
        double worldX = Math.cos(azimuth) * planeX;
        double worldZ = Math.sin(azimuth) * planeX;
        if (!Double.isFinite(worldX) || !Double.isFinite(planeY) || !Double.isFinite(worldZ)) {
            return Motion.ZERO;
        }
        return new Motion(worldX, planeY, worldZ);
    }

    private static double signedEpsilon(double value) {
        if (Math.abs(value) >= EPSILON) {
            return value;
        }
        return Math.copySign(EPSILON, value == 0D ? 1D : value);
    }

    record Motion(double x, double y, double z) {
        static final Motion ZERO = new Motion(0D, 0D, 0D);
    }

    private NuclearCloudMotion() {
    }
}
