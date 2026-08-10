package com.hbm.world.radiation;

/** Pure treatment timer so dosage behavior remains deterministic and unit-testable. */
public final class RadiationTreatment {
    private int remainingTicks;
    private double perTick;

    public void start(int ticks, double perTick) {
        if (ticks <= 0 || perTick <= 0D) {
            return;
        }
        this.remainingTicks = Math.max(this.remainingTicks, ticks);
        this.perTick = Math.max(this.perTick, perTick);
    }

    public double tick() {
        return tick(1);
    }

    public double tick(int elapsedTicks) {
        if (this.remainingTicks <= 0 || this.perTick <= 0D) {
            return 0D;
        }
        int appliedTicks = Math.min(Math.max(0, elapsedTicks), this.remainingTicks);
        if (appliedTicks == 0) {
            return 0D;
        }
        this.remainingTicks -= appliedTicks;
        double reduction = this.perTick * appliedTicks;
        if (this.remainingTicks == 0) {
            this.perTick = 0D;
        }
        return reduction;
    }

    public int remainingTicks() {
        return remainingTicks;
    }

    public double perTick() {
        return perTick;
    }

    public void restore(int remainingTicks, double perTick) {
        this.remainingTicks = Math.max(0, remainingTicks);
        this.perTick = this.remainingTicks == 0 ? 0D : Math.max(0D, perTick);
    }

    public void clear() {
        this.remainingTicks = 0;
        this.perTick = 0D;
    }
}
