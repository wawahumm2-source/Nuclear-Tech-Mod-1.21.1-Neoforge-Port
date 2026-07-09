package com.hbm.item;

import com.hbm.registry.HbmSounds;
import com.hbm.world.radiation.RadiationManager;
import com.hbm.world.radiation.RadiationSavedData;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundSource;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;

public class GeigerCounterItem extends Item {
    public GeigerCounterItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!level.isClientSide && player instanceof ServerPlayer serverPlayer && level instanceof ServerLevel serverLevel) {
            double exposure = RadiationManager.getExposure(serverPlayer);
            double fallout = RadiationSavedData.get(serverLevel).getChunkFallout(new ChunkPos(serverPlayer.blockPosition()));
            serverLevel.playSound(null, serverPlayer.blockPosition(), HbmSounds.GEIGER_CLICK.get(), SoundSource.PLAYERS, 0.8F, 1.0F);
            serverPlayer.displayClientMessage(Component.translatable("item.hbm.geiger_counter.reading", round(exposure), round(fallout)), true);
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
    }

    private static double round(double value) {
        return Math.round(value * 100D) / 100D;
    }
}
