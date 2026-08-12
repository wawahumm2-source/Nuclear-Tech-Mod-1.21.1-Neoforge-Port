package com.hbm.weapon.ballistics;

import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

/** Shared server trajectory and client HUD projection for a weapon's iron-sight zero. */
public final class WeaponAim {
    public static Vec3 direction(float playerPitchDegrees, float playerYawDegrees,
                                 double zeroPitchDegrees) {
        double pitch = Math.toRadians(playerPitchDegrees + zeroPitchDegrees);
        double yaw = Math.toRadians(playerYawDegrees);
        double horizontal = Math.cos(pitch);
        return new Vec3(
                -Math.sin(yaw) * horizontal,
                -Math.sin(pitch),
                Math.cos(yaw) * horizontal
        ).normalize();
    }

    /**
     * Solves an ADS launch direction that intersects the visual sight line at the requested
     * zero distance using the same per-tick gravity and drag order as {@link BallisticsService}.
     * A zero distance of zero retains the direct sight ray.
     */
    public static Vec3 zeroedDirection(float playerPitchDegrees, float playerYawDegrees,
                                       double sightPitchDegrees, double zeroDistance,
                                       double muzzleVelocity, double gravity, double drag) {
        if (zeroDistance <= 0.0D || muzzleVelocity <= 0.0D) {
            return direction(playerPitchDegrees, playerYawDegrees, sightPitchDegrees);
        }
        double sightPitch = playerPitchDegrees + sightPitchDegrees;
        double sightRadians = Math.toRadians(sightPitch);
        double targetHorizontal = Math.cos(sightRadians) * zeroDistance;
        double targetY = -Math.sin(sightRadians) * zeroDistance;

        double low = sightPitch - 12.0D;
        double high = sightPitch + 12.0D;
        for (int iteration = 0; iteration < 48; iteration++) {
            double candidate = (low + high) * 0.5D;
            double candidateY = verticalAtHorizontal(
                    candidate, targetHorizontal, muzzleVelocity, gravity, drag);
            // Minecraft pitch increases downward, so increasing the candidate lowers impact.
            if (candidateY > targetY) {
                low = candidate;
            } else {
                high = candidate;
            }
        }
        double launchPitch = (low + high) * 0.5D;
        return direction((float) launchPitch, playerYawDegrees, 0.0D);
    }

    private static double verticalAtHorizontal(double pitchDegrees, double targetHorizontal,
                                               double speed, double gravity, double drag) {
        double pitch = Math.toRadians(pitchDegrees);
        double horizontalVelocity = Math.max(1.0E-6D, Math.cos(pitch) * speed);
        double verticalVelocity = -Math.sin(pitch) * speed;
        double horizontal = 0.0D;
        double vertical = 0.0D;
        double retention = 1.0D - Mth.clamp(drag, 0.0D, 0.999D);
        for (int tick = 0; tick < 4096 && horizontal < targetHorizontal; tick++) {
            double step = Math.min(1.0D,
                    (targetHorizontal - horizontal) / horizontalVelocity);
            horizontal += horizontalVelocity * step;
            vertical += verticalVelocity * step;
            if (step < 1.0D) {
                break;
            }
            horizontalVelocity *= retention;
            verticalVelocity = verticalVelocity * retention - gravity;
        }
        return vertical;
    }

    /** Converts an angular zero into the equivalent GUI-space displacement below screen centre. */
    public static int screenOffsetY(double zeroPitchDegrees, double verticalFovDegrees,
                                    int guiHeight) {
        if (guiHeight <= 0 || Math.abs(zeroPitchDegrees) < 1.0E-6D) {
            return 0;
        }
        double clampedFov = Mth.clamp(verticalFovDegrees, 10.0D, 170.0D);
        double halfHeight = guiHeight * 0.5D;
        return (int) Math.round(Math.tan(Math.toRadians(zeroPitchDegrees))
                / Math.tan(Math.toRadians(clampedFov * 0.5D)) * halfHeight);
    }

    private WeaponAim() {
    }
}
