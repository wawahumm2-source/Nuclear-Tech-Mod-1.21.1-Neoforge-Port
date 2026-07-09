package com.hbm.blockentity.machine;

import java.util.function.BiPredicate;
import java.util.function.IntPredicate;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.IItemHandler;

public final class SidedMachineItemHandler implements IItemHandler {
    private final IItemHandler items;
    private final BiPredicate<Integer, ItemStack> canInsert;
    private final IntPredicate canExtract;

    public SidedMachineItemHandler(IItemHandler items, BiPredicate<Integer, ItemStack> canInsert, IntPredicate canExtract) {
        this.items = items;
        this.canInsert = canInsert;
        this.canExtract = canExtract;
    }

    @Override
    public int getSlots() {
        return this.items.getSlots();
    }

    @Override
    public ItemStack getStackInSlot(int slot) {
        validateMachineSlot(slot);
        return this.items.getStackInSlot(slot);
    }

    @Override
    public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
        validateMachineSlot(slot);
        return this.canInsert.test(slot, stack) ? this.items.insertItem(slot, stack, simulate) : stack;
    }

    @Override
    public ItemStack extractItem(int slot, int amount, boolean simulate) {
        validateMachineSlot(slot);
        return this.canExtract.test(slot) ? this.items.extractItem(slot, amount, simulate) : ItemStack.EMPTY;
    }

    @Override
    public int getSlotLimit(int slot) {
        validateMachineSlot(slot);
        return this.items.getSlotLimit(slot);
    }

    @Override
    public boolean isItemValid(int slot, ItemStack stack) {
        validateMachineSlot(slot);
        return this.canInsert.test(slot, stack);
    }

    private void validateMachineSlot(int slot) {
        if (slot < 0 || slot >= this.items.getSlots()) {
            throw new RuntimeException("Slot " + slot + " is not in range 0.." + (this.items.getSlots() - 1));
        }
    }
}
