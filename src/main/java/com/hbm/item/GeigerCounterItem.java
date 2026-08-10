package com.hbm.item;

import com.hbm.registry.HbmSounds;
import com.hbm.world.radiation.RadiationDiagnostics;
import com.hbm.world.radiation.RadiationManager;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundSource;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

public class GeigerCounterItem extends Item {
    public GeigerCounterItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!level.isClientSide && player instanceof ServerPlayer serverPlayer && level instanceof ServerLevel serverLevel) {
            RadiationDiagnostics data = RadiationManager.getDiagnostics(serverPlayer);
            serverLevel.playSound(null, serverPlayer.blockPosition(), HbmSounds.GEIGER_CLICK.get(), SoundSource.PLAYERS, 0.8F, 1.0F);
            serverPlayer.displayClientMessage(Component.translatable(
                    "item.hbm.geiger_counter.reading",
                    round(data.accumulatedRadiation()),
                    round(data.totalRate()),
                    round(data.resistance())
            ), true);
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
    }

    @Override
    public void inventoryTick(ItemStack stack, Level level, net.minecraft.world.entity.Entity entity, int slotId, boolean selected) {
        if (level.isClientSide || !(entity instanceof ServerPlayer player) || level.getGameTime() % 5 != 0) {
            return;
        }

        double rate = RadiationManager.getEnvironmentRate(player);
        if (rate <= 0.00001D) {
            if (player.getRandom().nextInt(50) != 0) {
                return;
            }
        } else {
            double clickChance = Math.min(0.95D, 0.10D + rate / 30D);
            if (player.getRandom().nextDouble() > clickChance) {
                return;
            }
        }

        level.playSound(null, player.blockPosition(), geigerSound(rate), SoundSource.PLAYERS, 0.7F, 1.0F);
    }

    private static SoundEvent geigerSound(double rate) {
        if (rate > 25D) {
            return HbmSounds.GEIGER_6.get();
        }
        if (rate > 20D) {
            return HbmSounds.GEIGER_5.get();
        }
        if (rate > 15D) {
            return HbmSounds.GEIGER_4.get();
        }
        if (rate > 10D) {
            return HbmSounds.GEIGER_3.get();
        }
        if (rate > 5D) {
            return HbmSounds.GEIGER_2.get();
        }
        return HbmSounds.GEIGER_1.get();
    }

    private static double round(double value) {
        return Math.round(value * 100D) / 100D;
    }
}
