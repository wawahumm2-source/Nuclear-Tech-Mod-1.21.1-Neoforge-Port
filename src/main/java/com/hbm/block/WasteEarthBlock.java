package com.hbm.block;

import com.hbm.config.HbmConfig;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.loot.LootParams;

/** Tier 1 Waste Earth, named Dead Grass in the original language file. */
public final class WasteEarthBlock extends Block {
    public WasteEarthBlock(BlockBehaviour.Properties properties) {
        super(properties);
    }

    @Override
    protected void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        BlockPos above = pos.above();
        boolean coveredAndDark = level.getRawBrightness(above, 0) < 4
                && level.getBlockState(above).getLightBlock(level, above) > 2;
        if (HbmConfig.RADIATION.cleanupDeadDirt.get() || coveredAndDark) {
            level.setBlock(pos, Blocks.DIRT.defaultBlockState(), 3);
        }
    }

    @Override
    protected List<ItemStack> getDrops(BlockState state, LootParams.Builder builder) {
        return List.of(new ItemStack(Blocks.DIRT));
    }
}
