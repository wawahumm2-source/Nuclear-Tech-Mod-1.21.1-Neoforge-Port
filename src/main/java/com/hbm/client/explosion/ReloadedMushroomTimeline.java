package com.hbm.client.explosion;

/** Pure timing and scale rules from Reloaded 1.12.2's RenderSmallNukeMK4. */
final class ReloadedMushroomTimeline {
    private static final float SOURCE_TEXTURE_SCALE = 15129F;
    private static final int[] TEXTURE_THRESHOLDS = {
            100, 140, 200, 300, 460, 720, 1140, 1820, 2920, 4700
    };

    static int textureStage(float age, float sourceRadius) {
        float radius = Math.max(0F, sourceRadius);
        float sizeFactor = radius * radius / SOURCE_TEXTURE_SCALE;
        for (int stage = 0; stage < TEXTURE_THRESHOLDS.length; stage++) {
            if (age < TEXTURE_THRESHOLDS[stage] * sizeFactor) {
                return stage;
            }
        }
        return 10;
    }

    static float modelScale(float sourceRadius) {
        return Math.max(0F, sourceRadius) * 0.025F;
    }

    static float headWidth(float age) {
        return 0.7F + clamp(age, 0F, 100F) / 100F * 0.3F;
    }

    static float textureScroll(float age, int maxAge) {
        float percentageAge = clamp(age / Math.max(1F, maxAge), 0F, 1F);
        double riseSpeed = 0.014D * Math.pow(0.02D, percentageAge) + 0.005D;
        return (float) (Math.max(0F, age) * riseSpeed);
    }

    static float alpha(float age, int fadeStartAge, int maxAge) {
        if (age <= fadeStartAge) {
            return 1F;
        }
        return clamp(1F - (age - fadeStartAge) / Math.max(1F, maxAge - fadeStartAge), 0F, 1F);
    }

    private static float clamp(float value, float minimum, float maximum) {
        return Math.max(minimum, Math.min(maximum, value));
    }

    private ReloadedMushroomTimeline() {
    }
}
