package com.hbm.world.radiation;

import com.hbm.HbmNuclearTech;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;

public final class HbmTags {
    public static final class Items {
        public static final TagKey<Item> RADIATION_SHIELDING = create("radiation_shielding");

        private static TagKey<Item> create(String name) {
            return TagKey.create(Registries.ITEM, ResourceLocation.fromNamespaceAndPath(HbmNuclearTech.MOD_ID, name));
        }

        private Items() {
        }
    }

    private HbmTags() {
    }
}
