package com.hbm.weapon.ammo;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

/** Future HBM ammo bags can implement this without coupling the gun service to a bag UI. */
public interface AmmoProviderItem {
    int availableAmmo(ServerPlayer player, ItemStack providerStack, ResourceLocation ammoId);

    int extractAmmo(ServerPlayer player, ItemStack providerStack, ResourceLocation ammoId, int requested);
}
