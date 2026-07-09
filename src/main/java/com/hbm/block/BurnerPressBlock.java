package com.hbm.block;

import com.hbm.blockentity.BurnerPressBlockEntity;
import com.hbm.registry.HbmBlockEntities;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Containers;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

public class BurnerPressBlock extends BaseEntityBlock {
    public static final MapCodec<BurnerPressBlock> CODEC = simpleCodec(BurnerPressBlock::new);
    private static final VoxelShape SHAPE = Shapes.or(
            box(0.0D, 0.0D, 0.0D, 16.0D, 3.0D, 16.0D),
            box(2.0D, 3.0D, 2.0D, 14.0D, 6.0D, 14.0D),
            box(1.0D, 6.0D, 1.0D, 4.0D, 32.0D, 4.0D),
            box(12.0D, 6.0D, 1.0D, 15.0D, 32.0D, 4.0D),
            box(1.0D, 6.0D, 12.0D, 4.0D, 32.0D, 15.0D),
            box(12.0D, 6.0D, 12.0D, 15.0D, 32.0D, 15.0D),
            box(2.0D, 32.0D, 2.0D, 14.0D, 40.0D, 14.0D),
            box(5.0D, 14.0D, 5.0D, 11.0D, 32.0D, 11.0D),
            box(4.0D, 40.0D, 4.0D, 12.0D, 48.0D, 12.0D));

    public BurnerPressBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.INVISIBLE;
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new BurnerPressBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public MenuProvider getMenuProvider(BlockState state, Level level, BlockPos pos) {
        BlockEntity blockEntity = level.getBlockEntity(pos);
        return blockEntity instanceof BurnerPressBlockEntity burnerPress ? burnerPress : null;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
        if (!level.isClientSide && player instanceof ServerPlayer serverPlayer) {
            MenuProvider provider = state.getMenuProvider(level, pos);
            if (provider != null) {
                serverPlayer.openMenu(provider);
            }
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        if (level.isClientSide) {
            return createTickerHelper(type, HbmBlockEntities.BURNER_PRESS.get(), BurnerPressBlockEntity::clientTick);
        }
        return createTickerHelper(type, HbmBlockEntities.BURNER_PRESS.get(), BurnerPressBlockEntity::serverTick);
    }

    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        if (!state.is(newState.getBlock())) {
            BlockEntity blockEntity = level.getBlockEntity(pos);
            if (blockEntity instanceof BurnerPressBlockEntity burnerPress) {
                burnerPress.dropContents(level, pos);
            }
        }
        super.onRemove(state, level, pos, newState, movedByPiston);
    }
}
