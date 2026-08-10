package com.hbm.weapon.state;

import java.util.UUID;

/** Server-only state for the weapon currently controlled by a player. */
public final class WeaponSession {
    private static final int MAX_PACKETS_PER_TICK = 8;

    private boolean triggerHeld;
    private boolean adsHeld;
    private boolean semiQueued;
    private double cooldownTicks;
    private ReloadPhase reloadPhase = ReloadPhase.IDLE;
    private int actionTicks;
    private int acknowledgedSequence = -1;
    private UUID heldStackIdentity;
    private int burstRemaining;
    private long packetTick = Long.MIN_VALUE;
    private int packetsThisTick;

    public boolean triggerHeld() {
        return triggerHeld;
    }

    public void setTriggerHeld(boolean triggerHeld) {
        if (triggerHeld && !this.triggerHeld) {
            this.semiQueued = true;
            // Do not carry idle negative time into a new trigger pull. During sustained fire,
            // sub-tick residuals remain intact so fractional RPM is averaged correctly.
            cooldownTicks = Math.max(0.0D, cooldownTicks);
        }
        this.triggerHeld = triggerHeld;
    }

    public boolean adsHeld() {
        return adsHeld;
    }

    public void setAdsHeld(boolean adsHeld) {
        this.adsHeld = adsHeld;
    }

    public boolean consumeSemiQueued() {
        boolean queued = semiQueued;
        semiQueued = false;
        return queued;
    }

    public double cooldownTicks() {
        return cooldownTicks;
    }

    public void tickCooldown() {
        cooldownTicks = Math.max(-1.0D, cooldownTicks - 1.0D);
    }

    public void addCooldown(double ticks) {
        if (!Double.isFinite(ticks) || ticks < 0.0D) {
            throw new IllegalArgumentException("Cooldown must be a finite non-negative value");
        }
        cooldownTicks += ticks;
    }

    public ReloadPhase reloadPhase() {
        return reloadPhase;
    }

    public void setReload(ReloadPhase phase, int ticks) {
        reloadPhase = phase;
        actionTicks = Math.max(0, ticks);
    }

    public int actionTicks() {
        return actionTicks;
    }

    public boolean tickAction() {
        if (actionTicks > 0) {
            actionTicks--;
        }
        return actionTicks == 0;
    }

    public int acknowledgedSequence() {
        return acknowledgedSequence;
    }

    public boolean acknowledge(int sequence) {
        if (sequence <= acknowledgedSequence) {
            return false;
        }
        acknowledgedSequence = sequence;
        return true;
    }

    public UUID heldStackIdentity() {
        return heldStackIdentity;
    }

    public boolean bind(UUID identity) {
        if (!identity.equals(heldStackIdentity)) {
            cancelActions();
            heldStackIdentity = identity;
            return true;
        }
        return false;
    }

    public boolean allowPacket(long gameTime) {
        if (packetTick != gameTime) {
            packetTick = gameTime;
            packetsThisTick = 0;
        }
        if (packetsThisTick >= MAX_PACKETS_PER_TICK) {
            return false;
        }
        packetsThisTick++;
        return true;
    }

    public int burstRemaining() {
        return burstRemaining;
    }

    public void beginBurst(int count) {
        burstRemaining = Math.max(0, count);
    }

    public void consumeBurstRound() {
        burstRemaining = Math.max(0, burstRemaining - 1);
    }

    public void cancelActions() {
        triggerHeld = false;
        adsHeld = false;
        semiQueued = false;
        reloadPhase = ReloadPhase.IDLE;
        actionTicks = 0;
        burstRemaining = 0;
    }

    public void reset() {
        cancelActions();
        cooldownTicks = 0.0D;
        heldStackIdentity = null;
    }
}
