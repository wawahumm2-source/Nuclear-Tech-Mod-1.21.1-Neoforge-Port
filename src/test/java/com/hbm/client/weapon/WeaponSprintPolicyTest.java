package com.hbm.client.weapon;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class WeaponSprintPolicyTest {
    @Test
    void adsNeverUsesTheSprintFireDelayEvenWhenCtrlReassertsSprint() {
        assertFalse(WeaponSprintPolicy.shouldUseSprintFireTransition(true, true));
        assertTrue(WeaponSprintPolicy.shouldUseSprintFireTransition(true, false));
        assertFalse(WeaponSprintPolicy.shouldUseSprintFireTransition(false, false));
    }

    @Test
    void releasingAdsRestartsStillHeldSprintIntent() {
        assertTrue(WeaponSprintPolicy.shouldRestartAfterAds(
                true, true, true, true, false, false));
    }

    @Test
    void adsRestartRequiresCtrlForwardAndAnUnblockedWeapon() {
        assertFalse(WeaponSprintPolicy.shouldRestartAfterAds(
                false, true, true, true, false, false));
        assertFalse(WeaponSprintPolicy.shouldRestartAfterAds(
                true, true, false, true, false, false));
        assertFalse(WeaponSprintPolicy.shouldRestartAfterAds(
                true, true, true, false, false, false));
        assertFalse(WeaponSprintPolicy.shouldRestartAfterAds(
                true, false, true, true, false, false));
        assertFalse(WeaponSprintPolicy.shouldRestartAfterAds(
                true, true, true, true, true, false));
        assertFalse(WeaponSprintPolicy.shouldRestartAfterAds(
                true, true, true, true, false, true));
    }
}
