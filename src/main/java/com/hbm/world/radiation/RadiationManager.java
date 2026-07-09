package com.hbm.world.radiation;

import com.hbm.config.HbmConfig;
import com.hbm.network.HbmPayloads;
import com.hbm.world.damage.HbmDamageTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

public final class RadiationManager {
    public static void tickPlayer(ServerPlayer player) {
        if (!(player.level() instanceof ServerLevel level)) {
            return;
        }

        double dose = scanAmbientBlocks(level, player.blockPosition());
        dose += scanInventory(player);
        dose += ChunkRadiationService.getRadiation(level, player.blockPosition());
        dose *= protectionMultiplier(player);

        int interval = HbmConfig.RADIATION_TICK_INTERVAL.get();
        RadiationSavedData data = RadiationSavedData.get(level);
        if (dose > 0D) {
            data.addExposure(player.getUUID(), dose);
        }
        double exposure = data.addExposure(player.getUUID(), -HbmConfig.RADIATION_DECAY_PER_TICK.get() * interval);

        if (exposure >= HbmConfig.RADIATION_DAMAGE_THRESHOLD.get() && player.tickCount % 80 == 0) {
            player.hurt(HbmDamageTypes.radiation(level), 1.0F);
        }
        HbmPayloads.syncRadiation(player);
    }

    public static double getExposure(ServerPlayer player) {
        return RadiationSavedData.get(player.serverLevel()).getExposure(player.getUUID());
    }

    public static void reduceExposure(ServerPlayer player, double amount) {
        RadiationSavedData.get(player.serverLevel()).removeExposure(player.getUUID(), amount);
        HbmPayloads.syncRadiation(player);
    }

    private static double scanAmbientBlocks(ServerLevel level, BlockPos center) {
        int radius = HbmConfig.AMBIENT_RADIATION_RADIUS.get();
        double dose = 0D;
        for (BlockPos pos : BlockPos.betweenClosed(center.offset(-radius, -1, -radius), center.offset(radius, 1, radius))) {
            dose += RadiationSources.fromBlock(level.getBlockState(pos));
        }
        return dose;
    }

    private static double scanInventory(ServerPlayer player) {
        double dose = 0D;
        for (int slot = 0; slot < player.getInventory().getContainerSize(); slot++) {
            dose += RadiationSources.fromStack(player.getInventory().getItem(slot));
        }
        return dose;
    }

    private static double protectionMultiplier(ServerPlayer player) {
        double multiplier = 1D;
        for (ItemStack stack : player.getArmorSlots()) {
            if (stack.is(HbmTags.Items.RADIATION_SHIELDING)) {
                multiplier -= HbmConfig.SHIELDING_ITEM_FACTOR.get();
            }
        }
        return Math.max(0.05D, multiplier);
    }

    private RadiationManager() {
    }
}
