package com.hbm.weapon.behavior;

import com.hbm.HbmNuclearTech;
import net.minecraft.resources.ResourceLocation;

import java.util.Map;

public final class GunBehaviorRegistry {
    private static final Map<ResourceLocation, GunBehavior> BEHAVIORS = Map.of(
            id("standard"), new StandardGunBehavior(),
            id("grenade_launcher"), new GrenadeLauncherBehavior()
    );

    public static GunBehavior require(ResourceLocation id) {
        GunBehavior behavior = BEHAVIORS.get(id);
        if (behavior == null) {
            throw new IllegalArgumentException("Unsupported gun behavior " + id);
        }
        return behavior;
    }

    private static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(HbmNuclearTech.MOD_ID, path);
    }

    private GunBehaviorRegistry() {
    }
}
