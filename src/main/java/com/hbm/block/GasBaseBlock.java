package com.hbm.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/** Invisible, replaceable source gas that moves every two ticks without recursive world updates. */
public abstract class GasBaseBlock extends Block {
    protected GasBaseBlock(BlockBehaviour.Properties properties) {
        super(properties);
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.INVISIBLE;
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return Shapes.empty();
    }

    @Override
    protected void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean movedByPiston) {
        if (!oldState.is(this) && !level.isClientSide) {
            level.scheduleTick(pos, this, 10);
        }
    }

    @Override
    protected void neighborChanged(BlockState state, Level level, BlockPos pos, Block neighborBlock,
            BlockPos neighborPos, boolean movedByPiston) {
        if (!level.isClientSide) {
            level.scheduleTick(pos, this, 10);
        }
    }

    @Override
    protected void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        if (!tryMove(level, pos, firstDirection(random))
                && !tryMove(level, pos, randomHorizontal(random))) {
            level.scheduleTick(pos, this, 2);
        }
    }

    protected abstract Direction firstDirection(RandomSource random);

    protected boolean tryMove(ServerLevel level, BlockPos pos, Direction direction) {
        BlockPos target = pos.relative(direction);
        if (!level.getBlockState(target).isAir()) {
            return false;
        }
        level.setBlock(pos, Blocks.AIR.defaultBlockState(), 2);
        level.setBlock(target, defaultBlockState(), 2);
        level.scheduleTick(target, this, 2);
        return true;
    }

    private static Direction randomHorizontal(RandomSource random) {
        return switch (random.nextInt(4)) {
            case 0 -> Direction.NORTH;
            case 1 -> Direction.SOUTH;
            case 2 -> Direction.WEST;
            default -> Direction.EAST;
        };
    }
}
