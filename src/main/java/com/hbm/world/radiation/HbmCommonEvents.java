package com.hbm.world.radiation;

import com.hbm.config.HbmConfig;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

public final class HbmCommonEvents {
    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        if (player.tickCount % HbmConfig.RADIATION_TICK_INTERVAL.get() == 0) {
            RadiationManager.tickPlayer(player);
        }
    }

    private HbmCommonEvents() {
    }
}
