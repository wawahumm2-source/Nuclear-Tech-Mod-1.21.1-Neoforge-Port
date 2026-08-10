package com.hbm.item;

import com.hbm.world.radiation.RadiationDiagnostics;
import com.hbm.world.radiation.RadiationManager;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/** Developer-only inspector. Holding it renders the client overlay; right-clicking prints the server snapshot. */
public final class RadiationInspectorItem extends Item {
    public RadiationInspectorItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!level.isClientSide && player instanceof ServerPlayer serverPlayer) {
            RadiationDiagnostics data = RadiationManager.getDiagnostics(serverPlayer);
            serverPlayer.displayClientMessage(Component.literal(String.format(
                    "RAD %.2f | %.3f RAD/s | resistance %.3f",
                    data.accumulatedRadiation(), data.totalRate(), data.resistance()
            )), false);
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
    }

    @Override
    public InteractionResult interactLivingEntity(ItemStack stack, Player player, LivingEntity target, InteractionHand hand) {
        if (!(target instanceof Mob mob)) {
            return InteractionResult.PASS;
        }
        if (!player.level().isClientSide && player instanceof ServerPlayer serverPlayer) {
            if (RadiationManager.isRadiationImmune(mob)) {
                serverPlayer.displayClientMessage(Component.literal(mob.getDisplayName().getString() + " is radiation-immune."), true);
            } else {
                RadiationDiagnostics data = RadiationManager.getDiagnostics(mob);
                serverPlayer.displayClientMessage(Component.literal(String.format(
                        "%s | RAD %.2f | %.3f RAD/s",
                        mob.getDisplayName().getString(), data.accumulatedRadiation(), data.totalRate()
                )), true);
            }
        }
        return InteractionResult.sidedSuccess(player.level().isClientSide);
    }
}
