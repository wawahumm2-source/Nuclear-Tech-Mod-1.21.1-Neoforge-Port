package com.hbm.weapon.ballistics;

import com.hbm.network.WeaponEffectType;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class BallisticsFeedbackTest {
    @Test
    void onlyConfirmedDamageProducesHitFeedback() {
        assertNull(BallisticsService.confirmationEffect(false, false, false));
        assertNull(BallisticsService.confirmationEffect(false, true, false));
        assertEquals(WeaponEffectType.HIT,
                BallisticsService.confirmationEffect(true, false, false));
        assertEquals(WeaponEffectType.HEADSHOT,
                BallisticsService.confirmationEffect(true, true, false));
        assertEquals(WeaponEffectType.KILL,
                BallisticsService.confirmationEffect(true, false, true));
        assertEquals(WeaponEffectType.HEADSHOT_KILL,
                BallisticsService.confirmationEffect(true, true, true));
    }
}
