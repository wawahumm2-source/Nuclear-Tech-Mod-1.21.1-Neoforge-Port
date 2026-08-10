package com.hbm.weapon.state;

/** Client-side camera recoil debt with deterministic, bounded per-tick recovery. */
public final class RecoilAccumulator {
    private float pitchDebt;
    private float yawDebt;

    public void add(float pitch, float yaw) {
        pitchDebt += Math.max(0.0F, pitch);
        yawDebt += yaw;
    }

    public Recovery recover(float recoveryPerTick) {
        float recovery = Math.max(0.0F, recoveryPerTick);
        float pitch = Math.min(pitchDebt, recovery);
        float yaw = Math.copySign(Math.min(Math.abs(yawDebt), recovery * 0.65F), yawDebt);
        pitchDebt -= pitch;
        yawDebt -= yaw;
        return new Recovery(pitch, yaw);
    }

    public float pitchDebt() {
        return pitchDebt;
    }

    public float yawDebt() {
        return yawDebt;
    }

    public record Recovery(float pitch, float yaw) {
    }
}
