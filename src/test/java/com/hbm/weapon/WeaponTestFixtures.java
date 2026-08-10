package com.hbm.weapon;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.hbm.weapon.data.AmmoDefinition;
import com.hbm.weapon.data.GunDefinition;
import com.hbm.weapon.data.WeaponDefinitionParser;
import net.minecraft.resources.ResourceLocation;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;

final class WeaponTestFixtures {
    static GunDefinition gun(String name) {
        ResourceLocation id = ResourceLocation.fromNamespaceAndPath("hbm", name);
        return WeaponDefinitionParser.parseGun(id, json(
                "/data/hbm/guns/" + name + ".json"));
    }

    static AmmoDefinition ammo(String name) {
        ResourceLocation id = ResourceLocation.fromNamespaceAndPath("hbm", name);
        return WeaponDefinitionParser.parseAmmo(id, json(
                "/data/hbm/ammo/" + name + ".json"));
    }

    static Map<ResourceLocation, GunDefinition> allGuns() {
        Map<ResourceLocation, GunDefinition> definitions = new LinkedHashMap<>();
        for (String id : new String[]{"gun_star_f", "gun_stg77", "gun_spas12", "gun_congolake"}) {
            GunDefinition definition = gun(id);
            definitions.put(definition.getId(), definition);
        }
        return definitions;
    }

    static Map<ResourceLocation, AmmoDefinition> allAmmo() {
        Map<ResourceLocation, AmmoDefinition> definitions = new LinkedHashMap<>();
        for (String id : new String[]{
                "p22_fmj", "p22_ap", "r556_fmj", "r556_ap",
                "g12_buckshot", "g12_slug", "g40_he", "g40_heat"
        }) {
            AmmoDefinition definition = ammo(id);
            definitions.put(definition.getId(), definition);
        }
        return definitions;
    }

    static JsonObject json(String resourcePath) {
        InputStream stream = WeaponTestFixtures.class.getResourceAsStream(resourcePath);
        if (stream == null) {
            throw new AssertionError("Missing classpath test fixture " + resourcePath);
        }
        try (Reader reader = new InputStreamReader(stream, StandardCharsets.UTF_8)) {
            return JsonParser.parseReader(reader).getAsJsonObject();
        } catch (Exception exception) {
            throw new AssertionError("Unable to read test fixture " + resourcePath, exception);
        }
    }

    private WeaponTestFixtures() {
    }
}
