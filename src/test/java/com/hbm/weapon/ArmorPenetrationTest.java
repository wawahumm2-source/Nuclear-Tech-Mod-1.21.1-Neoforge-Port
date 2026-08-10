package com.hbm.weapon;

import com.hbm.world.damage.HbmGunDamageSource;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ArmorPenetrationTest {
    @Test
    void penetrationReducesOnlyTheArmorReduction() {
        assertEquals(6.0F, HbmGunDamageSource.applyPenetration(10.0F, 0.4F), 1.0E-6F);
        assertEquals(0.0F, HbmGunDamageSource.applyPenetration(10.0F, 1.0F), 1.0E-6F);
        assertEquals(10.0F, HbmGunDamageSource.applyPenetration(10.0F, 0.0F), 1.0E-6F);
    }
}
