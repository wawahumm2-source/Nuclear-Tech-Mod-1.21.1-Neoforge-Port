package com.hbm.weapon;

import com.hbm.weapon.data.GunDefinition;
import com.hbm.weapon.state.ReloadPhase;
import com.hbm.weapon.state.WeaponSession;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ReloadControlTest {
    @Test
    void unqueuedSprintStillBlocksTriggerProcessing() {
        assertFalse(HbmWeaponService.triggerAllowed(true));
        assertTrue(HbmWeaponService.triggerAllowed(false));
        assertTrue(HbmWeaponService.triggerAllowed(true, true));
    }

    @Test
    void adsBypassesSprintFireTransitionRegardlessOfTransientSprintState() {
        assertFalse(HbmWeaponService.shouldQueueSprintFire(true, true, true));
        assertFalse(HbmWeaponService.shouldQueueSprintFire(true, true, false));
        assertTrue(HbmWeaponService.shouldQueueSprintFire(false, true, false));
        assertTrue(HbmWeaponService.shouldQueueSprintFire(false, false, true));
    }

    @Test
    void perRoundTriggerInterruptsBeforeTheNextInsertion() {
        GunDefinition.ReloadProfile reload = WeaponTestFixtures.gun("gun_spas12").getReload();
        WeaponSession session = new WeaponSession();
        session.setReload(ReloadPhase.LOOP, reload.getLoopTicks());
        session.setTriggerHeld(true);

        assertTrue(HbmWeaponService.interruptPerRoundReload(session, reload));
        assertEquals(ReloadPhase.END, session.reloadPhase());
        assertEquals(reload.getEndTicks(), session.actionTicks());
    }

    @Test
    void magazineReloadIsNotTreatedAsAShellLoopInterrupt() {
        GunDefinition.ReloadProfile reload = WeaponTestFixtures.gun("gun_star_f").getReload();
        WeaponSession session = new WeaponSession();
        session.setReload(ReloadPhase.TRANSFER, reload.getTransferTicks());
        session.setTriggerHeld(true);

        assertFalse(HbmWeaponService.interruptPerRoundReload(session, reload));
        assertEquals(ReloadPhase.TRANSFER, session.reloadPhase());
        assertEquals(reload.getTransferTicks(), session.actionTicks());
    }
}
