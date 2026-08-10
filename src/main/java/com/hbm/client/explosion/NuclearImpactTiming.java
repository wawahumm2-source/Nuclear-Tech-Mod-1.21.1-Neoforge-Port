package com.hbm.client.explosion;

/** Pure timing curves for the cinematic client-only nuclear impact presentation. */
final class NuclearImpactTiming {
    static final int FLASH_RAMP_TICKS = 1;
    static final int FLASH_HARD_WHITE_TICKS = 8;
    static final int FLASH_HOLD_TICKS = 40;
    static final int FLASH_FADE_TICKS = 60;
    static final int FLASH_DURATION_TICKS = FLASH_RAMP_TICKS + FLASH_HOLD_TICKS + FLASH_FADE_TICKS;

    static final int SHAKE_IMPACT_TICKS = 4;
    static final int SHAKE_RUMBLE_TICKS = 26;
    static final int SHAKE_DURATION_TICKS = SHAKE_IMPACT_TICKS + SHAKE_RUMBLE_TICKS;

    static float flashEnvelope(float age) {
        return flashEnvelope(age, FLASH_HOLD_TICKS, FLASH_FADE_TICKS);
    }

    static float flashEnvelope(float age, int holdTicks, int fadeTicks) {
        int safeHoldTicks = Math.max(0, holdTicks);
        int safeFadeTicks = Math.max(1, fadeTicks);
        int durationTicks = FLASH_RAMP_TICKS + safeHoldTicks + safeFadeTicks;
        if (age < 0F || age >= durationTicks) {
            return 0F;
        }
        if (age < FLASH_RAMP_TICKS) {
            return clamp((age + 1F) / FLASH_RAMP_TICKS);
        }
        float fadeStart = FLASH_RAMP_TICKS + safeHoldTicks;
        float hardWhiteEnd = Math.min(fadeStart, FLASH_RAMP_TICKS + FLASH_HARD_WHITE_TICKS);
        if (age < hardWhiteEnd) {
            return 1F;
        }
        if (age < fadeStart) {
            float decay = (age - hardWhiteEnd) / Math.max(1F, fadeStart - hardWhiteEnd);
            return 1F + (0.38F - 1F) * smooth(decay);
        }
        return 0.38F * clamp(1F - (age - fadeStart) / safeFadeTicks);
    }

    static int flashDurationTicks(int holdTicks, int fadeTicks) {
        return FLASH_RAMP_TICKS + Math.max(0, holdTicks) + Math.max(1, fadeTicks);
    }

    static float shakeEnvelope(float age) {
        if (age < 0F || age >= SHAKE_DURATION_TICKS) {
            return 0F;
        }
        float impact = clamp(1F - age / SHAKE_IMPACT_TICKS);
        float rumble = 0.55F * clamp(1F - Math.max(0F, age - SHAKE_IMPACT_TICKS) / SHAKE_RUMBLE_TICKS);
        return Math.max(impact, rumble);
    }

    static ShakeSample shakeSample(float age) {
        float envelope = shakeEnvelope(age);
        return new ShakeSample(
                (float) Math.sin(age * 1.73F + 0.35F) * 0.12F * envelope,
                (float) Math.sin(age * 2.41F + 1.10F) * 0.10F * envelope,
                (float) Math.sin(age * 1.31F + 2.20F) * 0.24F * envelope,
                (float) Math.sin(age * 1.87F + 0.70F) * 0.70F * envelope,
                (float) Math.sin(age * 2.23F + 1.90F) * 0.50F * envelope,
                (float) Math.sin(age * 1.47F + 2.70F) * 0.28F * envelope
        );
    }

    private static float smooth(float value) {
        float clamped = clamp(value);
        return clamped * clamped * (3F - 2F * clamped);
    }

    private static float clamp(float value) {
        return Math.max(0F, Math.min(1F, value));
    }

    record ShakeSample(float forward, float vertical, float lateral, float yaw, float pitch, float roll) {
    }

    private NuclearImpactTiming() {
    }
}
