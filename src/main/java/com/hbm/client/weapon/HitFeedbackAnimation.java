package com.hbm.client.weapon;

import net.minecraft.util.Mth;

/** Smooth, severity-aware HUD confirmation driven only by server-confirmed entity damage. */
final class HitFeedbackAnimation {
    enum Kind {
        NONE(0, 0),
        HIT(1, 8),
        HEADSHOT(2, 11),
        KILL(3, 15),
        HEADSHOT_KILL(4, 15);

        private final int priority;
        private final int durationTicks;

        Kind(int priority, int durationTicks) {
            this.priority = priority;
            this.durationTicks = durationTicks;
        }
    }

    record Frame(Kind kind, float alpha, float expansion, int armLength) {
    }

    private Kind kind = Kind.NONE;
    private int remainingTicks;
    private int durationTicks;

    void start(Kind next) {
        if (next == Kind.NONE) {
            return;
        }
        if (remainingTicks <= 0 || next.priority >= kind.priority) {
            kind = next;
            durationTicks = next.durationTicks;
            remainingTicks = durationTicks;
        }
    }

    void tick() {
        if (remainingTicks > 0 && --remainingTicks == 0) {
            kind = Kind.NONE;
            durationTicks = 0;
        }
    }

    Frame sample(float partialTick) {
        if (remainingTicks <= 0 || durationTicks <= 0) {
            return new Frame(Kind.NONE, 0.0F, 0.0F, 0);
        }
        float elapsed = durationTicks - Math.max(0.0F,
                remainingTicks - Mth.clamp(partialTick, 0.0F, 1.0F));
        float progress = Mth.clamp(elapsed / durationTicks, 0.0F, 1.0F);
        float eased = 1.0F - (float) Math.pow(1.0F - progress, 3.0D);
        float alpha = (float) Math.pow(1.0F - progress, 1.35D);
        int length = switch (kind) {
            case HIT -> 4;
            case HEADSHOT -> 5;
            case KILL -> 6;
            case HEADSHOT_KILL -> 6;
            case NONE -> 0;
        };
        return new Frame(kind, alpha, eased, length);
    }
}
