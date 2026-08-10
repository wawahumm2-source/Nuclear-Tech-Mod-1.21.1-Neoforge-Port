package com.hbm.world.radiation;

import com.hbm.config.HbmConfig;
import com.hbm.world.explosion.HbmExplosionService;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Mob;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.tick.EntityTickEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

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

    @SubscribeEvent
    public static void onMobTick(EntityTickEvent.Post event) {
        if (!(event.getEntity() instanceof Mob mob) || !(mob.level() instanceof ServerLevel)) {
            return;
        }
        if (!HbmConfig.RADIATION.enableMobRadiation.get()) {
            return;
        }
        int interval = HbmConfig.RADIATION.mobTickInterval.get();
        if (Math.floorMod(mob.tickCount + mob.getId(), interval) == 0) {
            RadiationManager.tickMob(mob);
        }
    }

    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Post event) {
        HbmExplosionService.tick(event.getServer());
        if (!HbmConfig.RADIATION.enableChunkRadiation.get()) {
            return;
        }
        if (event.getServer().getTickCount() % HbmConfig.RADIATION.chunkUpdateInterval.get() != 0) {
            return;
        }
        event.getServer().getAllLevels().forEach(ChunkRadiationService::tick);
    }

    private HbmCommonEvents() {
    }
}
