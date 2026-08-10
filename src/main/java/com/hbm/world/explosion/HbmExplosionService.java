package com.hbm.world.explosion;

import com.hbm.HbmNuclearTech;
import com.hbm.config.HbmConfig;
import com.hbm.network.ClientEffectPayload;
import com.hbm.registry.HbmSounds;
import com.hbm.world.damage.HbmDamageTypes;
import com.hbm.world.radiation.ChunkRadiationService;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.Ocelot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.network.PacketDistributor;
import org.jetbrains.annotations.Nullable;

public final class HbmExplosionService {
    public static final ResourceLocation PROTOTYPE_NUKE_EFFECT = ResourceLocation.fromNamespaceAndPath(HbmNuclearTech.MOD_ID, "prototype_nuke");
    public static final ResourceLocation LITTLE_BOY_EFFECT = ResourceLocation.fromNamespaceAndPath(HbmNuclearTech.MOD_ID, "little_boy");
    public static final ResourceLocation MUKE_SOUND = ResourceLocation.fromNamespaceAndPath(HbmNuclearTech.MOD_ID, "weapon.mukeexplosion");
    private static final ResourceLocation VANILLA_EXPLOSION_SOUND = ResourceLocation.parse("minecraft:entity.generic.explode");
    private static final ResourceLocation NO_CLIENT_EFFECT = ResourceLocation.fromNamespaceAndPath(HbmNuclearTech.MOD_ID, "none");

    public static HbmExplosionProfile hbmTntProfile() {
        return new HbmExplosionProfile(
                HbmExplosionMode.VANILLA,
                HbmExplosionExecution.IMMEDIATE,
                HbmExplosionTerrainAlgorithm.EXPLOSION_NT,
                HbmConfig.BOMBS.hbmTntStrength.get(),
                0F,
                1F,
                0,
                0,
                0F,
                0F,
                0,
                0,
                0F,
                0F,
                false,
                HbmFalloutProfile.none(),
                NO_CLIENT_EFFECT,
                NO_CLIENT_EFFECT
        );
    }

    public static HbmExplosionProfile prototypeNukeProfile() {
        int terrainStrength = HbmConfig.BOMBS.prototypeNukeTerrainRadius.get();
        return new HbmExplosionProfile(
                HbmExplosionMode.NUCLEAR_RAY,
                HbmExplosionExecution.forTerrainStrength(
                        terrainStrength,
                        HbmConfig.BOMBS.nuclearImmediateTerrainStrength.get()
                ),
                HbmExplosionTerrainAlgorithm.EXPLOSION_NT,
                terrainStrength,
                0F,
                1F,
                HbmConfig.BOMBS.prototypeNukeRayResolution.get(),
                0,
                HbmConfig.BOMBS.prototypeNukeKillRadius.get(),
                HbmConfig.BOMBS.prototypeNukeMaxDamage.get().floatValue(),
                HbmConfig.BOMBS.prototypeNukeRadiationLevel.get(),
                0,
                0F,
                0F,
                true,
                HbmFalloutProfile.none(),
                MUKE_SOUND,
                PROTOTYPE_NUKE_EFFECT
        );
    }

    public static void detonatePrototypeNuke(ServerLevel level, BlockPos pos, @Nullable Entity owner) {
        detonate(level, pos, owner, prototypeNukeProfile());
    }

    public static HbmExplosionProfile nukeBoyProfile() {
        int radius = HbmConfig.BOMBS.nukeBoyRadius.get();
        boolean useHybrid = HbmConfig.BOMBS.nukeBoyUseHybridPlanner.get();
        return new HbmExplosionProfile(
                HbmExplosionMode.NUCLEAR_RAY,
                HbmExplosionExecution.BATCHED,
                useHybrid
                        ? HbmExplosionTerrainAlgorithm.MK5_HYBRID_RADIAL
                        : HbmExplosionTerrainAlgorithm.MK5_GENERALIZED_SPIRAL,
                HbmExplosionMath.mk5TerrainStrength(radius),
                radius,
                useHybrid ? HbmConfig.BOMBS.nukeBoyCraterDepthMultiplier.get().floatValue() : 1F,
                0,
                HbmConfig.BOMBS.nukeBoyRayCount.get(),
                radius * 2F,
                250F,
                0,
                HbmConfig.BOMBS.nukeBoyRadiationBurstTicks.get(),
                HbmConfig.BOMBS.nukeBoyRadiationBurstBaseDose.get().floatValue(),
                radius * 2F,
                true,
                HbmFalloutProfile.mk5(radius),
                VANILLA_EXPLOSION_SOUND,
                LITTLE_BOY_EFFECT
        );
    }

    public static void detonateNukeBoy(ServerLevel level, BlockPos pos, @Nullable Entity owner) {
        detonate(level, pos, owner, nukeBoyProfile());
    }

    public static void detonateHbmTnt(ServerLevel level, double x, double y, double z, @Nullable Entity owner) {
        HbmExplosionProfile profile = hbmTntProfile();
        level.explode(owner, x, y, z, profile.terrainStrength(), Level.ExplosionInteraction.TNT);
    }

    public static void detonate(ServerLevel level, BlockPos pos, @Nullable Entity owner, HbmExplosionProfile profile) {
        level.removeBlock(pos, false);
        if (profile.mode() == HbmExplosionMode.VANILLA) {
            detonateHbmTnt(level, pos.getX() + 0.5D, pos.getY() + 0.5D, pos.getZ() + 0.5D, owner);
            return;
        }

        Vec3 origin = Vec3.atCenterOf(pos);
        applyLowYieldFallout(level, pos, profile);
        playNuclearSound(level, origin, profile);
        HbmNuclearExplosionJob terrainJob = new HbmNuclearExplosionJob(pos, profile, level.getRandom().nextLong());
        HbmNuclearExplosionSavedData savedData = HbmNuclearExplosionSavedData.get(level);
        enqueueWaterVaporization(savedData, pos, profile);
        if (profile.terrainExecution() == HbmExplosionExecution.IMMEDIATE) {
            terrainJob.executeImmediately(level);
            savedData.enqueueFallout(terrainJob.createFalloutRainJob());
        } else {
            savedData.enqueue(terrainJob);
        }
        triggerClientEffect(level, pos, profile);
    }

    public static void tick(MinecraftServer server) {
        for (ServerLevel level : server.getAllLevels()) {
            HbmNuclearExplosionSavedData.get(level).tick(level);
        }
    }

    /** Source MK5's active heat and blast wave, called once per running explosion tick. */
    static void applyNuclearDamage(ServerLevel level, Vec3 origin, HbmExplosionProfile profile) {
        if (profile.killRadius() <= 0F || profile.maxDamage() <= 0F) {
            return;
        }

        AABB bounds = new AABB(origin, origin).inflate(profile.killRadius());
        for (LivingEntity entity : level.getEntitiesOfClass(LivingEntity.class, bounds)) {
            if (entity.isRemoved() || isNuclearBlastExempt(entity)) {
                continue;
            }

            double distance = entity.position().distanceTo(origin);
            double damage = HbmExplosionMath.nuclearDamage(distance, profile.killRadius(), profile.maxDamage());
            if (damage <= 0D || isObstructed(level, origin, entity)) {
                continue;
            }

            boolean hurt = entity.hurt(HbmDamageTypes.nuclearBlast(level), (float) damage);
            entity.igniteForSeconds(5);
            if (hurt) {
                Vec3 knockback = entity.getEyePosition().subtract(origin);
                if (knockback.lengthSqr() > 0.000001D) {
                    knockback = knockback.normalize().scale(0.2D);
                    entity.push(knockback.x, knockback.y, knockback.z);
                }
            }
        }
    }

    private static void enqueueWaterVaporization(HbmNuclearExplosionSavedData savedData, BlockPos pos,
            HbmExplosionProfile profile) {
        if (!profile.fallout().isEnabled() || !HbmConfig.BOMBS.nukeBoyVaporizeWater.get()) {
            return;
        }
        int radius = Math.max(0, Math.round(profile.fallout().radius()
                * HbmConfig.BOMBS.nukeBoyWaterVaporRadiusMultiplier.get().floatValue()));
        if (radius > 0) {
            savedData.enqueueWaterVaporization(new HbmWaterVaporizationJob(
                    pos,
                    radius,
                    Math.max(1, Math.round(profile.maxTerrainDistance())),
                    HbmConfig.BOMBS.nukeBoyWaterVaporTransitionBlocks.get()
            ));
        }
    }

    private static boolean isNuclearBlastExempt(Entity entity) {
        return entity instanceof Ocelot || entity instanceof Player player && player.getAbilities().instabuild;
    }

    private static boolean isObstructed(ServerLevel level, Vec3 origin, Entity target) {
        HitResult hit = level.clip(new ClipContext(origin, target.getEyePosition(), ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, target));
        return hit.getType() != HitResult.Type.MISS;
    }

    private static void applyLowYieldFallout(ServerLevel level, BlockPos pos, HbmExplosionProfile profile) {
        ChunkPos center = new ChunkPos(pos);
        for (Map.Entry<HbmNuclearFallout.ChunkOffset, Double> entry : HbmNuclearFallout.lowYieldDistribution(profile.radiationLevel()).entrySet()) {
            HbmNuclearFallout.ChunkOffset offset = entry.getKey();
            ChunkRadiationService.incrementRad(level, new ChunkPos(center.x + offset.x(), center.z + offset.z()), entry.getValue());
        }
    }

    private static void playNuclearSound(ServerLevel level, Vec3 origin, HbmExplosionProfile profile) {
        if (profile.sound().equals(MUKE_SOUND)) {
            level.playSound(null, origin.x, origin.y, origin.z, HbmSounds.MUKE_EXPLOSION.get(), SoundSource.BLOCKS, 15F, 1F);
        } else if (profile.sound().equals(VANILLA_EXPLOSION_SOUND)) {
            level.playSound(null, origin.x, origin.y, origin.z, SoundEvents.GENERIC_EXPLODE, SoundSource.BLOCKS, 1F, 1F);
        }
    }

    private static void triggerClientEffect(ServerLevel level, BlockPos pos, HbmExplosionProfile profile) {
        if (profile.clientEffect().equals(NO_CLIENT_EFFECT)) {
            return;
        }
        PacketDistributor.sendToPlayersNear(
                level,
                null,
                pos.getX() + 0.5D,
                pos.getY() + 0.5D,
                pos.getZ() + 0.5D,
                Math.max(64D, profile.terrainStrength() * 8D),
                new ClientEffectPayload(profile.clientEffect(), pos.getX() + 0.5D, pos.getY() + 0.5D, pos.getZ() + 0.5D, Math.round(profile.terrainStrength()))
        );
    }

    private HbmExplosionService() {
    }
}
