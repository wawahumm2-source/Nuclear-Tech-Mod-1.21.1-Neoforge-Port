package com.hbm.client.weapon;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AdsSprintMomentumTest {
    @Test
    void restoresCapturedSprintPaceAcrossTheAdsReleaseWindow() {
        AdsSprintMomentum momentum = new AdsSprintMomentum();
        momentum.capture(0.13D);
        momentum.beginRestore();

        for (int tick = 0; tick < AdsSprintMomentum.RESTORE_TICKS; tick++) {
            AdsSprintMomentum.Result result = momentum.tick(true, 0.075D);
            assertTrue(result.restartSprint());
            assertEquals(0.13D, result.targetHorizontalSpeed(), 1.0E-9D);
        }
        assertFalse(momentum.tick(true, 0.075D).restartSprint());
    }

    @Test
    void neverStacksSpeedAboveTheCapturedPace() {
        AdsSprintMomentum momentum = new AdsSprintMomentum();
        momentum.capture(0.13D);
        momentum.beginRestore();

        AdsSprintMomentum.Result result = momentum.tick(true, 0.16D);
        assertEquals(0.16D, result.targetHorizontalSpeed(), 1.0E-9D);
    }

    @Test
    void standingAdsCannotCreateMomentumAndInterruptedRestartIsCancelled() {
        AdsSprintMomentum standing = new AdsSprintMomentum();
        standing.capture(0.0D);
        standing.beginRestore();
        assertFalse(standing.tick(true, 0.0D).restartSprint());

        AdsSprintMomentum interrupted = new AdsSprintMomentum();
        interrupted.capture(0.13D);
        interrupted.beginRestore();
        assertFalse(interrupted.tick(false, 0.075D).restartSprint());
        assertFalse(interrupted.tick(true, 0.075D).restartSprint());
    }

    @Test
    void captureIsBoundedToNormalGroundSprintScale() {
        AdsSprintMomentum momentum = new AdsSprintMomentum();
        momentum.capture(3.0D);
        momentum.beginRestore();

        assertEquals(AdsSprintMomentum.MAX_CAPTURE_SPEED,
                momentum.tick(true, 0.0D).targetHorizontalSpeed(), 1.0E-9D);
    }
}
