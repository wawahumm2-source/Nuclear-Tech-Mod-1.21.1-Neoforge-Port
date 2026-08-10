package com.hbm.world.explosion;

import com.hbm.HbmNuclearTech;
import com.hbm.config.HbmConfig;
import com.hbm.world.radiation.RadiationManager;
import com.hbm.world.radiation.RadiationSourceType;
import java.util.ArrayList;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.LongArrayTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * Main-thread, resumable terrain scheduler for HBM nuclear profiles.
 *
 * <p>The original MK5 batches rays by chunks. This port preserves block-level
 * resistance math while storing targets in compact, real-chunk bitsets. It
 * force-loads only bounded work frontiers, then releases every ticket once its
 * chunk is complete.</p>
 */
final class HbmNuclearExplosionJob {
    private static final Map<RayKey, List<RayDirection>> DIRECTION_CACHE = new HashMap<>();

    private final BlockPos origin;
    private final HbmExplosionProfile profile;
    private final long seed;
    private final UUID ticketOwner;
    private final List<Long> craterChunks;
    private final Set<Long> activeTickets = new LinkedHashSet<>();
    private final Map<Long, HbmNuclearChunkTargets> plannedTargetsByChunk = new HashMap<>();
    private final Map<BlockState, Float> mk5VanillaResistanceCache = new IdentityHashMap<>();
    private final List<Long> destructionChunks = new ArrayList<>();
    private final BlockPos.MutableBlockPos tracePosition = new BlockPos.MutableBlockPos();

    private int craterPreloadIndex;
    private int craterReadyIndex;
    private int rayIndex;
    private int destructionChunkIndex;
    private int destructionPrefetchIndex;
    private int radiationBurstTick;
    private double gspHeight;
    private double gspAzimuth;
    private boolean exactFallback;
    private Phase phase;
    private HbmBlastTerrainJob blastTerrain;
    private transient HbmNuclearTerrainSnapshot.Builder snapshotBuilder;
    private transient HbmNuclearResistanceVolume workerSnapshot;
    private transient CompletableFuture<HbmHybridExplosionPlanner.Result> hybridPlanFuture;
    private transient Deque<SourceBatchTask> sourceBatches;
    private transient int sourceSubmitRayIndex = -1;
    private transient long sourcePlanningNanos;
    private transient long snapshotCaptureStartedNanos;
    private transient boolean restartSnapshotTickets;
    private transient boolean restartDestructionTickets;

    HbmNuclearExplosionJob(BlockPos origin, HbmExplosionProfile profile, long seed) {
        this(origin, profile, seed, UUID.randomUUID(), 0, 0, 0, 0, 0, 0, -1D, 0D, Phase.PRELOADING);
    }

    private HbmNuclearExplosionJob(BlockPos origin, HbmExplosionProfile profile, long seed, UUID ticketOwner,
            int craterPreloadIndex, int craterReadyIndex, int rayIndex, int destructionChunkIndex, int destructionPrefetchIndex,
            int radiationBurstTick, double gspHeight, double gspAzimuth, Phase phase) {
        this.origin = origin.immutable();
        this.profile = profile;
        this.seed = seed;
        this.ticketOwner = ticketOwner;
        this.craterChunks = HbmNuclearChunkMath.orderedCircle(this.origin, planningRadius(profile));
        this.craterPreloadIndex = craterPreloadIndex;
        this.craterReadyIndex = craterReadyIndex;
        this.rayIndex = rayIndex;
        this.destructionChunkIndex = destructionChunkIndex;
        this.destructionPrefetchIndex = destructionPrefetchIndex;
        this.radiationBurstTick = radiationBurstTick;
        this.gspHeight = gspHeight;
        this.gspAzimuth = gspAzimuth;
        this.phase = phase;
        if (profile.fallout().terrainTransformation()) {
            this.blastTerrain = new HbmBlastTerrainJob(this.origin, profile.fallout(), seed);
        }
    }

    int preload(ServerLevel level, HbmNuclearChunkLoadBudget ticketBudget) {
        if (this.phase != Phase.PRELOADING) {
            return 0;
        }

        if (this.restartSnapshotTickets) {
            releaseAllTickets(level);
            this.craterPreloadIndex = 0;
            this.craterReadyIndex = 0;
            this.restartSnapshotTickets = false;
            return 0;
        }

        // Worker-planned MK5 jobs are admitted by the shared scheduler. Their snapshot
        // capture owns a bounded chunk window instead of force-loading the whole crater.
        if (awaitsWorkerPlanningSlot()) {
            return 0;
        }

        int used = 0;
        if (!HbmConfig.BOMBS.forceLoadNuclearWork.get()) {
            this.craterPreloadIndex = this.craterChunks.size();
            beginPlanningPhase();
            return 0;
        }

        while (this.craterPreloadIndex < this.craterChunks.size() && ticketBudget.remaining() > 0) {
            long chunkKey = this.craterChunks.get(this.craterPreloadIndex);
            if (!ticketBudget.ensureForced(level, this.ticketOwner, chunkKey, this.activeTickets)) {
                break;
            }
            this.craterPreloadIndex++;
            used++;
        }

        while (this.craterPreloadIndex >= this.craterChunks.size()
                && this.craterReadyIndex < this.craterChunks.size()) {
            long chunkKey = this.craterChunks.get(this.craterReadyIndex);
            if (!level.hasChunk(ChunkPos.getX(chunkKey), ChunkPos.getZ(chunkKey))) {
                break;
            }
            this.craterReadyIndex++;
        }

        if (this.craterReadyIndex >= this.craterChunks.size()) {
            beginPlanningPhase();
        }
        return used;
    }

    int captureWorkerSnapshot(ServerLevel level, int sectionBudget, long deadlineNanos,
            HbmNuclearChunkLoadBudget ticketBudget) {
        if (this.phase != Phase.HYBRID_SNAPSHOT || sectionBudget <= 0) {
            return 0;
        }
        if (this.snapshotBuilder == null) {
            this.snapshotCaptureStartedNanos = System.nanoTime();
            this.snapshotBuilder = HbmNuclearTerrainSnapshot.builder(
                    level,
                    this.origin,
                    planningRadius(this.profile),
                    this.craterChunks
            );
        }

        prefetchSnapshot(level, ticketBudget);
        int used = this.snapshotBuilder.capture(level, sectionBudget, deadlineNanos);
        releaseCapturedSnapshotChunks(level);
        prefetchSnapshot(level, ticketBudget);
        if (!this.snapshotBuilder.isComplete()) {
            return used;
        }

        this.workerSnapshot = this.snapshotBuilder.build();
        this.snapshotBuilder = null;
        releaseAllTickets(level);
        HbmNuclearTech.LOGGER.info(
                "Nuclear snapshot completed at {} in {} ms using approximately {} MiB; workers active {}, queued {}",
                this.origin,
                (System.nanoTime() - this.snapshotCaptureStartedNanos) / 1_000_000L,
                this.workerSnapshot.estimatedBytes() / (1024L * 1024L),
                HbmNuclearPlanningService.activeWorkers(),
                HbmNuclearPlanningService.queuedJobs()
        );
        this.snapshotCaptureStartedNanos = 0L;
        this.phase = useHybridPlanner() ? Phase.HYBRID_WAITING : Phase.SOURCE_PLANNING;
        return used;
    }

    boolean pollWorkerPlanning(ServerLevel level) {
        if (this.phase == Phase.SOURCE_PLANNING) {
            return pollSourcePlanning(level);
        }
        if (this.phase != Phase.HYBRID_WAITING || this.workerSnapshot == null) {
            return false;
        }
        if (this.hybridPlanFuture == null) {
            HbmHybridExplosionPlanner.Parameters parameters = new HbmHybridExplosionPlanner.Parameters(
                    this.profile.terrainStrength(),
                    planningRadius(this.profile),
                    this.profile.craterDepthMultiplier(),
                    HbmConfig.BOMBS.nuclearHybridCubeResolution.get(),
                    this.seed
            );
            this.hybridPlanFuture = HbmNuclearPlanningService.submit(this.workerSnapshot, parameters);
            return false;
        }
        if (!this.hybridPlanFuture.isDone()) {
            return false;
        }
        try {
            HbmHybridExplosionPlanner.Result result = this.hybridPlanFuture.join();
            this.hybridPlanFuture = null;
            this.workerSnapshot = null;
            this.plannedTargetsByChunk.clear();
            for (HbmHybridChunkMask mask : result.targets().values()) {
                this.plannedTargetsByChunk.put(mask.chunkKey(), HbmNuclearChunkTargets.fromHybridMask(mask));
            }
            this.rayIndex = directionCount(this.profile);
            HbmNuclearTech.LOGGER.info(
                    "Hybrid nuclear plan completed at {} with {} targets in {} ms",
                    this.origin,
                    result.targetCount(),
                    result.planningNanos() / 1_000_000L
            );
            finishPlanning(level);
        } catch (CompletionException failure) {
            HbmNuclearTech.LOGGER.error(
                    "Hybrid nuclear planning failed at {}; continuing with source-density MK5 batches",
                    this.origin,
                    failure.getCause()
            );
            this.hybridPlanFuture = null;
            this.plannedTargetsByChunk.clear();
            this.destructionChunks.clear();
            this.rayIndex = 0;
            this.gspHeight = -1D;
            this.gspAzimuth = 0D;
            this.exactFallback = true;
            this.phase = Phase.SOURCE_PLANNING;
        }
        return true;
    }

    private boolean pollSourcePlanning(ServerLevel level) {
        if (this.workerSnapshot == null) {
            HbmNuclearTech.LOGGER.error(
                    "Source-density nuclear planner at {} lost its terrain snapshot; restarting capture",
                    this.origin
            );
            this.phase = Phase.PRELOADING;
            this.craterPreloadIndex = 0;
            this.craterReadyIndex = 0;
            return true;
        }

        int directionCount = directionCount(this.profile);
        if (this.sourceBatches == null) {
            this.sourceBatches = new ArrayDeque<>();
            this.sourceSubmitRayIndex = this.rayIndex;
        }
        if (this.sourceSubmitRayIndex < this.rayIndex) {
            this.sourceSubmitRayIndex = this.rayIndex;
        }

        boolean changed = false;
        SourceBatchTask completed = this.sourceBatches.peekFirst();
        if (completed != null && completed.future().isDone()) {
            try {
                HbmGspExplosionPlanner.BatchResult result = completed.future().join();
                if (result.startRay() != this.rayIndex || result.startRay() != completed.startRay()
                        || result.endRay() != completed.endRay()) {
                    throw new IllegalStateException("Out-of-order source-density nuclear batch");
                }
                mergeSourceBatch(result);
                this.sourceBatches.removeFirst();
                this.rayIndex = result.endRay();
                this.sourcePlanningNanos += result.planningNanos();
                changed = true;
            } catch (CompletionException | IllegalStateException failure) {
                HbmNuclearTech.LOGGER.error(
                        "Source-density nuclear batch {}-{} failed at {}; retrying without discarding the explosion",
                        completed.startRay(),
                        completed.endRay(),
                        this.origin,
                        failure instanceof CompletionException ? failure.getCause() : failure
                );
                for (SourceBatchTask task : this.sourceBatches) {
                    task.future().cancel(true);
                }
                this.sourceBatches.clear();
                this.sourceSubmitRayIndex = this.rayIndex;
                return false;
            }
        }

        if (this.rayIndex >= directionCount && this.sourceBatches.isEmpty()) {
            finishSourcePlanning(level);
            return true;
        }

        HbmGspExplosionPlanner.Parameters parameters = new HbmGspExplosionPlanner.Parameters(
                this.profile.terrainStrength(),
                planningRadius(this.profile),
                1F,
                directionCount
        );
        int perExplosionConcurrency = Math.max(1, HbmConfig.BOMBS.nuclearHybridWorkerThreads.get());
        while (this.sourceBatches.size() < perExplosionConcurrency
                && this.sourceSubmitRayIndex < directionCount) {
            int startRay = this.sourceSubmitRayIndex;
            int endRay = Math.min(
                    directionCount,
                    startRay + HbmConfig.BOMBS.nuclearSourceRayBatchSize.get()
            );
            CompletableFuture<HbmGspExplosionPlanner.BatchResult> submitted =
                    HbmNuclearPlanningService.submitSourceBatch(
                            this.workerSnapshot,
                            parameters,
                            startRay,
                            endRay
                    );
            if (submitted == null) {
                break;
            }
            this.sourceBatches.addLast(new SourceBatchTask(startRay, endRay, submitted));
            this.sourceSubmitRayIndex = endRay;
        }
        return changed;
    }

    private void mergeSourceBatch(HbmGspExplosionPlanner.BatchResult result) {
        for (HbmHybridChunkMask mask : result.targets().values()) {
            this.plannedTargetsByChunk.compute(mask.chunkKey(), (chunkKey, existing) -> {
                if (existing == null) {
                    return HbmNuclearChunkTargets.fromHybridMask(mask);
                }
                existing.merge(mask);
                return existing;
            });
        }
    }

    private void finishSourcePlanning(ServerLevel level) {
        int targetCount = this.plannedTargetsByChunk.values().stream()
                .mapToInt(HbmNuclearChunkTargets::targetCount)
                .sum();
        HbmNuclearTech.LOGGER.info(
                "Source-density MK5 plan completed at {} with {} targets across {} rays in {} ms of worker time",
                this.origin,
                targetCount,
                this.rayIndex,
                this.sourcePlanningNanos / 1_000_000L
        );
        if (this.sourceBatches != null) {
            this.sourceBatches.clear();
        }
        this.sourceSubmitRayIndex = -1;
        this.sourcePlanningNanos = 0L;
        this.workerSnapshot = null;
        finishPlanning(level);
    }

    int plan(ServerLevel level, int workBudget) {
        return plan(level, workBudget, Long.MAX_VALUE);
    }

    int plan(ServerLevel level, int workBudget, long deadlineNanos) {
        if (this.phase != Phase.PLANNING || workBudget <= 0) {
            return 0;
        }

        int directionCount = directionCount(this.profile);
        boolean mk5 = isMk5Algorithm(this.profile.terrainAlgorithm());
        float stoneResistance = mk5
                ? Blocks.STONE.defaultBlockState().getExplosionResistance(level, this.origin, null)
                : 0F;
        int used = 0;
        while (this.rayIndex < directionCount && used < workBudget && System.nanoTime() < deadlineNanos) {
            if (mk5) {
                double radial = Math.sqrt(Math.max(0D, 1D - this.gspHeight * this.gspHeight));
                used += traceMk5Ray(
                        level,
                        radial * Math.cos(this.gspAzimuth),
                        this.gspHeight,
                        radial * Math.sin(this.gspAzimuth),
                        stoneResistance
                );
            } else {
                RayDirection direction = directions(this.profile).get(this.rayIndex);
                used += traceExplosionNtRay(level, direction, rayRandom(this.rayIndex));
            }
            advanceMk5Direction(directionCount);
            this.rayIndex++;
        }

        if (this.rayIndex >= directionCount) {
            finishPlanning(level);
        }
        return used;
    }

    void executeImmediately(ServerLevel level) {
        if (this.phase != Phase.PRELOADING && this.phase != Phase.PLANNING) {
            return;
        }

        this.phase = Phase.PLANNING;
        plan(level, Integer.MAX_VALUE);
        planBlastTerrain(Integer.MAX_VALUE);
        beginDestroying();
        destroy(level, Integer.MAX_VALUE, Long.MAX_VALUE, new HbmNuclearChunkLoadBudget(Integer.MAX_VALUE));
        applyBlastTerrain(level, Integer.MAX_VALUE, Long.MAX_VALUE, new HbmNuclearChunkLoadBudget(Integer.MAX_VALUE));
    }

    int planBlastTerrain(int columnBudget) {
        return this.blastTerrain == null ? 0 : this.blastTerrain.plan(columnBudget);
    }

    int applyBlastTerrain(ServerLevel level, int columnBudget, long deadlineNanos,
            HbmNuclearChunkLoadBudget ticketBudget) {
        if (!isCraterExcavationComplete()) {
            return 0;
        }
        if (this.blastTerrain != null && this.blastTerrain.isReady()) {
            this.blastTerrain.beginApplying();
        }
        int used = this.blastTerrain == null ? 0 : this.blastTerrain.apply(level, columnBudget, deadlineNanos, ticketBudget);
        updateCompletion(level);
        return used;
    }

    boolean tickRadiationBurst(ServerLevel level) {
        if (this.radiationBurstTick >= this.profile.radiationBurstTicks()
                || this.profile.radiationBurstBaseDose() <= 0F
                || this.profile.radiationBurstRange() <= 0F) {
            updateCompletion(level);
            return false;
        }

        float sourceDose = this.profile.radiationBurstBaseDose() / (this.radiationBurstTick * 5F + 1F);
        Vec3 originCenter = Vec3.atCenterOf(this.origin);
        AABB bounds = new AABB(originCenter, originCenter).inflate(this.profile.radiationBurstRange());
        for (LivingEntity entity : level.getEntitiesOfClass(LivingEntity.class, bounds)) {
            if (entity.isRemoved()) {
                continue;
            }
            Vec3 offset = entity.getEyePosition().subtract(originCenter);
            double distance = offset.length();
            Vec3 direction = distance > 0D ? offset.scale(1D / distance) : Vec3.ZERO;
            double resistance = 0D;
            for (int step = 1; step < distance; step++) {
                BlockPos sample = BlockPos.containing(originCenter.add(direction.scale(step)));
                if (!level.hasChunkAt(sample)) {
                    break;
                }
                BlockState state = level.getBlockState(sample);
                resistance += state.getExplosionResistance(level, sample, null);
            }
            resistance = Math.max(1D, resistance);
            double dose = sourceDose / resistance / Math.max(1D, distance * distance);
            RadiationManager.applyDirectExposure(entity, RadiationSourceType.EXPLOSION, dose, true);
        }
        this.radiationBurstTick++;
        updateCompletion(level);
        return true;
    }

    /** EntityNukeExplosionMK5 applies ExplosionNukeGeneric.dealDamage on every active explosion tick. */
    void tickHeatWave(ServerLevel level) {
        if (this.phase == Phase.COMPLETE
                || !isMk5Algorithm(this.profile.terrainAlgorithm())) {
            return;
        }
        HbmExplosionService.applyNuclearDamage(level, Vec3.atCenterOf(this.origin), this.profile);
    }

    int destroy(ServerLevel level, int blockBudget, long deadlineNanos, HbmNuclearChunkLoadBudget ticketBudget) {
        if (this.phase != Phase.DESTROYING || blockBudget <= 0) {
            return 0;
        }

        repairDestructionTicketWindow(level);
        prefetchDestruction(level, ticketBudget);
        int used = 0;
        while (this.destructionChunkIndex < this.destructionChunks.size()
                && used < blockBudget && System.nanoTime() < deadlineNanos) {
            long chunkKey = this.destructionChunks.get(this.destructionChunkIndex);
            if (HbmConfig.BOMBS.forceLoadNuclearWork.get() && !this.activeTickets.contains(chunkKey)) {
                break;
            }
            int chunkX = ChunkPos.getX(chunkKey);
            int chunkZ = ChunkPos.getZ(chunkKey);
            if (!level.hasChunk(chunkX, chunkZ)) {
                if (!HbmConfig.BOMBS.forceLoadNuclearWork.get()) {
                    this.destructionChunkIndex++;
                    continue;
                }
                break;
            }

            HbmNuclearChunkTargets targets = this.plannedTargetsByChunk.get(chunkKey);
            if (targets != null) {
                used += targets.destroy(level, blockBudget - used, deadlineNanos);
                if (!targets.isComplete()) {
                    break;
                }
            }

            this.plannedTargetsByChunk.remove(chunkKey);
            releaseChunk(level, chunkKey);
            this.destructionChunkIndex++;
            prefetchDestruction(level, ticketBudget);
        }

        updateCompletion(level);
        return used;
    }

    boolean isPreloading() {
        return this.phase == Phase.PRELOADING;
    }

    boolean isPlanning() {
        return this.phase == Phase.PRELOADING || this.phase == Phase.PLANNING
                || this.phase == Phase.HYBRID_SNAPSHOT || this.phase == Phase.HYBRID_WAITING
                || this.phase == Phase.SOURCE_PLANNING
                || this.blastTerrain != null && this.blastTerrain.isPlanning();
    }

    boolean isCapturingWorkerSnapshot() {
        return this.phase == Phase.HYBRID_SNAPSHOT;
    }

    boolean awaitsWorkerPlanningSlot() {
        return this.phase == Phase.PRELOADING
                && HbmConfig.BOMBS.forceLoadNuclearWork.get()
                && isMk5Algorithm(this.profile.terrainAlgorithm());
    }

    boolean occupiesWorkerPlanningSlot() {
        return this.phase == Phase.HYBRID_SNAPSHOT
                || this.phase == Phase.HYBRID_WAITING
                || this.phase == Phase.SOURCE_PLANNING;
    }

    void beginWorkerPlanning() {
        if (awaitsWorkerPlanningSlot()) {
            this.phase = Phase.HYBRID_SNAPSHOT;
        }
    }

    boolean isReady() {
        return this.phase == Phase.READY && (this.blastTerrain == null || this.blastTerrain.isReady());
    }

    boolean isDestroying() {
        return this.phase == Phase.DESTROYING;
    }

    void beginDestroying() {
        if (isReady()) {
            this.phase = Phase.DESTROYING;
        }
    }

    boolean isCraterExcavationComplete() {
        return this.phase == Phase.COMPLETE
                || this.phase == Phase.DESTROYING && this.destructionChunkIndex >= this.destructionChunks.size();
    }

    boolean isComplete() {
        return this.phase == Phase.COMPLETE;
    }

    BlockPos origin() {
        return this.origin;
    }

    HbmExplosionProfile profile() {
        return this.profile;
    }

    UUID ticketOwner() {
        return this.ticketOwner;
    }

    void collectTicketOwners(Set<UUID> owners) {
        owners.add(this.ticketOwner);
        if (this.blastTerrain != null) {
            owners.add(this.blastTerrain.ticketOwner());
        }
    }

    int progressStage() {
        if (isPlanning()) {
            return 0;
        }
        if (isCraterExcavationComplete() && this.blastTerrain != null && !this.blastTerrain.isComplete()) {
            return 3;
        }
        return this.phase == Phase.COMPLETE ? 2 : 1;
    }

    int progressPercent() {
        if (this.phase == Phase.COMPLETE) {
            return 100;
        }
        if (this.phase == Phase.PRELOADING) {
            return percentage(this.craterPreloadIndex, this.craterChunks.size()) / 10;
        }
        if (isPlanning()) {
            int rayProgress = switch (this.phase) {
                case PRELOADING -> percentage(this.craterReadyIndex, this.craterChunks.size()) / 2;
                case HYBRID_SNAPSHOT -> this.snapshotBuilder == null
                        ? 0
                        : 10 + this.snapshotBuilder.progressPercent() * 60 / 100;
                case HYBRID_WAITING -> 80;
                case SOURCE_PLANNING -> percentage(this.rayIndex, directionCount(this.profile));
                case PLANNING -> percentage(this.rayIndex, directionCount(this.profile));
                default -> 100;
            };
            return 10 + rayProgress * 90 / 100;
        }
        if (!isCraterExcavationComplete()) {
            return percentage(this.destructionChunkIndex, this.destructionChunks.size());
        }
        if (this.blastTerrain != null && !this.blastTerrain.isComplete()) {
            return this.blastTerrain.progressPercent();
        }
        return 100;
    }

    HbmFalloutRainJob createFalloutRainJob() {
        return this.profile.fallout().isEnabled()
                ? new HbmFalloutRainJob(this.origin, this.profile.fallout(), this.seed)
                : null;
    }

    CompoundTag save() {
        CompoundTag tag = new CompoundTag();
        tag.putLong("Origin", this.origin.asLong());
        tag.putLong("Seed", this.seed);
        tag.putUUID("TicketOwner", this.ticketOwner);
        boolean transientWorkerPhase = this.phase == Phase.HYBRID_SNAPSHOT
                || this.phase == Phase.HYBRID_WAITING
                || this.phase == Phase.SOURCE_PLANNING;
        tag.putInt("CraterPreloadIndex", transientWorkerPhase ? 0 : this.craterPreloadIndex);
        tag.putInt("CraterReadyIndex", transientWorkerPhase ? 0 : this.craterReadyIndex);
        tag.putInt("RayIndex", this.rayIndex);
        tag.putInt("DestructionChunkIndex", this.destructionChunkIndex);
        tag.putInt("DestructionPrefetchIndex", this.destructionPrefetchIndex);
        tag.putInt("RadiationBurstTick", this.radiationBurstTick);
        tag.putDouble("GspHeight", this.gspHeight);
        tag.putDouble("GspAzimuth", this.gspAzimuth);
        Phase persistedPhase = transientWorkerPhase ? Phase.PRELOADING : this.phase;
        tag.putString("Phase", persistedPhase.name());
        tag.putBoolean("RestartSnapshotTickets", transientWorkerPhase);
        tag.putBoolean("ExactFallback", this.exactFallback);
        writeProfile(tag, this.profile);
        tag.put("ActiveTickets", new LongArrayTag(this.activeTickets.stream().mapToLong(Long::longValue).toArray()));
        tag.put("DestructionChunks", new LongArrayTag(this.destructionChunks.stream().mapToLong(Long::longValue).toArray()));

        ListTag targetChunks = new ListTag();
        for (HbmNuclearChunkTargets targets : this.plannedTargetsByChunk.values()) {
            targetChunks.add(targets.save());
        }
        tag.put("TargetChunks", targetChunks);
        if (this.blastTerrain != null) {
            tag.put("BlastTerrain", this.blastTerrain.save());
        }
        return tag;
    }

    static HbmNuclearExplosionJob load(CompoundTag tag) {
        Phase phase = readPhase(tag.getString("Phase"));
        boolean restartSnapshotTickets = tag.getBoolean("RestartSnapshotTickets")
                || phase == Phase.HYBRID_SNAPSHOT
                || phase == Phase.HYBRID_WAITING
                || phase == Phase.SOURCE_PLANNING;
        if (restartSnapshotTickets) {
            phase = Phase.PRELOADING;
        }
        HbmNuclearExplosionJob job = new HbmNuclearExplosionJob(
                BlockPos.of(tag.getLong("Origin")),
                readProfile(tag),
                tag.getLong("Seed"),
                tag.hasUUID("TicketOwner") ? tag.getUUID("TicketOwner") : UUID.randomUUID(),
                tag.getInt("CraterPreloadIndex"),
                tag.contains("CraterReadyIndex") ? tag.getInt("CraterReadyIndex") : 0,
                tag.getInt("RayIndex"),
                tag.getInt("DestructionChunkIndex"),
                tag.getInt("DestructionPrefetchIndex"),
                tag.getInt("RadiationBurstTick"),
                tag.contains("GspHeight")
                        ? tag.getDouble("GspHeight")
                        : tag.contains("GspPolar") ? Math.cos(tag.getDouble("GspPolar")) : -1D,
                tag.contains("GspAzimuth") ? tag.getDouble("GspAzimuth") : 0D,
                phase
        );
        job.exactFallback = tag.getBoolean("ExactFallback");
        job.restartSnapshotTickets = restartSnapshotTickets || phase == Phase.PRELOADING;

        if (!tag.contains("GspHeight") && !tag.contains("GspPolar")
                && isMk5Algorithm(job.profile.terrainAlgorithm())) {
            job.restoreMk5Direction();
        }

        if (tag.contains("BlastTerrain")) {
            job.blastTerrain = HbmBlastTerrainJob.load(
                    job.origin,
                    job.profile.fallout(),
                    job.seed,
                    tag.getCompound("BlastTerrain")
            );
        }
        for (long ticket : tag.getLongArray("ActiveTickets")) {
            job.activeTickets.add(ticket);
        }
        job.restartDestructionTickets = phase == Phase.DESTROYING;
        for (long chunk : tag.getLongArray("DestructionChunks")) {
            job.destructionChunks.add(chunk);
        }
        ListTag targetChunks = tag.getList("TargetChunks", Tag.TAG_COMPOUND);
        for (int index = 0; index < targetChunks.size(); index++) {
            HbmNuclearChunkTargets targets = HbmNuclearChunkTargets.load(targetChunks.getCompound(index));
            job.plannedTargetsByChunk.put(targets.chunkKey(), targets);
        }

        // Migration for saved jobs created by the old global BlockPos scheduler.
        if (targetChunks.isEmpty()) {
            long[] legacyTargets = tag.getLongArray("Targets");
            for (long target : legacyTargets) {
                BlockPos pos = BlockPos.of(target);
                job.addTarget(pos, tag.contains("MinBuildHeight") ? tag.getInt("MinBuildHeight") : -64,
                        tag.contains("BuildHeight") ? tag.getInt("BuildHeight") : 384);
            }
            if ((phase == Phase.READY || phase == Phase.DESTROYING) && job.destructionChunks.isEmpty()) {
                job.destructionChunks.addAll(job.plannedTargetsByChunk.keySet());
                job.destructionChunks.sort(HbmNuclearChunkMath.centerOutComparator(job.origin));
            }
        }
        return job;
    }

    private void updateCompletion(ServerLevel level) {
        if (this.phase != Phase.DESTROYING) {
            return;
        }
        boolean craterComplete = this.destructionChunkIndex >= this.destructionChunks.size();
        boolean terrainComplete = this.blastTerrain == null || this.blastTerrain.isComplete();
        boolean radiationComplete = this.radiationBurstTick >= this.profile.radiationBurstTicks();
        if (craterComplete && terrainComplete && radiationComplete) {
            releaseAllTickets(level);
            this.phase = Phase.COMPLETE;
        }
    }

    private void beginPlanningPhase() {
        boolean useWorker = HbmConfig.BOMBS.forceLoadNuclearWork.get()
                && isMk5Algorithm(this.profile.terrainAlgorithm());
        if (useWorker) {
            this.phase = Phase.HYBRID_SNAPSHOT;
            return;
        }
        if (this.profile.terrainAlgorithm() == HbmExplosionTerrainAlgorithm.MK5_HYBRID_RADIAL) {
            this.exactFallback = true;
        }
        this.phase = Phase.PLANNING;
    }

    private void finishPlanning(ServerLevel level) {
        this.destructionChunks.clear();
        this.destructionChunks.addAll(this.plannedTargetsByChunk.keySet());
        this.destructionChunks.sort(HbmNuclearChunkMath.centerOutComparator(this.origin));
        this.destructionChunkIndex = 0;
        this.destructionPrefetchIndex = 0;
        this.mk5VanillaResistanceCache.clear();
        this.workerSnapshot = null;
        this.snapshotBuilder = null;
        this.releaseAllTickets(level);
        this.phase = Phase.READY;
    }

    private boolean useHybridPlanner() {
        return this.profile.terrainAlgorithm() == HbmExplosionTerrainAlgorithm.MK5_HYBRID_RADIAL
                && !this.exactFallback;
    }

    private void prefetchSnapshot(ServerLevel level, HbmNuclearChunkLoadBudget ticketBudget) {
        if (!HbmConfig.BOMBS.forceLoadNuclearWork.get()) {
            return;
        }
        int window = HbmConfig.BOMBS.nuclearActiveChunkWindow.get();
        while (this.activeTickets.size() < window
                && this.craterPreloadIndex < this.craterChunks.size()
                && ticketBudget.remaining() > 0) {
            long chunkKey = this.craterChunks.get(this.craterPreloadIndex);
            if (!ticketBudget.ensureForced(level, this.ticketOwner, chunkKey, this.activeTickets)) {
                break;
            }
            this.craterPreloadIndex++;
        }
    }

    private void releaseCapturedSnapshotChunks(ServerLevel level) {
        if (this.snapshotBuilder == null) {
            return;
        }
        int completedChunks = this.snapshotBuilder.completedChunkCount();
        while (this.craterReadyIndex < completedChunks) {
            releaseChunk(level, this.craterChunks.get(this.craterReadyIndex));
            this.craterReadyIndex++;
        }
    }

    private void prefetchDestruction(ServerLevel level, HbmNuclearChunkLoadBudget ticketBudget) {
        if (!HbmConfig.BOMBS.forceLoadNuclearWork.get()) {
            return;
        }
        this.destructionPrefetchIndex = HbmNuclearTicketWindow.prefetch(
                this.destructionChunks,
                this.activeTickets,
                this.destructionPrefetchIndex,
                HbmConfig.BOMBS.nuclearActiveChunkWindow.get(),
                chunkKey -> ticketBudget.ensureForced(level, this.ticketOwner, chunkKey, this.activeTickets)
        );
    }

    private void repairDestructionTicketWindow(ServerLevel level) {
        if (!HbmConfig.BOMBS.forceLoadNuclearWork.get()) {
            this.restartDestructionTickets = false;
            return;
        }
        boolean skippedCurrent = HbmNuclearTicketWindow.hasSkippedCurrentChunk(
                this.destructionChunks,
                this.activeTickets,
                this.destructionChunkIndex,
                this.destructionPrefetchIndex
        );
        if (!this.restartDestructionTickets && !skippedCurrent) {
            return;
        }
        releaseAllTickets(level);
        this.destructionPrefetchIndex = this.destructionChunkIndex;
        this.restartDestructionTickets = false;
    }

    private void releaseChunk(ServerLevel level, long chunkKey) {
        if (this.activeTickets.remove(chunkKey)) {
            HbmNuclearChunkTickets.release(level, this.ticketOwner, chunkKey);
        }
    }

    private void releaseAllTickets(ServerLevel level) {
        for (long chunkKey : this.activeTickets) {
            HbmNuclearChunkTickets.release(level, this.ticketOwner, chunkKey);
        }
        this.activeTickets.clear();
    }

    private int traceExplosionNtRay(ServerLevel level, RayDirection direction, float randomUnit) {
        double remainingPower = HbmExplosionMath.initialRayPower(this.profile.terrainStrength(), randomUnit);
        double currentX = this.origin.getX() + 0.5D;
        double currentY = this.origin.getY() + 0.5D;
        double currentZ = this.origin.getZ() + 0.5D;
        double traveledDistance = 0D;
        int steps = 0;
        BlockPos.MutableBlockPos pos = this.tracePosition;

        while (remainingPower > 0D && isWithinTerrainRange(traveledDistance)) {
            pos.set((int) Math.floor(currentX), (int) Math.floor(currentY), (int) Math.floor(currentZ));
            if (pos.getY() < level.getMinBuildHeight() || pos.getY() >= level.getMaxBuildHeight()) {
                break;
            }
            if (!level.hasChunk(pos.getX() >> 4, pos.getZ() >> 4)) {
                break;
            }

            BlockState state = level.getBlockState(pos);
            if (!state.isAir()) {
                remainingPower = HbmExplosionMath.remainingPowerAfterBlock(
                        remainingPower,
                        state.getExplosionResistance(level, pos, null)
                );
                if (remainingPower > 0D) {
                    addTarget(pos, level.getMinBuildHeight(), level.getHeight());
                }
            }

            currentX += direction.x * HbmExplosionMath.RAY_STEP;
            currentY += direction.y * HbmExplosionMath.RAY_STEP;
            currentZ += direction.z * HbmExplosionMath.RAY_STEP;
            traveledDistance += HbmExplosionMath.RAY_STEP;
            remainingPower = HbmExplosionMath.remainingPowerAfterTravel(remainingPower);
            steps++;
        }
        return steps;
    }

    /** Main-thread port of ExplosionNukeRayBatched's generalized-spiral resistance trace. */
    private int traceMk5Ray(ServerLevel level, double directionX, double directionY, double directionZ,
            float stoneResistance) {
        int strengthLength = Math.max(1, (int) Math.ceil(this.profile.terrainStrength()));
        int maxSteps = this.profile.maxTerrainDistance() > 0F
                ? Math.max(1, (int) Math.ceil(this.profile.maxTerrainDistance()))
                : strengthLength;
        double remainingPower = this.profile.terrainStrength();
        int steps = 0;
        BlockPos.MutableBlockPos pos = this.tracePosition;
        double currentX = this.origin.getX() + 0.5D;
        double currentY = this.origin.getY() + 0.5D;
        double currentZ = this.origin.getZ() + 0.5D;
        int minBuildHeight = level.getMinBuildHeight();
        int maxBuildHeight = level.getMaxBuildHeight();
        int buildHeight = level.getHeight();
        int currentChunkX = Integer.MIN_VALUE;
        int currentChunkZ = Integer.MIN_VALUE;
        LevelChunk currentChunk = null;
        HbmNuclearChunkTargets currentTargets = null;

        for (int step = 0; step < maxSteps && remainingPower > 0D; step++) {
            int blockX = (int) Math.floor(currentX);
            int blockY = (int) Math.floor(currentY);
            int blockZ = (int) Math.floor(currentZ);
            if (blockY < minBuildHeight || blockY >= maxBuildHeight) {
                break;
            }
            pos.set(blockX, blockY, blockZ);

            int chunkX = blockX >> 4;
            int chunkZ = blockZ >> 4;
            if (chunkX != currentChunkX || chunkZ != currentChunkZ) {
                if (!level.hasChunk(chunkX, chunkZ)) {
                    break;
                }
                currentChunkX = chunkX;
                currentChunkZ = chunkZ;
                currentChunk = level.getChunk(chunkX, chunkZ);
                long chunkKey = ChunkPos.asLong(chunkX, chunkZ);
                currentTargets = this.plannedTargetsByChunk.computeIfAbsent(
                        chunkKey,
                        key -> new HbmNuclearChunkTargets(key, minBuildHeight, buildHeight)
                );
            }

            BlockState state = currentChunk.getBlockState(pos);
            if (!state.isAir()) {
                if (state.getFluidState().isEmpty()) {
                    remainingPower -= HbmExplosionMath.mk5AdjustedResistanceLoss(
                            mk5MasqueradeResistance(level, pos, state, stoneResistance),
                            step,
                            strengthLength,
                            directionY,
                            this.profile.craterDepthMultiplier()
                    );
                }
                if (remainingPower > 0D) {
                    currentTargets.add(blockX, blockY, blockZ);
                }
            }
            currentX += directionX;
            currentY += directionY;
            currentZ += directionZ;
            steps++;
        }
        return steps;
    }

    private void addTarget(BlockPos pos, int minBuildHeight, int buildHeight) {
        long chunkKey = ChunkPos.asLong(pos.getX() >> 4, pos.getZ() >> 4);
        this.plannedTargetsByChunk
                .computeIfAbsent(chunkKey, key -> new HbmNuclearChunkTargets(key, minBuildHeight, buildHeight))
                .add(pos);
    }

    private float mk5MasqueradeResistance(ServerLevel level, BlockPos pos, BlockState state, float stoneResistance) {
        Float cached = this.mk5VanillaResistanceCache.get(state);
        if (cached != null) {
            return cached;
        }
        float resistance = state.getExplosionResistance(level, pos, null);
        float adjusted = HbmExplosionMath.mk5MasqueradeResistance(
                resistance,
                state.is(Blocks.SANDSTONE),
                state.is(Blocks.OBSIDIAN),
                stoneResistance
        );
        if ("minecraft".equals(BuiltInRegistries.BLOCK.getKey(state.getBlock()).getNamespace())) {
            this.mk5VanillaResistanceCache.put(state, adjusted);
        }
        return adjusted;
    }

    private float rayRandom(int index) {
        return HbmExplosionMath.deterministicRayRandom(this.seed, index);
    }

    private boolean isWithinTerrainRange(double traveledDistance) {
        return this.profile.maxTerrainDistance() <= 0F || traveledDistance <= this.profile.maxTerrainDistance();
    }

    private static int planningRadius(HbmExplosionProfile profile) {
        return Math.max(1, (int) Math.ceil(profile.maxTerrainDistance() > 0F
                ? profile.maxTerrainDistance()
                : profile.terrainStrength()));
    }

    private static int directionCount(HbmExplosionProfile profile) {
        return isMk5Algorithm(profile.terrainAlgorithm())
                ? profile.rayCount()
                : HbmExplosionMath.boundaryRayCount(profile.rayResolution());
    }

    private static List<RayDirection> directions(HbmExplosionProfile profile) {
        int samplingCount = profile.rayResolution();
        RayKey key = new RayKey(profile.terrainAlgorithm(), samplingCount);
        return DIRECTION_CACHE.computeIfAbsent(key, HbmNuclearExplosionJob::buildDirections);
    }

    private static List<RayDirection> buildDirections(RayKey key) {
        return buildExplosionNtDirections(key.samplingCount);
    }

    private static List<RayDirection> buildExplosionNtDirections(int resolution) {
        List<RayDirection> directions = new ArrayList<>(HbmExplosionMath.boundaryRayCount(resolution));
        for (int x = 0; x < resolution; x++) {
            for (int y = 0; y < resolution; y++) {
                for (int z = 0; z < resolution; z++) {
                    if (x != 0 && x != resolution - 1 && y != 0 && y != resolution - 1 && z != 0 && z != resolution - 1) {
                        continue;
                    }
                    double dx = (double) x / (resolution - 1D) * 2D - 1D;
                    double dy = (double) y / (resolution - 1D) * 2D - 1D;
                    double dz = (double) z / (resolution - 1D) * 2D - 1D;
                    double length = Math.sqrt(dx * dx + dy * dy + dz * dz);
                    directions.add(new RayDirection(dx / length, dy / length, dz / length));
                }
            }
        }
        return directions;
    }

    private void advanceMk5Direction(int directionCount) {
        if (!isMk5Algorithm(this.profile.terrainAlgorithm())) {
            return;
        }
        int sourcePoint = this.rayIndex + 1;
        if (sourcePoint >= directionCount) {
            this.gspHeight = 1D;
            this.gspAzimuth = 0D;
            return;
        }
        this.gspHeight = HbmExplosionMath.mk5SpiralHeight(sourcePoint, directionCount);
        this.gspAzimuth = (this.gspAzimuth
                + HbmExplosionMath.mk5SpiralAzimuthIncrement(sourcePoint, directionCount)) % (Math.PI * 2D);
    }

    private void restoreMk5Direction() {
        this.gspHeight = -1D;
        this.gspAzimuth = 0D;
        int directionCount = directionCount(this.profile);
        for (int index = 0; index < this.rayIndex; index++) {
            int sourcePoint = index + 1;
            if (sourcePoint >= directionCount) {
                this.gspHeight = 1D;
                this.gspAzimuth = 0D;
                return;
            }
            this.gspHeight = HbmExplosionMath.mk5SpiralHeight(sourcePoint, directionCount);
            this.gspAzimuth = (this.gspAzimuth
                    + HbmExplosionMath.mk5SpiralAzimuthIncrement(sourcePoint, directionCount)) % (Math.PI * 2D);
        }
    }

    private static void writeProfile(CompoundTag tag, HbmExplosionProfile profile) {
        tag.putString("Mode", profile.mode().name());
        tag.putString("TerrainExecution", profile.terrainExecution().name());
        tag.putString("TerrainAlgorithm", profile.terrainAlgorithm().name());
        tag.putFloat("TerrainStrength", profile.terrainStrength());
        tag.putFloat("MaxTerrainDistance", profile.maxTerrainDistance());
        tag.putFloat("CraterDepthMultiplier", profile.craterDepthMultiplier());
        tag.putInt("RayResolution", profile.rayResolution());
        tag.putInt("RayCount", profile.rayCount());
        tag.putFloat("KillRadius", profile.killRadius());
        tag.putFloat("MaxDamage", profile.maxDamage());
        tag.putInt("RadiationLevel", profile.radiationLevel());
        tag.putInt("RadiationBurstTicks", profile.radiationBurstTicks());
        tag.putFloat("RadiationBurstBaseDose", profile.radiationBurstBaseDose());
        tag.putFloat("RadiationBurstRange", profile.radiationBurstRange());
        tag.putBoolean("NoBlockDrops", profile.noBlockDrops());
        profile.fallout().save(tag);
        tag.putString("Sound", profile.sound().toString());
        tag.putString("ClientEffect", profile.clientEffect().toString());
    }

    private static HbmExplosionProfile readProfile(CompoundTag tag) {
        String storedExecution = tag.getString("TerrainExecution");
        HbmExplosionExecution terrainExecution = storedExecution.isBlank()
                ? HbmExplosionExecution.BATCHED
                : HbmExplosionExecution.valueOf(storedExecution);
        String storedAlgorithm = tag.getString("TerrainAlgorithm");
        HbmExplosionTerrainAlgorithm terrainAlgorithm = storedAlgorithm.isBlank()
                ? HbmExplosionTerrainAlgorithm.EXPLOSION_NT
                : HbmExplosionTerrainAlgorithm.valueOf(storedAlgorithm);
        int rayResolution = tag.getInt("RayResolution");
        int rayCount = tag.contains("RayCount")
                ? tag.getInt("RayCount")
                : isMk5Algorithm(terrainAlgorithm)
                        ? HbmExplosionMath.mk5SourceRayCount(tag.getFloat("TerrainStrength"))
                        : 0;
        return new HbmExplosionProfile(
                HbmExplosionMode.valueOf(tag.getString("Mode")),
                terrainExecution,
                terrainAlgorithm,
                tag.getFloat("TerrainStrength"),
                tag.getFloat("MaxTerrainDistance"),
                tag.contains("CraterDepthMultiplier") ? tag.getFloat("CraterDepthMultiplier") : 1F,
                rayResolution,
                rayCount,
                tag.getFloat("KillRadius"),
                tag.getFloat("MaxDamage"),
                tag.getInt("RadiationLevel"),
                tag.getInt("RadiationBurstTicks"),
                tag.getFloat("RadiationBurstBaseDose"),
                tag.getFloat("RadiationBurstRange"),
                tag.getBoolean("NoBlockDrops"),
                HbmFalloutProfile.load(tag),
                ResourceLocation.parse(tag.getString("Sound")),
                ResourceLocation.parse(tag.getString("ClientEffect"))
        );
    }

    private static Phase readPhase(String value) {
        if (value.isBlank()) {
            return Phase.PRELOADING;
        }
        try {
            return Phase.valueOf(value);
        } catch (IllegalArgumentException ignored) {
            return Phase.PRELOADING;
        }
    }

    private static boolean isMk5Algorithm(HbmExplosionTerrainAlgorithm algorithm) {
        return algorithm == HbmExplosionTerrainAlgorithm.MK5_GENERALIZED_SPIRAL
                || algorithm == HbmExplosionTerrainAlgorithm.MK5_HYBRID_RADIAL;
    }

    private static int percentage(int numerator, int denominator) {
        if (denominator <= 0) {
            return numerator > 0 ? 100 : 0;
        }
        return Math.clamp((int) Math.floor((double) numerator * 100D / denominator), 0, 100);
    }

    private enum Phase {
        PRELOADING,
        HYBRID_SNAPSHOT,
        HYBRID_WAITING,
        SOURCE_PLANNING,
        PLANNING,
        READY,
        DESTROYING,
        COMPLETE
    }

    private record RayDirection(double x, double y, double z) {
    }

    private record SourceBatchTask(int startRay, int endRay,
            CompletableFuture<HbmGspExplosionPlanner.BatchResult> future) {
    }

    private record RayKey(HbmExplosionTerrainAlgorithm algorithm, int samplingCount) {
    }
}
