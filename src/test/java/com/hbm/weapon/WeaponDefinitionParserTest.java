package com.hbm.weapon;

import com.google.gson.JsonObject;
import com.hbm.weapon.data.GunDefinition;
import com.hbm.weapon.data.GunDefinitionRegistry;
import com.hbm.weapon.data.WeaponDefinitionParser;
import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class WeaponDefinitionParserTest {
    @Test
    void parsesEveryBuiltInDefinition() {
        assertEquals(4, WeaponTestFixtures.allGuns().size());
        assertEquals(8, WeaponTestFixtures.allAmmo().size());
        assertEquals(1200.0D / 650.0D,
                WeaponTestFixtures.gun("gun_stg77").getShotIntervalTicks(), 1.0E-9D);
    }

    @Test
    void rejectsFractionalIntegerAndUnknownField() {
        ResourceLocation id = ResourceLocation.fromNamespaceAndPath("hbm", "gun_star_f");
        JsonObject fractional = WeaponTestFixtures.json(
                "/data/hbm/guns/gun_star_f.json");
        fractional.addProperty("rpm", 420.5D);
        IllegalArgumentException timing = assertThrows(IllegalArgumentException.class,
                () -> WeaponDefinitionParser.parseGun(id, fractional));
        assertTrue(timing.getMessage().contains("rpm must be an integer"));

        JsonObject unknown = WeaponTestFixtures.json(
                "/data/hbm/guns/gun_star_f.json");
        unknown.addProperty("client_damage", 9999);
        IllegalArgumentException field = assertThrows(IllegalArgumentException.class,
                () -> WeaponDefinitionParser.parseGun(id, unknown));
        assertTrue(field.getMessage().contains("unknown fields"));
    }

    @Test
    void invalidRegistryInstallKeepsPreviousSnapshot() {
        GunDefinitionRegistry.Snapshot installed = GunDefinitionRegistry.install(
                WeaponTestFixtures.allGuns(), WeaponTestFixtures.allAmmo());
        LinkedHashMap<ResourceLocation, GunDefinition> wrongKeys = new LinkedHashMap<>(installed.getGuns());
        GunDefinition pistol = wrongKeys.remove(ResourceLocation.fromNamespaceAndPath("hbm", "gun_star_f"));
        wrongKeys.put(ResourceLocation.fromNamespaceAndPath("hbm", "wrong_key"), pistol);

        assertThrows(IllegalArgumentException.class,
                () -> GunDefinitionRegistry.install(wrongKeys, installed.getAmmo()));
        assertSame(installed, GunDefinitionRegistry.snapshot());
    }
}
