package com.hbm.item;

import com.hbm.config.HbmConfig;
import com.hbm.world.radiation.RadiationManager;
import com.hbm.world.radiation.RadiationSourceType;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/** Developer-only direct exposure trigger representing the radiation portion of an explosion. */
public final class RadiationPulseItem extends Item {
    public RadiationPulseItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!level.isClientSide && player instanceof ServerPlayer serverPlayer) {
            RadiationManager.applyDirectExposure(serverPlayer, RadiationSourceType.EXPLOSION, HbmConfig.RADIATION.developerExplosionDose.get(), false);
            serverPlayer.displayClientMessage(Component.literal("HBM explosion-radiation pulse applied."), true);
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
    }
}
