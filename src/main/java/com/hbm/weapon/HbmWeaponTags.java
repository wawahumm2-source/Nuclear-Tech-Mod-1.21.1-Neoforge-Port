package com.hbm.weapon;

import com.hbm.HbmNuclearTech;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.block.Block;

public final class HbmWeaponTags {
    public static final TagKey<Block> FRAGILE_TO_GUNFIRE = TagKey.create(
            Registries.BLOCK,
            ResourceLocation.fromNamespaceAndPath(HbmNuclearTech.MOD_ID, "fragile_to_gunfire")
    );
    public static final TagKey<EntityType<?>> NO_HEADSHOTS = TagKey.create(
            Registries.ENTITY_TYPE,
            ResourceLocation.fromNamespaceAndPath(HbmNuclearTech.MOD_ID, "no_headshots")
    );

    private HbmWeaponTags() {
    }
}
