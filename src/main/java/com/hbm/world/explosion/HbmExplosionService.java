package com.hbm.world.explosion;

import com.hbm.HbmNuclearTech;
import com.hbm.config.HbmConfig;
import com.hbm.network.ClientEffectPayload;
import com.hbm.world.radiation.ChunkRadiationService;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.network.PacketDistributor;
import org.jetbrains.annotations.Nullable;

public final class HbmExplosionService {
    public static final ResourceLocation PROTOTYPE_NUKE_EFFECT = ResourceLocation.fromNamespaceAndPath(HbmNuclearTech.MOD_ID, "prototype_nuke");

    public static HbmExplosionProfile prototypeNukeProfile() {
        return new HbmExplosionProfile(
                HbmConfig.BOMBS.prototypeNukeRadius.get().floatValue(),
                HbmConfig.FALLOUT.prototypeNukeFallout.get(),
                Level.ExplosionInteraction.TNT,
                PROTOTYPE_NUKE_EFFECT
        );
    }

    public static void detonatePrototypeNuke(ServerLevel level, BlockPos pos, @Nullable Entity owner) {
        detonate(level, pos, owner, prototypeNukeProfile());
    }

    public static void detonate(ServerLevel level, BlockPos pos, @Nullable Entity owner, HbmExplosionProfile profile) {
        level.removeBlock(pos, false);
        level.explode(owner, pos.getX() + 0.5D, pos.getY() + 0.5D, pos.getZ() + 0.5D, profile.radius(), profile.blockInteraction());
        applyFallout(level, pos, profile);
        triggerClientEffect(level, pos, profile);
    }

    private static void applyFallout(ServerLevel level, BlockPos pos, HbmExplosionProfile profile) {
        if (profile.fallout() <= 0D) {
            return;
        }

        int falloutRadius = Math.max(1, Math.round(profile.radius() / 4F));
        ChunkPos centerChunk = new ChunkPos(pos);
        for (int x = -falloutRadius; x <= falloutRadius; x++) {
            for (int z = -falloutRadius; z <= falloutRadius; z++) {
                double falloff = 1D / (1D + Math.abs(x) + Math.abs(z));
                ChunkRadiationService.incrementRad(level, new ChunkPos(centerChunk.x + x, centerChunk.z + z), profile.fallout() * falloff);
            }
        }
    }

    private static void triggerClientEffect(ServerLevel level, BlockPos pos, HbmExplosionProfile profile) {
        PacketDistributor.sendToPlayersNear(
                level,
                null,
                pos.getX() + 0.5D,
                pos.getY() + 0.5D,
                pos.getZ() + 0.5D,
                Math.max(64D, profile.radius() * 8D),
                new ClientEffectPayload(profile.clientEffect(), pos.getX() + 0.5D, pos.getY() + 0.5D, pos.getZ() + 0.5D, Math.round(profile.radius()))
        );
    }

    private HbmExplosionService() {
    }
}
