package com.hbm.item;

import com.hbm.config.HbmConfig;
import com.hbm.world.radiation.ChunkRadiationService;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.context.UseOnContext;

/** Developer-only field injector for deterministic fallout and chunk-spread tests. */
public final class FalloutInjectorItem extends Item {
    public FalloutInjectorItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        if (!context.getLevel().isClientSide && context.getLevel() instanceof ServerLevel level && context.getPlayer() instanceof ServerPlayer player) {
            BlockPos pos = context.getClickedPos();
            double field = ChunkRadiationService.incrementRad(level, pos, HbmConfig.RADIATION.developerFalloutAmount.get());
            player.displayClientMessage(Component.literal(String.format("HBM fallout field: %.2f RAD/s", field)), true);
        }
        return InteractionResult.sidedSuccess(context.getLevel().isClientSide);
    }
}
