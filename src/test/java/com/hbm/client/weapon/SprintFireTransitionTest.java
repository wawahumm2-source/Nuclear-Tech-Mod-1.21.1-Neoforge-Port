package com.hbm.client.weapon;

import com.hbm.weapon.state.WeaponSession;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SprintFireTransitionTest {
    @Test
    void settlesShootsHoldsRecoveryThenResumesSprint() {
        SprintFireTransition transition = new SprintFireTransition();
        transition.begin(WeaponSession.SPRINT_FIRE_SETTLE_TICKS,
                WeaponSession.SPRINT_FIRE_RECOVERY_TICKS);

        for (int tick = 0; tick < WeaponSession.SPRINT_FIRE_SETTLE_TICKS; tick++) {
            SprintFireTransition.Result result = transition.tick(true, true, false);
            assertTrue(result.holdHipfire());
            assertFalse(result.predictShot());
            assertFalse(result.resumeSprint());
        }

        SprintFireTransition.Result shot = transition.tick(true, true, false);
        assertTrue(shot.holdHipfire());
        assertTrue(shot.predictShot());
        assertFalse(shot.resumeSprint());

        for (int tick = 0; tick < WeaponSession.SPRINT_FIRE_RECOVERY_TICKS; tick++) {
            SprintFireTransition.Result recovery = transition.tick(true, true, false);
            assertTrue(recovery.holdHipfire());
            assertFalse(recovery.predictShot());
            assertFalse(recovery.resumeSprint());
        }

        SprintFireTransition.Result resume = transition.tick(true, true, false);
        assertFalse(resume.holdHipfire());
        assertFalse(resume.predictShot());
        assertTrue(resume.resumeSprint());
        assertFalse(transition.active());
    }

    @Test
    void adsAllowsTheShotButPreventsSprintResumption() {
        SprintFireTransition transition = new SprintFireTransition();
        transition.begin(0, 0);

        SprintFireTransition.Result shot = transition.tick(true, true, true);

        assertTrue(shot.predictShot());
        assertFalse(shot.resumeSprint());
        assertFalse(transition.tick(true, true, true).resumeSprint());
    }

    @Test
    void serverAcknowledgementPreventsDuplicatePredictionWithoutBlockingResume() {
        SprintFireTransition transition = new SprintFireTransition();
        transition.begin(0, 0);
        transition.acknowledgeAttempt();

        SprintFireTransition.Result shot = transition.tick(true, true, false);

        assertFalse(shot.predictShot());
        assertFalse(shot.resumeSprint());
        assertTrue(transition.tick(true, true, false).resumeSprint());
    }

    @Test
    void releasedSprintKeyOrLostForwardMotionKeepsPlayerWalking() {
        SprintFireTransition noKey = new SprintFireTransition();
        noKey.begin(0, 0);
        noKey.tick(false, true, false);
        assertFalse(noKey.tick(false, true, false).resumeSprint());

        SprintFireTransition noForward = new SprintFireTransition();
        noForward.begin(0, 0);
        noForward.tick(true, false, false);
        assertFalse(noForward.tick(true, false, false).resumeSprint());
    }
}
