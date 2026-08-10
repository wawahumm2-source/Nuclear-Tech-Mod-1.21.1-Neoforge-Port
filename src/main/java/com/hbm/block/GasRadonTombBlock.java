package com.hbm.block;

import com.hbm.registry.HbmBlocks;
import com.hbm.world.radiation.RadiationManager;
import com.hbm.world.radiation.RadiationSourceType;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;

public final class GasRadonTombBlock extends GasBaseBlock {
    public GasRadonTombBlock(BlockBehaviour.Properties properties) {
        super(properties);
    }

    @Override
    protected void entityInside(BlockState state, Level level, BlockPos pos, Entity entity) {
        if (level.isClientSide || !(entity instanceof LivingEntity living)) {
            return;
        }
        if (living instanceof ServerPlayer player) {
            RadiationManager.cancelTreatmentAndRadX(player);
        }
        RadiationManager.applyDirectExposure(living, RadiationSourceType.BLOCK, 0.5D, true);
    }

    @Override
    protected Direction firstDirection(RandomSource random) {
        return random.nextInt(3) == 0 ? Direction.UP : Direction.DOWN;
    }

    @Override
    protected void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        if (random.nextInt(10) == 0) {
            BlockPos below = pos.below();
            BlockState belowState = level.getBlockState(below);
            if (belowState.is(Blocks.GRASS_BLOCK)) {
                level.setBlock(
                        below,
                        random.nextInt(5) == 0
                                ? Blocks.DIRT.defaultBlockState()
                                : HbmBlocks.WASTE_EARTH.get().defaultBlockState(),
                        3
                );
            } else if (!belowState.isCollisionShapeFullBlock(level, below)
                    && (belowState.is(BlockTags.LEAVES)
                    || belowState.is(BlockTags.FLOWERS)
                    || belowState.is(BlockTags.SAPLINGS)
                    || belowState.is(BlockTags.CLIMBABLE)
                    || belowState.is(BlockTags.REPLACEABLE))) {
                level.setBlock(below, Blocks.AIR.defaultBlockState(), 3);
            }
        }
        if (random.nextInt(600) == 0) {
            level.setBlock(pos, Blocks.AIR.defaultBlockState(), 3);
            return;
        }
        super.tick(state, level, pos, random);
    }
}
