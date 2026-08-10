package com.hbm.weapon.ammo;

import com.hbm.item.HbmAmmoItem;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

public final class PlayerInventoryAmmoSource implements AmmoSource {
    private final ServerPlayer player;

    public PlayerInventoryAmmoSource(ServerPlayer player) {
        this.player = player;
    }

    @Override
    public int available(ResourceLocation ammoId) {
        if (player.isCreative()) {
            return Integer.MAX_VALUE;
        }
        int total = 0;
        for (ItemStack stack : player.getInventory().items) {
            if (matches(stack, ammoId)) {
                total += stack.getCount();
            }
        }
        return total;
    }

    @Override
    public int extract(ResourceLocation ammoId, int requested) {
        if (requested <= 0) {
            return 0;
        }
        if (player.isCreative()) {
            return requested;
        }
        int remaining = requested;
        for (ItemStack stack : player.getInventory().items) {
            if (!matches(stack, ammoId)) {
                continue;
            }
            int taken = Math.min(remaining, stack.getCount());
            stack.shrink(taken);
            remaining -= taken;
            if (remaining == 0) {
                break;
            }
        }
        return requested - remaining;
    }

    private static boolean matches(ItemStack stack, ResourceLocation ammoId) {
        return stack.getItem() instanceof HbmAmmoItem ammo && ammo.ammoDefinitionId().equals(ammoId);
    }
}
