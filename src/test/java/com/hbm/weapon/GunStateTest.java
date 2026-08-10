package com.hbm.weapon;

import com.hbm.weapon.data.FireMode;
import com.hbm.weapon.data.GunDefinition;
import com.hbm.weapon.state.GunState;
import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

class GunStateTest {
    @Test
    void migrationBoundsMagazineAndNormalizesSelections() {
        GunDefinition pistol = WeaponTestFixtures.gun("gun_star_f");
        ResourceLocation fmj = ResourceLocation.fromNamespaceAndPath("hbm", "p22_fmj");
        GunState oversized = new GunState(0, UUID.randomUUID(), fmj, 200, true,
                FireMode.AUTO, ResourceLocation.fromNamespaceAndPath("hbm", "not_ammo"), 3.0F, 4);

        GunState migrated = oversized.migrated(pistol);
        assertEquals(GunState.CURRENT_VERSION, migrated.version());
        assertEquals(10, migrated.ammoCount());
        assertEquals(FireMode.SEMI, migrated.fireMode());
        assertEquals(fmj, migrated.selectedAmmoId());
    }

    @Test
    void invalidLoadedAmmoIsClearedAndStackCopyCanBeReminted() {
        GunDefinition pistol = WeaponTestFixtures.gun("gun_star_f");
        UUID identity = UUID.randomUUID();
        GunState invalid = new GunState(1, identity,
                ResourceLocation.fromNamespaceAndPath("hbm", "g40_he"), 4, true,
                FireMode.SEMI, ResourceLocation.fromNamespaceAndPath("hbm", "p22_fmj"), 0.0F, 0);

        GunState migrated = invalid.migrated(pistol);
        assertEquals(0, migrated.ammoCount());
        assertEquals(GunState.EMPTY_AMMO, migrated.loadedAmmoId());
        assertFalse(migrated.chambered());

        GunState reminted = migrated.withStackIdentity(UUID.randomUUID());
        assertNotEquals(identity, reminted.stackIdentity());
        assertEquals(migrated.ammoCount(), reminted.ammoCount());
    }
}
