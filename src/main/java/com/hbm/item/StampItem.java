package com.hbm.item;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

public class StampItem extends Item {
    private final StampType stampType;

    public StampItem(Properties properties, StampType stampType) {
        super(properties);
        this.stampType = stampType;
    }

    public StampType getStampType() {
        return this.stampType;
    }

    public static boolean hasType(ItemStack stack, StampType stampType) {
        return stack.getItem() instanceof StampItem stampItem && stampItem.getStampType() == stampType;
    }

    public static boolean isStamp(ItemStack stack) {
        return stack.getItem() instanceof StampItem;
    }
}
