package com.hbm.weapon.ballistics;

import com.hbm.HbmNuclearTech;
import com.hbm.network.WeaponEffectPayload;
import com.hbm.network.WeaponEffectType;
import com.hbm.weapon.HbmWeaponTags;
import com.hbm.weapon.data.AmmoDefinition;
import com.hbm.weapon.data.GunDefinition;
import com.hbm.world.damage.HbmDamageTypes;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.WeakHashMap;

/** Lightweight server trajectories: no Entity allocation for bullets or shotgun pellets. */
public final class BallisticsService {
    private static final Map<ServerLevel, List<ActiveRound>> ACTIVE = new WeakHashMap<>();
    private static final Map<ServerLevel, TickMetrics> METRICS = new WeakHashMap<>();
    private static final int MAX_ACTIVE_ROUNDS_PER_LEVEL = 8192;

    public static void fire(
            LivingEntity shooter,
            GunDefinition gun,
            AmmoDefinition ammo,
            Vec3 origin,
            Vec3 direction,
            double spreadDegrees
    ) {
        if (!(shooter.level() instanceof ServerLevel level)) {
            throw new IllegalArgumentException("Ballistic trajectories must originate on a server level");
        }
        List<ActiveRound> rounds = ACTIVE.computeIfAbsent(level, ignored -> new ArrayList<>());
        int availableSlots = Math.max(0, MAX_ACTIVE_ROUNDS_PER_LEVEL - rounds.size());
        int count = Math.min(ammo.getPelletCount(), availableSlots);
        RandomSource random = shooter.getRandom();
        for (int pellet = 0; pellet < count; pellet++) {
            Vec3 shotDirection = spreadDirection(direction, spreadDegrees * ammo.getSpreadMultiplier(), random);
            rounds.add(new ActiveRound(
                    shooter.getUUID(),
                    origin,
                    shotDirection.scale(gun.getMuzzleVelocity()),
                    gun.getMaxRange(),
                    gun.getBaseDamage() * ammo.getDamageMultiplier(),
                    gun.getHeadshotMultiplier(),
                    ammo.getArmorPenetration(),
                    ammo.getGravity(),
                    ammo.getDrag(),
                    gun.getId(),
                    ammo.getId(),
                    ammo.getImpactEffect(),
                    ammo.getTracerColor()
            ));
        }
        if (count < ammo.getPelletCount()) {
            HbmNuclearTech.LOGGER.warn("Dropped {} HBM trajectories at the per-level safety cap.", ammo.getPelletCount() - count);
        }
    }

    public static void tick(ServerLevel level) {
        TickMetrics timing = METRICS.get(level);
        long started = timing == null ? 0L : System.nanoTime();
        try {
            List<ActiveRound> rounds = ACTIVE.get(level);
            if (rounds == null || rounds.isEmpty()) {
                return;
            }

            Iterator<ActiveRound> iterator = rounds.iterator();
            while (iterator.hasNext()) {
                ActiveRound round = iterator.next();
                if (!advance(level, round)) {
                    iterator.remove();
                }
            }
            if (rounds.isEmpty()) {
                ACTIVE.remove(level);
            }
        } finally {
            if (timing != null) {
                timing.record(System.nanoTime() - started);
            }
        }
    }

    public static void clear(ServerLevel level) {
        ACTIVE.remove(level);
        METRICS.remove(level);
    }

    public static int activeRoundCount(ServerLevel level) {
        List<ActiveRound> rounds = ACTIVE.get(level);
        return rounds == null ? 0 : rounds.size();
    }

    /** Returns only trajectories owned by one shooter, avoiding cross-session/test interference. */
    public static int activeRoundCount(ServerLevel level, UUID shooterId) {
        List<ActiveRound> rounds = ACTIVE.get(level);
        if (rounds == null) {
            return 0;
        }
        return (int) rounds.stream().filter(round -> round.shooterId.equals(shooterId)).count();
    }

    public static void resetMetrics(ServerLevel level) {
        METRICS.put(level, new TickMetrics());
    }

    public static long measuredTickCount(ServerLevel level) {
        TickMetrics metrics = METRICS.get(level);
        return metrics == null ? 0L : metrics.samples;
    }

    public static double averageTickMillis(ServerLevel level) {
        TickMetrics metrics = METRICS.get(level);
        return metrics == null || metrics.samples == 0L
                ? 0.0D
                : metrics.totalNanos / (metrics.samples * 1_000_000.0D);
    }

    private static boolean advance(ServerLevel level, ActiveRound round) {
        Entity shooter = level.getEntity(round.shooterId);
        if (!(shooter instanceof LivingEntity) || !shooter.isAlive()) {
            return false;
        }

        Vec3 start = round.position;
        Vec3 end = start.add(round.velocity);
        double stepDistance = start.distanceTo(end);
        if (stepDistance <= 1.0E-5 || round.remainingRange <= 0.0D) {
            return false;
        }
        if (stepDistance > round.remainingRange) {
            end = start.add(round.velocity.normalize().scale(round.remainingRange));
            stepDistance = round.remainingRange;
        }
        if (!level.hasChunkAt(BlockPos.containing(end))) {
            return false;
        }

        BlockHitResult blockHit = level.clip(new ClipContext(
                start, end, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, shooter));
        EntityHitResult entityHit = ProjectileUtil.getEntityHitResult(
                shooter,
                start,
                end,
                new AABB(start, end).inflate(0.35D),
                candidate -> candidate.isAlive() && candidate.isPickable()
                        && !candidate.isSpectator() && candidate != shooter,
                Double.MAX_VALUE
        );

        HitResult nearest = nearest(start, blockHit, entityHit);
        broadcastTracer(level, round, start, nearest.getType() == HitResult.Type.MISS ? end : nearest.getLocation());

        if (nearest instanceof EntityHitResult hit) {
            applyEntityHit(level, shooter, hit, round);
            return false;
        }
        if (nearest instanceof BlockHitResult hit && hit.getType() != HitResult.Type.MISS) {
            BlockState state = level.getBlockState(hit.getBlockPos());
            if (state.is(HbmWeaponTags.FRAGILE_TO_GUNFIRE)) {
                level.destroyBlock(hit.getBlockPos(), true, shooter);
            }
            broadcastEffect(level, round, WeaponEffectType.IMPACT, hit.getLocation(), round.impactEffect, 0);
            return false;
        }

        round.position = end;
        round.remainingRange -= stepDistance;
        round.velocity = round.velocity.scale(1.0D - round.drag).add(0.0D, -round.gravity, 0.0D);
        return round.remainingRange > 0.0D;
    }

    private static HitResult nearest(Vec3 start, BlockHitResult block, EntityHitResult entity) {
        if (entity == null) {
            return block;
        }
        if (block.getType() == HitResult.Type.MISS) {
            return entity;
        }
        return start.distanceToSqr(entity.getLocation()) < start.distanceToSqr(block.getLocation()) ? entity : block;
    }

    private static void applyEntityHit(ServerLevel level, Entity shooter, EntityHitResult hit, ActiveRound round) {
        Entity target = hit.getEntity();
        boolean headshot = target instanceof LivingEntity living
                && !target.getType().is(HbmWeaponTags.NO_HEADSHOTS)
                && hit.getLocation().y >= living.getBoundingBox().minY + living.getBbHeight() * 0.72D;
        float totalDamage = round.damage * (headshot ? round.headshotMultiplier : 1.0F);
        target.hurt(headshot
                ? HbmDamageTypes.headshot(level, shooter, round.armorPenetration)
                : HbmDamageTypes.gunfire(level, shooter, round.armorPenetration), totalDamage);
        broadcastEffect(level, round, headshot ? WeaponEffectType.HEADSHOT : WeaponEffectType.IMPACT,
                hit.getLocation(), round.impactEffect, headshot ? 1 : 0);
    }

    private static void broadcastTracer(ServerLevel level, ActiveRound round, Vec3 start, Vec3 end) {
        if (round.tracerColor == 0) {
            return;
        }
        Vec3 delta = end.subtract(start);
        WeaponEffectPayload payload = new WeaponEffectPayload(
                WeaponEffectType.TRACER,
                round.gunId,
                round.ammoId,
                start.x,
                start.y,
                start.z,
                (float) delta.x,
                (float) delta.y,
                -1,
                round.tracerColor
        );
        PacketDistributor.sendToPlayersNear(level, null, start.x, start.y, start.z, 160.0D, payload);
    }

    private static void broadcastEffect(ServerLevel level, ActiveRound round, WeaponEffectType type,
                                        Vec3 position, ResourceLocation resource, int variant) {
        Entity shooter = level.getEntity(round.shooterId);
        int sourceEntityId = shooter == null ? -1 : shooter.getId();
        PacketDistributor.sendToPlayersNear(level, null, position.x, position.y, position.z, 96.0D,
                new WeaponEffectPayload(type, round.gunId, resource,
                        position.x, position.y, position.z, 0.0F, 0.0F, sourceEntityId, variant));
    }

    public static Vec3 spreadDirection(Vec3 direction, double spreadDegrees, RandomSource random) {
        if (spreadDegrees <= 0.0D) {
            return direction.normalize();
        }
        double deviation = Math.tan(Math.toRadians(spreadDegrees));
        Vec3 jitter = new Vec3(
                random.nextGaussian() * deviation,
                random.nextGaussian() * deviation,
                random.nextGaussian() * deviation
        );
        return direction.normalize().add(jitter).normalize();
    }

    private static final class ActiveRound {
        private final java.util.UUID shooterId;
        private Vec3 position;
        private Vec3 velocity;
        private double remainingRange;
        private final float damage;
        private final float headshotMultiplier;
        private final float armorPenetration;
        private final double gravity;
        private final double drag;
        private final ResourceLocation gunId;
        private final ResourceLocation ammoId;
        private final ResourceLocation impactEffect;
        private final int tracerColor;

        private ActiveRound(java.util.UUID shooterId, Vec3 position, Vec3 velocity, double remainingRange,
                            float damage, float headshotMultiplier, float armorPenetration,
                            double gravity, double drag, ResourceLocation gunId,
                            ResourceLocation ammoId, ResourceLocation impactEffect, int tracerColor) {
            this.shooterId = shooterId;
            this.position = position;
            this.velocity = velocity;
            this.remainingRange = remainingRange;
            this.damage = damage;
            this.headshotMultiplier = headshotMultiplier;
            this.armorPenetration = armorPenetration;
            this.gravity = gravity;
            this.drag = drag;
            this.gunId = gunId;
            this.ammoId = ammoId;
            this.impactEffect = impactEffect;
            this.tracerColor = tracerColor;
        }
    }

    private BallisticsService() {
    }

    private static final class TickMetrics {
        private long totalNanos;
        private long samples;

        private void record(long nanos) {
            totalNanos += Math.max(0L, nanos);
            samples++;
        }
    }
}
