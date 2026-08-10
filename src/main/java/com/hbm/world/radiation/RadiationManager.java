package com.hbm.world.radiation;

import com.hbm.config.HbmConfig;
import com.hbm.network.HbmPayloads;
import com.hbm.registry.HbmAttachments;
import com.hbm.registry.HbmFluids;
import java.util.EnumMap;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

public final class RadiationManager {
    public static void tickPlayer(ServerPlayer player) {
        if (!(player.level() instanceof ServerLevel level)) {
            return;
        }

        RadiationPlayerState state = getPlayerState(player);
        migrateLegacyExposure(player, state);
        tickLiving(level, player, state, HbmConfig.RADIATION_TICK_INTERVAL.get(), true);

        player.setData(HbmAttachments.PLAYER_RADIATION.get(), state);
        if (player.tickCount % HbmConfig.RADIATION.clientSyncInterval.get() == 0) {
            HbmPayloads.syncRadiation(player);
        }
    }

    public static void tickMob(Mob mob) {
        if (!(mob.level() instanceof ServerLevel level) || !HbmConfig.RADIATION.enableMobRadiation.get() || isRadiationImmune(mob)) {
            return;
        }

        RadiationPlayerState state = getMobState(mob);
        tickLiving(level, mob, state, HbmConfig.RADIATION.mobTickInterval.get(), false);
        mob.setData(HbmAttachments.MOB_RADIATION.get(), state);
    }

    public static double getExposure(ServerPlayer player) {
        return getPlayerState(player).getRadiation();
    }

    public static void reduceExposure(ServerPlayer player, double amount) {
        RadiationPlayerState state = getPlayerState(player);
        state.removeRadiation(amount);
        player.setData(HbmAttachments.PLAYER_RADIATION.get(), state);
        HbmPayloads.syncRadiation(player);
    }

    public static void beginTreatment(ServerPlayer player, int ticks, double reductionPerTick) {
        RadiationPlayerState state = getPlayerState(player);
        state.startTreatment(ticks, reductionPerTick);
        player.setData(HbmAttachments.PLAYER_RADIATION.get(), state);
        HbmPayloads.syncRadiation(player);
    }

    public static void activateRadX(ServerPlayer player) {
        RadiationPlayerState state = getPlayerState(player);
        state.startRadX(HbmConfig.RADIATION.radXDuration.get());
        player.setData(HbmAttachments.PLAYER_RADIATION.get(), state);
        HbmPayloads.syncRadiation(player);
    }

    public static void cancelTreatmentAndRadX(ServerPlayer player) {
        RadiationPlayerState state = getPlayerState(player);
        state.cancelTreatmentAndRadX();
        player.setData(HbmAttachments.PLAYER_RADIATION.get(), state);
        HbmPayloads.syncRadiation(player);
    }

    public static void clear(ServerPlayer player) {
        RadiationPlayerState state = getPlayerState(player);
        state.clear();
        player.setData(HbmAttachments.PLAYER_RADIATION.get(), state);
        HbmPayloads.syncRadiation(player);
    }

    public static void applyDirectExposure(ServerPlayer player, RadiationSourceType source, double amount, boolean bypassProtection) {
        RadiationPlayerState state = getPlayerState(player);
        if (!applyDirectExposure(player, state, source, amount, bypassProtection)) {
            return;
        }
        player.setData(HbmAttachments.PLAYER_RADIATION.get(), state);
        HbmPayloads.syncRadiation(player);
    }

    public static void applyDirectExposure(Mob mob, RadiationSourceType source, double amount, boolean bypassProtection) {
        RadiationPlayerState state = getMobState(mob);
        if (!applyDirectExposure(mob, state, source, amount, bypassProtection)) {
            return;
        }
        mob.setData(HbmAttachments.MOB_RADIATION.get(), state);
    }

    public static void applyDirectExposure(LivingEntity entity, RadiationSourceType source, double amount,
            boolean bypassProtection) {
        if (entity instanceof ServerPlayer player) {
            applyDirectExposure(player, source, amount, bypassProtection);
        } else if (entity instanceof Mob mob) {
            applyDirectExposure(mob, source, amount, bypassProtection);
        }
    }

    public static RadiationDiagnostics getDiagnostics(ServerPlayer player) {
        RadiationPlayerState state = getPlayerState(player);
        return createDiagnostics(state, getResistance(player, state));
    }

    public static RadiationDiagnostics getDiagnostics(Mob mob) {
        return createDiagnostics(getMobState(mob), 0D);
    }

    public static double getEnvironmentRate(ServerPlayer player) {
        return getPlayerState(player).getEnvironmentRate();
    }

    public static boolean isRadiationImmune(Mob mob) {
        return mob.getType().is(HbmTags.EntityTypes.RADIATION_IMMUNE);
    }

    private static void tickLiving(ServerLevel level, LivingEntity entity, RadiationPlayerState state, int interval, boolean playerControlled) {
        Map<RadiationSourceType, Double> rates = collectRates(level, entity);
        state.setRecentRates(rates);
        double resistance = getResistance(entity, state);

        if (HbmConfig.RADIATION.enableContamination.get() && !isCreativePlayer(entity)) {
            double appliedDose = RadiationMath.radiationForTicks(state.getTotalRate(), interval, resistance);
            state.addRadiation(appliedDose);
            state.removeRadiation(HbmConfig.RADIATION_DECAY_PER_TICK.get() * interval);
            if (playerControlled) {
                state.removeRadiation(state.tickTreatment(interval));
            }
        }
        if (playerControlled) {
            state.tickRadX(interval);
        }

        if (HbmConfig.RADIATION.enablePlayerEffects.get()) {
            RadiationEffects.apply(entity, state, interval);
        }
    }

    private static boolean applyDirectExposure(LivingEntity entity, RadiationPlayerState state, RadiationSourceType source, double amount, boolean bypassProtection) {
        if (!HbmConfig.RADIATION.enableContamination.get() || amount <= 0D || isCreativePlayer(entity)) {
            return false;
        }
        if (entity instanceof Mob mob && (!HbmConfig.RADIATION.enableMobRadiation.get() || isRadiationImmune(mob))) {
            return false;
        }

        double resistance = bypassProtection ? 0D : getResistance(entity, state);
        double sourceMultiplier = source == RadiationSourceType.EXPLOSION
                ? HbmConfig.RADIATION.explosionSourceMultiplier.get()
                : 1D;
        double appliedDose = amount * sourceMultiplier * RadiationMath.resistanceMultiplier(resistance);
        state.addRadiation(appliedDose);
        state.recordDirectDose(source, appliedDose);
        return true;
    }

    private static RadiationDiagnostics createDiagnostics(RadiationPlayerState state, double resistance) {
        return new RadiationDiagnostics(
                state.getRadiation(),
                state.getTotalRate(),
                state.getRate(RadiationSourceType.INVENTORY),
                state.getRate(RadiationSourceType.BLOCK),
                state.getRate(RadiationSourceType.FALLOUT),
                state.getRate(RadiationSourceType.EXPLOSION),
                state.getLastDirectDose(RadiationSourceType.EXPLOSION),
                state.getRate(RadiationSourceType.DIMENSION),
                state.getRate(RadiationSourceType.SCRIPTED),
                resistance,
                RadiationMath.resistanceMultiplier(resistance)
        );
    }

    private static RadiationPlayerState getPlayerState(ServerPlayer player) {
        return player.getData(HbmAttachments.PLAYER_RADIATION.get());
    }

    private static RadiationPlayerState getMobState(Mob mob) {
        return mob.getData(HbmAttachments.MOB_RADIATION.get());
    }

    private static void migrateLegacyExposure(ServerPlayer player, RadiationPlayerState state) {
        if (state.getRadiation() > 0D) {
            return;
        }
        double legacyExposure = RadiationSavedData.getLegacyExposure(player.serverLevel(), player.getUUID());
        if (legacyExposure > 0D) {
            state.setRadiation(legacyExposure);
            RadiationSavedData.consumeLegacyExposure(player.serverLevel(), player.getUUID());
        }
    }

    private static Map<RadiationSourceType, Double> collectRates(ServerLevel level, LivingEntity entity) {
        Map<RadiationSourceType, Double> rates = new EnumMap<>(RadiationSourceType.class);
        double inventoryRate = entity instanceof ServerPlayer player
                ? scanInventory(player)
                : entity instanceof Mob mob ? scanEquipment(mob) : 0D;
        double localBlockRate = scanAmbientBlocks(level, entity);
        if (isInContaminatedWater(entity)) {
            localBlockRate += HbmConfig.RADIATION.contaminatedWaterImmersionRate.get();
        }
        rates.put(RadiationSourceType.BLOCK, localBlockRate * HbmConfig.RADIATION.blockSourceMultiplier.get());
        rates.put(RadiationSourceType.INVENTORY, inventoryRate * HbmConfig.RADIATION.inventorySourceMultiplier.get());
        rates.put(RadiationSourceType.FALLOUT, HbmConfig.RADIATION.enableChunkRadiation.get()
                ? ChunkRadiationService.getRadiation(level, entity.blockPosition()) * HbmConfig.RADIATION.falloutSourceMultiplier.get()
                : 0D);
        rates.put(RadiationSourceType.DIMENSION, level.dimension() == Level.NETHER
                ? HbmConfig.RADIATION.netherAmbientRate.get()
                : 0D);
        rates.put(RadiationSourceType.EXPLOSION, 0D);
        rates.put(RadiationSourceType.SCRIPTED, 0D);
        return rates;
    }

    private static double scanAmbientBlocks(ServerLevel level, LivingEntity entity) {
        int radius = HbmConfig.AMBIENT_RADIATION_RADIUS.get();
        double dose = 0D;
        BlockPos center = entity.blockPosition();
        double entityY = entity.getY() + entity.getBbHeight() / 2D;
        for (BlockPos pos : BlockPos.betweenClosed(center.offset(-radius, -1, -radius), center.offset(radius, 1, radius))) {
            double sourceRate = RadiationSources.fromBlock(level.getBlockState(pos));
            if (sourceRate <= 0D) {
                continue;
            }
            double distance = RadiationMath.distanceToUnitCube(
                    entity.getX(),
                    entityY,
                    entity.getZ(),
                    pos.getX(),
                    pos.getY(),
                    pos.getZ()
            );
            dose += sourceRate * RadiationMath.localEmitterAttenuation(
                    distance,
                    radius,
                    HbmConfig.RADIATION.ambientBlockFalloffExponent.get()
            );
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

    private static double scanEquipment(Mob mob) {
        double dose = 0D;
        for (ItemStack stack : mob.getHandSlots()) {
            dose += RadiationSources.fromStack(stack);
        }
        for (ItemStack stack : mob.getArmorSlots()) {
            dose += RadiationSources.fromStack(stack);
        }
        return dose;
    }

    private static boolean isInContaminatedWater(LivingEntity entity) {
        BlockPos feet = entity.blockPosition();
        BlockPos eyes = BlockPos.containing(entity.getX(), entity.getEyeY(), entity.getZ());
        return isContaminatedWater(entity.level().getFluidState(feet).getType())
                || isContaminatedWater(entity.level().getFluidState(eyes).getType());
    }

    private static boolean isContaminatedWater(net.minecraft.world.level.material.Fluid fluid) {
        return fluid == HbmFluids.CONTAMINATED_WATER.get()
                || fluid == HbmFluids.FLOWING_CONTAMINATED_WATER.get();
    }

    private static boolean isCreativePlayer(LivingEntity entity) {
        return entity instanceof ServerPlayer player && player.getAbilities().instabuild;
    }

    private static double getResistance(LivingEntity entity, RadiationPlayerState state) {
        return entity instanceof ServerPlayer player ? RadiationProtectionRegistry.getResistance(player, state) : 0D;
    }

    private RadiationManager() {
    }
}
