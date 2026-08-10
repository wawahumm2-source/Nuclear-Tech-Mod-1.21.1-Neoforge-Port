package com.hbm.weapon.ammo;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.IItemHandlerModifiable;
import top.theillusivec4.curios.api.CuriosApi;

/** Reads only explicit AmmoProviderItem implementations equipped in Curios slots. */
public final class CuriosAmmoSource implements AmmoSource {
    private final ServerPlayer player;

    public CuriosAmmoSource(ServerPlayer player) {
        this.player = player;
    }

    @Override
    public int available(ResourceLocation ammoId) {
        return CuriosApi.getCuriosInventory(player).map(handler -> {
            int total = 0;
            IItemHandlerModifiable equipped = handler.getEquippedCurios();
            for (int slot = 0; slot < equipped.getSlots(); slot++) {
                ItemStack stack = equipped.getStackInSlot(slot);
                if (stack.getItem() instanceof AmmoProviderItem provider) {
                    total += Math.max(0, provider.availableAmmo(player, stack, ammoId));
                }
            }
            return total;
        }).orElse(0);
    }

    @Override
    public int extract(ResourceLocation ammoId, int requested) {
        if (requested <= 0) {
            return 0;
        }
        return CuriosApi.getCuriosInventory(player).map(handler -> {
            int remaining = requested;
            IItemHandlerModifiable equipped = handler.getEquippedCurios();
            for (int slot = 0; slot < equipped.getSlots() && remaining > 0; slot++) {
                ItemStack stack = equipped.getStackInSlot(slot);
                if (stack.getItem() instanceof AmmoProviderItem provider) {
                    int extracted = provider.extractAmmo(player, stack, ammoId, remaining);
                    remaining -= Math.max(0, Math.min(remaining, extracted));
                }
            }
            return requested - remaining;
        }).orElse(0);
    }
}
