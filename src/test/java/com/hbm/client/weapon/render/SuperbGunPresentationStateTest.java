package com.hbm.client.weapon.render;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SuperbGunPresentationStateTest {
    @Test
    void movementCadenceStaysReadableAtWalkAndSprint() {
        assertEquals(0.10F, SuperbGunPresentationState.movementPhaseSpeed(0.0F), 1.0E-6F);
        assertEquals(0.12F, SuperbGunPresentationState.movementPhaseSpeed(0.5F), 1.0E-6F);
        assertEquals(0.14F, SuperbGunPresentationState.movementPhaseSpeed(1.0F), 1.0E-6F);
    }

    @Test
    void movementCadenceClampsInvalidBlendValues() {
        assertEquals(0.10F, SuperbGunPresentationState.movementPhaseSpeed(-1.0F), 1.0E-6F);
        assertEquals(0.14F, SuperbGunPresentationState.movementPhaseSpeed(2.0F), 1.0E-6F);
    }

    @Test
    void sprintReleaseReturnsToHipFasterThanSprintEntry() {
        assertEquals(0.16F, SuperbGunPresentationState.sprintBlendStep(true), 1.0E-6F);
        assertEquals(0.34F, SuperbGunPresentationState.sprintBlendStep(false), 1.0E-6F);
        assertTrue(SuperbGunPresentationState.sprintBlendStep(false)
                > SuperbGunPresentationState.sprintBlendStep(true) * 2.0F);
    }

    @Test
    void reticleBloomRecoversWithThePrimaryWeaponRecoil() {
        assertEquals(0.0F, SuperbGunPresentationState.fireBloomAtTime(0.0F), 1.0E-6F);
        assertTrue(SuperbGunPresentationState.fireBloomAtTime(0.34F) > 0.90F);
        assertTrue(SuperbGunPresentationState.fireBloomAtTime(1.0F) > 0.45F);
        assertTrue(SuperbGunPresentationState.fireBloomAtTime(1.4F) < 0.06F);
        assertEquals(0.0F, SuperbGunPresentationState.fireBloomAtTime(3.0F), 1.0E-6F);
    }
}
