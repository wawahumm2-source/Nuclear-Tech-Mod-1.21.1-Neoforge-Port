package com.hbm.item;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;

public final class HbmAmmoItem extends Item {
    private final ResourceLocation ammoDefinitionId;

    public HbmAmmoItem(Properties properties, ResourceLocation ammoDefinitionId) {
        super(properties);
        this.ammoDefinitionId = ammoDefinitionId;
    }

    public ResourceLocation ammoDefinitionId() {
        return ammoDefinitionId;
    }
}
