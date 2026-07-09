package com.hbm.blockentity.machine;

import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.ItemStackHandler;

public final class MachineOutputHelper {
    public static boolean canMerge(ItemStack output, ItemStack result) {
        if (result.isEmpty()) {
            return false;
        }
        if (output.isEmpty()) {
            return true;
        }
        return ItemStack.isSameItemSameComponents(output, result) && output.getCount() + result.getCount() <= output.getMaxStackSize();
    }

    public static void mergeIntoSlot(ItemStackHandler items, int slot, ItemStack result) {
        ItemStack output = items.getStackInSlot(slot);
        if (output.isEmpty()) {
            items.setStackInSlot(slot, result.copy());
        } else {
            output.grow(result.getCount());
        }
    }

    private MachineOutputHelper() {
    }
}
