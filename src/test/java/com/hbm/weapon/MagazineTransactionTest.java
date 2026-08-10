package com.hbm.weapon;

import com.hbm.weapon.data.GunDefinition;
import com.hbm.weapon.state.GunState;
import com.hbm.weapon.state.MagazineTransaction;
import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MagazineTransactionTest {
    @Test
    void loadingIsCapacityBoundedAndChambersTheWeapon() {
        GunDefinition pistol = WeaponTestFixtures.gun("gun_star_f");
        ResourceLocation fmj = ResourceLocation.fromNamespaceAndPath("hbm", "p22_fmj");
        GunState state = GunState.create(pistol);

        MagazineTransaction.Result result = MagazineTransaction.load(state, pistol, fmj, 50);
        assertEquals(10, result.acceptedRounds());
        assertEquals(10, result.state().ammoCount());
        assertTrue(result.state().chambered());
    }

    @Test
    void loadingCannotMixAmmoOrAcceptUnsupportedRounds() {
        GunDefinition pistol = WeaponTestFixtures.gun("gun_star_f");
        ResourceLocation fmj = ResourceLocation.fromNamespaceAndPath("hbm", "p22_fmj");
        ResourceLocation ap = ResourceLocation.fromNamespaceAndPath("hbm", "p22_ap");
        GunState loaded = MagazineTransaction.load(GunState.create(pistol), pistol, fmj, 2).state();

        assertThrows(IllegalArgumentException.class,
                () -> MagazineTransaction.load(loaded, pistol, ap, 1));
        assertThrows(IllegalArgumentException.class,
                () -> MagazineTransaction.load(loaded, pistol,
                        ResourceLocation.fromNamespaceAndPath("hbm", "g40_he"), 1));
    }
}
