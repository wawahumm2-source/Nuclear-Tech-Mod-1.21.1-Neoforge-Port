package com.hbm.block;

import com.hbm.world.explosion.HbmExplosionService;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

public class PrototypeNukeBlock extends Block {
    public PrototypeNukeBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
        if (!level.isClientSide && player instanceof ServerPlayer serverPlayer && level instanceof ServerLevel serverLevel) {
            detonate(serverLevel, pos);
            serverPlayer.displayClientMessage(Component.translatable("message.hbm.prototype_nuke.armed"), true);
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override
    protected void neighborChanged(BlockState state, Level level, BlockPos pos, Block block, BlockPos fromPos, boolean isMoving) {
        if (!level.isClientSide && level.hasNeighborSignal(pos) && level instanceof ServerLevel serverLevel) {
            detonate(serverLevel, pos);
        }
    }

    private static void detonate(ServerLevel level, BlockPos pos) {
        HbmExplosionService.detonatePrototypeNuke(level, pos, null);
    }
}
