package com.hbm.client.explosion;

/** Pure timing rules for source-speed formation and Reloaded 1.12.2 cloud persistence. */
final class NuclearCloudTiming {
    static final double PERSISTENT_FORMATION_TAIL_SPEED = 0.035D;
    static final double PERSISTENT_END_SPEED = 0.008D;

    static int totalLifetimeTicks(int formationTicks, float sourceRadius) {
        double radius = Math.max(0D, sourceRadius);
        int reloadedTicks = (int) Math.max(300D, 0.55D * Math.pow(radius + 16D, 2D));
        return Math.max(Math.max(1, formationTicks), reloadedTicks);
    }

    static int fadeStartTick(int totalTicks) {
        int safeTotal = Math.max(1, totalTicks);
        int fadeTicks = Math.max(20, Math.round(safeTotal * 0.10F));
        return Math.max(0, safeTotal - fadeTicks);
    }

    static float aftermathProgress(int age, int sourceLifetimeTicks, int totalTicks) {
        if (age <= sourceLifetimeTicks || totalTicks <= sourceLifetimeTicks) {
            return 0F;
        }
        float raw = (float) (age - sourceLifetimeTicks) / (totalTicks - sourceLifetimeTicks);
        float clamped = Math.max(0F, Math.min(1F, raw));
        return clamped * clamped * (3F - 2F * clamped);
    }

    /**
     * Keeps an extended cloud gently convecting after source-speed formation.
     * Tier 1 stops at half-life; Waldemar instead decays motion through the
     * entity lifetime. Persistent port clouds use that later behavior at a
     * deliberately restrained tail speed so the radius-scaled cloud never freezes.
     */
    static double simulationSpeed(int age, int formationTicks, int totalTicks) {
        int formation = Math.max(1, formationTicks);
        int total = Math.max(formation, totalTicks);
        int life = Math.max(0, age);
        int slowStart = Math.max(1, formation / 4);

        if (total <= formation) {
            int stop = Math.max(slowStart + 1, formation / 2);
            if (life >= stop) {
                return 0D;
            }
            if (life <= slowStart) {
                return 1D;
            }
            return 1D - (double) (life - slowStart) / (stop - slowStart);
        }

        if (life <= slowStart) {
            return 1D;
        }
        if (life <= formation) {
            double progress = (double) (life - slowStart) / Math.max(1, formation - slowStart);
            return lerp(1D, PERSISTENT_FORMATION_TAIL_SPEED, progress);
        }

        double progress = (double) (Math.min(life, total) - formation) / Math.max(1, total - formation);
        return lerp(PERSISTENT_FORMATION_TAIL_SPEED, PERSISTENT_END_SPEED, progress);
    }

    static int shockLifetimeTicks(int age) {
        return Math.max(300 - Math.max(0, age) * 20, 50);
    }

    static double shockRadius(int age, double radialJitter) {
        return (Math.max(0, age) * 1.5D + Math.max(0D, Math.min(1D, radialJitter))) * 1.5D;
    }

    static double shockMotionMultiplier(int age) {
        return age > 15 ? 0.75D : 0D;
    }

    private static double lerp(double start, double end, double progress) {
        double clamped = Math.max(0D, Math.min(1D, progress));
        return start + (end - start) * clamped;
    }

    private NuclearCloudTiming() {
    }
}
