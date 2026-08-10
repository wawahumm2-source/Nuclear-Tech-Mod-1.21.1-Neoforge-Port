package com.hbm.block;

import com.hbm.world.radiation.RadiationManager;
import com.hbm.world.radiation.RadiationSourceType;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;

public final class GasRadonBlock extends GasBaseBlock {
    public GasRadonBlock(BlockBehaviour.Properties properties) {
        super(properties);
    }

    @Override
    protected void entityInside(BlockState state, Level level, BlockPos pos, Entity entity) {
        if (!level.isClientSide && entity instanceof LivingEntity living) {
            RadiationManager.applyDirectExposure(living, RadiationSourceType.BLOCK, 0.05D, true);
        }
    }

    @Override
    protected Direction firstDirection(RandomSource random) {
        return random.nextInt(5) == 0 ? Direction.UP : Direction.DOWN;
    }

    @Override
    protected void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        if (random.nextInt(50) == 0) {
            level.setBlock(pos, Blocks.AIR.defaultBlockState(), 3);
            return;
        }
        super.tick(state, level, pos, random);
    }
}
