package com.hbm.item;

import com.hbm.config.HbmConfig;
import com.hbm.registry.HbmSounds;
import com.hbm.world.radiation.RadiationManager;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundSource;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

public class RadAwayItem extends Item {
    public enum Treatment {
        NORMAL,
        STRONG,
        FLUSH
    }

    private final Treatment treatment;

    public RadAwayItem(Properties properties, Treatment treatment) {
        super(properties);
        this.treatment = treatment;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!level.isClientSide && player instanceof ServerPlayer serverPlayer) {
            RadiationManager.beginTreatment(serverPlayer, treatmentTicks(), treatmentPerTick());
            level.playSound(null, serverPlayer.blockPosition(), HbmSounds.RADAWAY.get(), SoundSource.PLAYERS, 1.0F, 1.0F);
            serverPlayer.displayClientMessage(Component.translatable("message.hbm.radaway", totalReduction()), true);
            if (!serverPlayer.getAbilities().instabuild) {
                stack.shrink(1);
            }
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("tooltip.hbm.radaway", totalReduction()).withStyle(ChatFormatting.AQUA));
    }

    private double totalReduction() {
        return treatmentTicks() * treatmentPerTick();
    }

    private int treatmentTicks() {
        return switch (this.treatment) {
            case NORMAL -> HbmConfig.RADIATION.radawayTicks.get();
            case STRONG -> HbmConfig.RADIATION.radawayStrongTicks.get();
            case FLUSH -> HbmConfig.RADIATION.radawayFlushTicks.get();
        };
    }

    private double treatmentPerTick() {
        return switch (this.treatment) {
            case NORMAL -> HbmConfig.RADIATION.radawayPerTick.get();
            case STRONG -> HbmConfig.RADIATION.radawayStrongPerTick.get();
            case FLUSH -> HbmConfig.RADIATION.radawayFlushPerTick.get();
        };
    }
}
