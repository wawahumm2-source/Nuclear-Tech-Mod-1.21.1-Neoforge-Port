package com.hbm.client;

import com.hbm.block.SellafieldSlakedBlock;
import com.hbm.world.radiation.SellafieldMath;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.BlockItemStateProperties;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

/** Client-only color math from BlockSellafieldSlaked. */
final class SellafieldColors {
    static int blockColor(BlockState state, @Nullable BlockAndTintGetter level, @Nullable BlockPos pos,
            int tintIndex) {
        return tintIndex == 0 ? SellafieldMath.slakedColor(state.getValue(SellafieldSlakedBlock.STAGE)) : 0xFFFFFF;
    }

    static int itemColor(ItemStack stack, int tintIndex) {
        if (tintIndex != 0 || !(stack.getItem() instanceof BlockItem blockItem)
                || !blockItem.getBlock().defaultBlockState().hasProperty(SellafieldSlakedBlock.STAGE)) {
            return 0xFFFFFF;
        }
        BlockItemStateProperties properties = stack.getOrDefault(
                DataComponents.BLOCK_STATE,
                BlockItemStateProperties.EMPTY
        );
        Integer stage = properties.get(SellafieldSlakedBlock.STAGE);
        return SellafieldMath.slakedColor(stage == null ? 0 : stage);
    }

    private SellafieldColors() {
    }
}
