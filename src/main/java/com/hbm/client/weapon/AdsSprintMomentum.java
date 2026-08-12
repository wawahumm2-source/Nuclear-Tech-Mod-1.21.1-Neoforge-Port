package com.hbm.client.weapon;

/**
 * Preserves the sprint pace that existed immediately before ADS. The short restore window
 * bridges the client/server tick in which the ADS movement modifier is removed; it never
 * creates more horizontal speed than the player brought into ADS.
 */
public final class AdsSprintMomentum {
    static final int RESTORE_TICKS = 3;
    static final double MAX_CAPTURE_SPEED = 0.22D;

    private double capturedSpeed;
    private int restoreTicks;

    public void capture(double horizontalSpeed) {
        capturedSpeed = Math.min(Math.max(0.0D, horizontalSpeed), MAX_CAPTURE_SPEED);
        restoreTicks = 0;
    }

    public void beginRestore() {
        restoreTicks = capturedSpeed > 0.0D ? RESTORE_TICKS : 0;
    }

    public Result tick(boolean restartAllowed, double currentHorizontalSpeed) {
        if (restoreTicks <= 0) {
            return Result.IDLE;
        }
        if (!restartAllowed) {
            cancel();
            return Result.IDLE;
        }
        restoreTicks--;
        double targetSpeed = Math.max(Math.max(0.0D, currentHorizontalSpeed), capturedSpeed);
        if (restoreTicks == 0) {
            capturedSpeed = 0.0D;
        }
        return new Result(true, targetSpeed);
    }

    public void cancel() {
        capturedSpeed = 0.0D;
        restoreTicks = 0;
    }

    public record Result(boolean restartSprint, double targetHorizontalSpeed) {
        static final Result IDLE = new Result(false, 0.0D);
    }
}
