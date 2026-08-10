package com.hbm.block;

import java.util.List;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.BlockItemStateProperties;
import net.minecraft.world.item.component.CustomModelData;
import net.minecraft.world.level.block.Block;

/** Metadata-style BlockItem wrapper for the six source Sellafite tiers. */
public final class SellafieldBlockItem extends BlockItem {
    public SellafieldBlockItem(Block block, Item.Properties properties) {
        super(block, properties);
    }

    public static ItemStack createStack(Item item, int level) {
        int clampedLevel = Math.clamp(level, 0, SellafieldBlock.LEVEL_COUNT - 1);
        ItemStack stack = new ItemStack(item);
        stack.set(DataComponents.BLOCK_STATE,
                BlockItemStateProperties.EMPTY.with(SellafieldBlock.LEVEL, clampedLevel));
        stack.set(DataComponents.CUSTOM_MODEL_DATA, new CustomModelData(clampedLevel));
        return stack;
    }

    public static int getLevel(ItemStack stack) {
        BlockItemStateProperties properties = stack.getOrDefault(
                DataComponents.BLOCK_STATE,
                BlockItemStateProperties.EMPTY
        );
        Integer value = properties.get(SellafieldBlock.LEVEL);
        return value == null ? 0 : Math.clamp(value, 0, SellafieldBlock.LEVEL_COUNT - 1);
    }

    @Override
    public String getDescriptionId(ItemStack stack) {
        return "block.hbm.sellafield." + getLevel(stack);
    }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, List<Component> tooltip,
            TooltipFlag flag) {
        tooltip.add(Component.translatable(
                "item.hbm.radioactive.tooltip",
                String.format(java.util.Locale.ROOT, "%.2f", SellafieldBlock.itemRadiation(getLevel(stack)))
        ));
        super.appendHoverText(stack, context, tooltip, flag);
    }
}
