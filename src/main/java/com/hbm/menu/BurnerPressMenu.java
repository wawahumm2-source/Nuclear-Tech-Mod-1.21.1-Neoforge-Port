package com.hbm.menu;

import com.hbm.blockentity.BurnerPressBlockEntity;
import com.hbm.item.StampItem;
import com.hbm.registry.HbmBlocks;
import com.hbm.registry.HbmMenus;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.AbstractFurnaceBlockEntity;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemStackHandler;
import net.neoforged.neoforge.items.SlotItemHandler;

public class BurnerPressMenu extends AbstractContainerMenu {
    private static final int BACKING_MACHINE_SLOT_COUNT = BurnerPressBlockEntity.SLOT_COUNT;
    private static final int VISIBLE_MACHINE_SLOT_COUNT = 4;
    private static final int MAX_SPEED = 400;
    private static final int PLAYER_INVENTORY_START = VISIBLE_MACHINE_SLOT_COUNT;
    private static final int PLAYER_INVENTORY_END = PLAYER_INVENTORY_START + 27;
    private static final int HOTBAR_END = PLAYER_INVENTORY_END + 9;

    private final ContainerLevelAccess access;
    private final ContainerData data;

    public BurnerPressMenu(int containerId, Inventory playerInventory) {
        this(containerId, playerInventory, new ItemStackHandler(BACKING_MACHINE_SLOT_COUNT), new SimpleContainerData(5), ContainerLevelAccess.NULL);
    }

    public BurnerPressMenu(int containerId, Inventory playerInventory, IItemHandler itemHandler, ContainerData data, ContainerLevelAccess access) {
        super(HbmMenus.BURNER_PRESS.get(), containerId);
        if (itemHandler.getSlots() != BACKING_MACHINE_SLOT_COUNT) {
            throw new IllegalArgumentException("Burner Press requires exactly " + BACKING_MACHINE_SLOT_COUNT + " backing slots.");
        }
        checkContainerDataCount(data, 5);
        this.access = access;
        this.data = data;

        this.addSlot(new FuelSlot(itemHandler, BurnerPressBlockEntity.SLOT_FUEL, 26, 53));
        this.addSlot(new StampSlot(itemHandler, BurnerPressBlockEntity.SLOT_STAMP, 80, 17));
        this.addSlot(new SlotItemHandler(itemHandler, BurnerPressBlockEntity.SLOT_INPUT, 80, 53));
        this.addSlot(new OutputSlot(itemHandler, BurnerPressBlockEntity.SLOT_OUTPUT, 140, 35));
        addPlayerInventory(playerInventory);
        addPlayerHotbar(playerInventory);
        this.addDataSlots(data);
    }

    public int getProgress() {
        return this.data.get(0);
    }

    public int getMaxProgress() {
        return this.data.get(1);
    }

    public int getBurnTime() {
        return this.data.get(2);
    }

    public int getBurnCost() {
        return Math.max(1, this.data.get(3));
    }

    public int getSpeed() {
        return this.data.get(4);
    }

    public int getScaledSpeed(int scale) {
        int speed = getSpeed();
        if (speed <= 0) {
            return 0;
        }
        return Math.max(1, Math.min(scale, speed * scale / MAX_SPEED));
    }

    public int getSpeedPercent() {
        return getSpeed() * 100 / MAX_SPEED;
    }

    public int getStoredOperations() {
        return getBurnTime() / getBurnCost();
    }

    public int getScaledProgress(int width) {
        int max = getMaxProgress();
        return max <= 0 ? 0 : getProgress() * width / max;
    }

    public int getScaledBurnTime(int height) {
        return Math.min(height, getBurnTime() * height / getBurnCost());
    }

    @Override
    public boolean stillValid(Player player) {
        return stillValid(this.access, player, HbmBlocks.BURNER_PRESS.get());
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        ItemStack quickMovedStack = ItemStack.EMPTY;
        Slot quickMovedSlot = this.slots.get(index);

        if (quickMovedSlot != null && quickMovedSlot.hasItem()) {
            ItemStack rawStack = quickMovedSlot.getItem();
            quickMovedStack = rawStack.copy();

            if (index < VISIBLE_MACHINE_SLOT_COUNT) {
                if (!this.moveItemStackTo(rawStack, PLAYER_INVENTORY_START, HOTBAR_END, true)) {
                    return ItemStack.EMPTY;
                }
            } else if (!movePlayerStackToMachine(rawStack)) {
                if (index < PLAYER_INVENTORY_END) {
                    if (!this.moveItemStackTo(rawStack, PLAYER_INVENTORY_END, HOTBAR_END, false)) {
                        return ItemStack.EMPTY;
                    }
                } else if (!this.moveItemStackTo(rawStack, PLAYER_INVENTORY_START, PLAYER_INVENTORY_END, false)) {
                    return ItemStack.EMPTY;
                }
            }

            if (rawStack.isEmpty()) {
                quickMovedSlot.set(ItemStack.EMPTY);
            } else {
                quickMovedSlot.setChanged();
            }
            quickMovedSlot.onTake(player, rawStack);
        }

        return quickMovedStack;
    }

    private boolean movePlayerStackToMachine(ItemStack rawStack) {
        if (StampItem.isStamp(rawStack) && this.moveItemStackTo(rawStack, BurnerPressBlockEntity.SLOT_STAMP, BurnerPressBlockEntity.SLOT_STAMP + 1, false)) {
            return true;
        }
        if (AbstractFurnaceBlockEntity.isFuel(rawStack) && this.moveItemStackTo(rawStack, BurnerPressBlockEntity.SLOT_FUEL, BurnerPressBlockEntity.SLOT_FUEL + 1, false)) {
            return true;
        }
        return this.moveItemStackTo(rawStack, BurnerPressBlockEntity.SLOT_INPUT, BurnerPressBlockEntity.SLOT_INPUT + 1, false);
    }

    private void addPlayerInventory(Inventory playerInventory) {
        for (int row = 0; row < 3; row++) {
            for (int column = 0; column < 9; column++) {
                this.addSlot(new Slot(playerInventory, column + row * 9 + 9, 8 + column * 18, 120 + row * 18));
            }
        }
    }

    private void addPlayerHotbar(Inventory playerInventory) {
        for (int column = 0; column < 9; column++) {
            this.addSlot(new Slot(playerInventory, column, 8 + column * 18, 178));
        }
    }

    private static class FuelSlot extends SlotItemHandler {
        FuelSlot(IItemHandler itemHandler, int index, int xPosition, int yPosition) {
            super(itemHandler, index, xPosition, yPosition);
        }

        @Override
        public boolean mayPlace(ItemStack stack) {
            return AbstractFurnaceBlockEntity.isFuel(stack);
        }
    }

    private static class StampSlot extends SlotItemHandler {
        StampSlot(IItemHandler itemHandler, int index, int xPosition, int yPosition) {
            super(itemHandler, index, xPosition, yPosition);
        }

        @Override
        public boolean mayPlace(ItemStack stack) {
            return StampItem.isStamp(stack);
        }
    }

    private static class OutputSlot extends SlotItemHandler {
        OutputSlot(IItemHandler itemHandler, int index, int xPosition, int yPosition) {
            super(itemHandler, index, xPosition, yPosition);
        }

        @Override
        public boolean mayPlace(ItemStack stack) {
            return false;
        }
    }
}
