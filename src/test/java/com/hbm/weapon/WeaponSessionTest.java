package com.hbm.weapon;

import com.hbm.weapon.state.ReloadPhase;
import com.hbm.weapon.state.WeaponSession;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class WeaponSessionTest {
    @Test
    void rejectsStaleSequencesAndRateLimitsPacketsPerTick() {
        WeaponSession session = new WeaponSession();
        assertTrue(session.acknowledge(1));
        assertFalse(session.acknowledge(1));
        assertFalse(session.acknowledge(0));
        for (int i = 0; i < 8; i++) {
            assertTrue(session.allowPacket(20));
        }
        assertFalse(session.allowPacket(20));
        assertTrue(session.allowPacket(21));
    }

    @Test
    void weaponSwapCancelsTransientActionsWithoutResettingSequence() {
        WeaponSession session = new WeaponSession();
        session.acknowledge(17);
        session.bind(UUID.randomUUID());
        session.setTriggerHeld(true);
        session.setAdsHeld(true);
        session.setReload(ReloadPhase.LOOP, 12);

        assertTrue(session.bind(UUID.randomUUID()));
        assertFalse(session.triggerHeld());
        assertFalse(session.adsHeld());
        assertEquals(ReloadPhase.IDLE, session.reloadPhase());
        assertEquals(17, session.acknowledgedSequence());
    }

    @Test
    void cooldownAndSemiTriggerAreDeterministic() {
        WeaponSession session = new WeaponSession();
        session.setTriggerHeld(true);
        assertTrue(session.consumeSemiQueued());
        assertFalse(session.consumeSemiQueued());
        session.addCooldown(2.5D);
        session.tickCooldown();
        assertEquals(1.5D, session.cooldownTicks());
        session.tickCooldown();
        session.tickCooldown();
        assertEquals(-0.5D, session.cooldownTicks());
    }

    @Test
    void fractionalCooldownPreservesRequestedAutomaticRate() {
        WeaponSession session = new WeaponSession();
        session.setTriggerHeld(true);
        int shots = 0;
        double interval = 1200.0D / 650.0D;

        for (int tick = 0; tick < 1200; tick++) {
            session.tickCooldown();
            if (session.cooldownTicks() <= 0.0D) {
                shots++;
                session.addCooldown(interval);
            }
        }

        assertEquals(650, shots);
    }

    @Test
    void sprintFireWaitsThreeFullTicksAndSurvivesTriggerRelease() {
        WeaponSession session = new WeaponSession();
        session.setTriggerHeld(true);
        session.queueSprintFire(WeaponSession.SPRINT_FIRE_SETTLE_TICKS);
        session.setTriggerHeld(false);

        assertTrue(session.sprintFirePending());
        assertTrue(session.holdSprintFireDelay());
        assertTrue(session.holdSprintFireDelay());
        assertTrue(session.holdSprintFireDelay());
        assertFalse(session.holdSprintFireDelay());
        assertTrue(session.consumeSemiQueued());

        session.completeSprintFire(WeaponSession.SPRINT_FIRE_RECOVERY_TICKS);
        assertFalse(session.sprintFirePending());
        assertFalse(session.triggerHeld());
        assertFalse(session.consumeSemiQueued());
        for (int tick = 0; tick < WeaponSession.SPRINT_FIRE_RECOVERY_TICKS; tick++) {
            assertTrue(session.holdSprintFireRecovery());
        }
        assertFalse(session.holdSprintFireRecovery());
    }

    @Test
    void weaponSwapCancelsQueuedSprintFire() {
        WeaponSession session = new WeaponSession();
        session.bind(UUID.randomUUID());
        session.setTriggerHeld(true);
        session.queueSprintFire(WeaponSession.SPRINT_FIRE_SETTLE_TICKS);

        session.bind(UUID.randomUUID());

        assertFalse(session.sprintFirePending());
        assertFalse(session.triggerHeld());
        assertFalse(session.consumeSemiQueued());
    }
}
