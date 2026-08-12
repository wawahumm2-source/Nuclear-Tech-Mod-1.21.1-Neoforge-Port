package com.hbm.client.weapon;

/** Client-side presentation timing for the server-authoritative sprint-to-fire transition. */
public final class SprintFireTransition {
    public static final Result IDLE = new Result(false, false, false);

    private boolean active;
    private int delayTicks;
    private int recoveryTicks;
    private boolean attemptTriggered;
    private boolean attemptAcknowledged;

    public void begin(int delayTicks, int recoveryTicks) {
        active = true;
        this.delayTicks = Math.max(0, delayTicks);
        this.recoveryTicks = Math.max(0, recoveryTicks);
        attemptTriggered = false;
        attemptAcknowledged = false;
    }

    public boolean active() {
        return active;
    }

    public Result tick(boolean sprintKeyHeld, boolean movingForward, boolean adsHeld) {
        if (!active) {
            return IDLE;
        }
        if (delayTicks > 0) {
            delayTicks--;
            return new Result(true, false, false);
        }
        if (!attemptTriggered) {
            attemptTriggered = true;
            return new Result(true, !attemptAcknowledged, false);
        }
        if (recoveryTicks > 0) {
            recoveryTicks--;
            return new Result(true, false, false);
        }
        active = false;
        return new Result(false, false,
                sprintKeyHeld && movingForward && !adsHeld);
    }

    /** Prevents duplicate local recoil when the integrated/server tick wins the race. */
    public void acknowledgeAttempt() {
        if (active) {
            attemptAcknowledged = true;
        }
    }

    public void cancel() {
        active = false;
        delayTicks = 0;
        recoveryTicks = 0;
        attemptTriggered = false;
        attemptAcknowledged = false;
    }

    public record Result(boolean holdHipfire, boolean predictShot, boolean resumeSprint) {
    }
}
