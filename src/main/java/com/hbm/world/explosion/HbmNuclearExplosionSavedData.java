package com.hbm.world.explosion;

import com.hbm.config.HbmConfig;
import com.hbm.network.NuclearProgressPayload;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.datafix.DataFixTypes;
import net.minecraft.world.level.saveddata.SavedData;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * Per-dimension persistence for queued HBM nuclear terrain work.
 */
final class HbmNuclearExplosionSavedData extends SavedData {
    private static final String DATA_NAME = "hbm_nuclear_explosions";
    private final List<HbmNuclearExplosionJob> jobs = new ArrayList<>();
    private final List<HbmWaterVaporizationJob> waterVaporizationJobs = new ArrayList<>();
    private final List<HbmFalloutRainJob> falloutJobs = new ArrayList<>();
    private int preloadRoundRobinCursor;
    private int snapshotRoundRobinCursor;
    private int planningRoundRobinCursor;
    private int terrainRoundRobinCursor;
    private int destructionRoundRobinCursor;
    private int waterRoundRobinCursor;
    private int falloutRoundRobinCursor;
    private int ticketLaneRoundRobinCursor;
    private int timeLaneRoundRobinCursor;

    private static final Factory<HbmNuclearExplosionSavedData> FACTORY = new Factory<>(
            HbmNuclearExplosionSavedData::new,
            HbmNuclearExplosionSavedData::load,
            DataFixTypes.LEVEL
    );

    static HbmNuclearExplosionSavedData get(ServerLevel level) {
        return level.getDataStorage().computeIfAbsent(FACTORY, DATA_NAME);
    }

    static HbmNuclearExplosionSavedData load(CompoundTag tag, HolderLookup.Provider provider) {
        HbmNuclearExplosionSavedData data = new HbmNuclearExplosionSavedData();
        ListTag jobs = tag.getList("Jobs", Tag.TAG_COMPOUND);
        for (int index = 0; index < jobs.size(); index++) {
            data.jobs.add(HbmNuclearExplosionJob.load(jobs.getCompound(index)));
        }
        ListTag waterJobs = tag.getList("WaterVaporizationJobs", Tag.TAG_COMPOUND);
        for (int index = 0; index < waterJobs.size(); index++) {
            data.waterVaporizationJobs.add(HbmWaterVaporizationJob.load(waterJobs.getCompound(index)));
        }
        ListTag falloutJobs = tag.getList("FalloutJobs", Tag.TAG_COMPOUND);
        for (int index = 0; index < falloutJobs.size(); index++) {
            data.falloutJobs.add(HbmFalloutRainJob.load(falloutJobs.getCompound(index)));
        }
        return data;
    }

    void enqueue(HbmNuclearExplosionJob job) {
        this.jobs.add(job);
        setDirty();
    }

    void enqueueFallout(HbmFalloutRainJob job) {
        if (job != null) {
            this.falloutJobs.add(job);
            setDirty();
        }
    }

    void enqueueWaterVaporization(HbmWaterVaporizationJob job) {
        if (job != null) {
            this.waterVaporizationJobs.add(job);
            setDirty();
        }
    }

    void tick(ServerLevel level) {
        if (this.jobs.isEmpty() && this.waterVaporizationJobs.isEmpty() && this.falloutJobs.isEmpty()) {
            return;
        }

        boolean changed = false;
        long schedulerDeadline = deadlineAfterMillis(HbmConfig.BOMBS.nuclearSchedulerTimeBudgetMs.get());
        int totalChunkTickets = HbmConfig.BOMBS.nuclearForceLoadChunksPerTick.get();
        int availableWorkerSlots = Math.max(0,
                HbmNuclearPlanningService.boundedJobCapacity()
                        - (int) this.jobs.stream().filter(HbmNuclearExplosionJob::occupiesWorkerPlanningSlot).count());
        int workerSlotsAtTickStart = availableWorkerSlots;
        boolean hasReadyWaterJob = this.waterVaporizationJobs.stream().anyMatch(this::isWaterJobReady);
        boolean hasPlanningTicketWork = HbmConfig.BOMBS.forceLoadNuclearWork.get()
                && this.jobs.stream().anyMatch(job -> job.isCapturingWorkerSnapshot()
                        || job.isPreloading()
                        && (!job.awaitsWorkerPlanningSlot() || workerSlotsAtTickStart > 0));
        boolean hasMutationTicketWork = this.jobs.stream().anyMatch(HbmNuclearExplosionJob::isDestroying);
        boolean hasFalloutTicketWork = !this.falloutJobs.isEmpty();
        boolean hasPlanningTimeWork = this.jobs.stream().anyMatch(HbmNuclearExplosionJob::isPlanning);
        HbmNuclearLaneAllocator.Allocation ticketAllocation = HbmNuclearLaneAllocator.allocateTickets(
                totalChunkTickets,
                HbmConfig.BOMBS.nuclearWaterForceLoadChunksPerTick.get(),
                this.ticketLaneRoundRobinCursor++,
                hasPlanningTicketWork,
                hasMutationTicketWork,
                hasReadyWaterJob,
                hasFalloutTicketWork
        );
        HbmNuclearLaneAllocator.Allocation timeAllocation = HbmNuclearLaneAllocator.allocateFair(
                HbmConfig.BOMBS.nuclearSchedulerTimeBudgetMs.get(),
                this.timeLaneRoundRobinCursor++,
                hasPlanningTimeWork,
                hasMutationTicketWork,
                hasReadyWaterJob,
                hasFalloutTicketWork
        );
        HbmNuclearChunkLoadBudget planningChunkLoadBudget = new HbmNuclearChunkLoadBudget(
                ticketAllocation.planning(), schedulerDeadline
        );
        HbmNuclearChunkLoadBudget mutationChunkLoadBudget = new HbmNuclearChunkLoadBudget(
                ticketAllocation.mutation(), schedulerDeadline
        );
        HbmNuclearChunkLoadBudget waterChunkLoadBudget = new HbmNuclearChunkLoadBudget(
                ticketAllocation.water(), schedulerDeadline
        );
        HbmNuclearChunkLoadBudget falloutChunkLoadBudget = new HbmNuclearChunkLoadBudget(
                ticketAllocation.fallout(), schedulerDeadline
        );

        int jobCount = this.jobs.size();
        int preloadStart = rotatedStart(this.preloadRoundRobinCursor++, jobCount);
        for (int offset = 0; offset < jobCount; offset++) {
            if (planningChunkLoadBudget.remaining() <= 0 || !planningChunkLoadBudget.hasTimeRemaining()) {
                break;
            }
            HbmNuclearExplosionJob job = this.jobs.get((preloadStart + offset) % jobCount);
            changed |= job.preload(level, planningChunkLoadBudget) > 0;
        }

        int admissionStart = rotatedStart(this.snapshotRoundRobinCursor, jobCount);
        for (int offset = 0; offset < jobCount && availableWorkerSlots > 0; offset++) {
            HbmNuclearExplosionJob job = this.jobs.get((admissionStart + offset) % jobCount);
            if (job.awaitsWorkerPlanningSlot()) {
                job.beginWorkerPlanning();
                availableWorkerSlots--;
                changed = true;
            }
        }

        int snapshotBudget = HbmConfig.BOMBS.nuclearSnapshotSectionsPerTick.get();
        long planningLaneDeadline = laneDeadline(schedulerDeadline, timeAllocation.planning());
        long snapshotDeadline = Math.min(planningLaneDeadline,
                deadlineAfterMillis(HbmConfig.BOMBS.nuclearSnapshotTimeBudgetMs.get()));
        int capturingJobs = (int) this.jobs.stream().filter(HbmNuclearExplosionJob::isCapturingWorkerSnapshot).count();
        int startIndex = rotatedStart(this.snapshotRoundRobinCursor++, jobCount);
        for (int offset = 0; offset < jobCount; offset++) {
            if (snapshotBudget <= 0 || System.nanoTime() >= snapshotDeadline) {
                break;
            }
            HbmNuclearExplosionJob job = this.jobs.get((startIndex + offset) % jobCount);
            if (!job.isCapturingWorkerSnapshot()) {
                continue;
            }
            int fairShare = Math.max(1, snapshotBudget / Math.max(1, capturingJobs));
            int used = job.captureWorkerSnapshot(level, fairShare, snapshotDeadline, planningChunkLoadBudget);
            snapshotBudget -= used;
            capturingJobs--;
            changed |= used > 0;
        }

        int planningStart = rotatedStart(this.planningRoundRobinCursor++, jobCount);
        for (int offset = 0; offset < jobCount; offset++) {
            if (System.nanoTime() >= planningLaneDeadline) {
                break;
            }
            HbmNuclearExplosionJob job = this.jobs.get((planningStart + offset) % jobCount);
            changed |= job.pollWorkerPlanning(level);
        }

        for (HbmNuclearExplosionJob job : this.jobs) {
            job.tickHeatWave(level);
            changed |= job.tickRadiationBurst(level);
        }

        // A completed plan remains queued for one full tick before any terrain is removed.
        for (HbmNuclearExplosionJob job : this.jobs) {
            if (job.isReady()) {
                job.beginDestroying();
                changed = true;
            }
        }

        int terrainPlanBudget = HbmConfig.FALLOUT.nuclearBlastTerrainPlanColumnsPerTick.get();
        for (HbmNuclearExplosionJob job : this.jobs) {
            if (terrainPlanBudget <= 0) {
                break;
            }
            int used = job.planBlastTerrain(terrainPlanBudget);
            terrainPlanBudget -= used;
            changed |= used > 0;
        }

        int rayBudget = HbmConfig.BOMBS.nuclearRayWorkPerTick.get();
        long planningDeadline = Math.min(planningLaneDeadline,
                deadlineAfterMillis(HbmConfig.BOMBS.nuclearPlanningTimeBudgetMs.get()));
        for (int offset = 0; offset < jobCount; offset++) {
            HbmNuclearExplosionJob job = this.jobs.get((planningStart + offset) % jobCount);
            if (rayBudget <= 0 || System.nanoTime() >= planningDeadline) {
                break;
            }
            if (job.isPlanning()) {
                int used = job.plan(level, rayBudget, planningDeadline);
                rayBudget -= used;
                changed |= used > 0;
            }
        }


        int terrainBudget = HbmConfig.FALLOUT.nuclearBlastTerrainColumnsPerTick.get();
        long mutationLaneDeadline = laneDeadline(schedulerDeadline, timeAllocation.mutation());
        long terrainDeadline = Math.min(mutationLaneDeadline,
                deadlineAfterMillis(HbmConfig.FALLOUT.nuclearBlastTerrainTimeBudgetMs.get()));
        int remainingTerrainJobs = (int) this.jobs.stream().filter(HbmNuclearExplosionJob::isDestroying).count();
        int terrainStart = rotatedStart(this.terrainRoundRobinCursor++, jobCount);
        for (int offset = 0; offset < jobCount; offset++) {
            HbmNuclearExplosionJob job = this.jobs.get((terrainStart + offset) % jobCount);
            if (terrainBudget <= 0 || System.nanoTime() >= terrainDeadline) {
                break;
            }
            if (job.isDestroying()) {
                int fairShare = Math.max(1, terrainBudget / Math.max(1, remainingTerrainJobs));
                int used = job.applyBlastTerrain(level, fairShare, terrainDeadline, mutationChunkLoadBudget);
                terrainBudget -= used;
                remainingTerrainJobs--;
                changed |= used > 0;
            }
        }

        int blockBudget = HbmConfig.BOMBS.nuclearBlockWorkPerTick.get();
        long destructionDeadline = Math.min(mutationLaneDeadline,
                deadlineAfterMillis(HbmConfig.BOMBS.nuclearDestructionTimeBudgetMs.get()));
        int remainingDestructionJobs = (int) this.jobs.stream().filter(HbmNuclearExplosionJob::isDestroying).count();
        int destructionStart = rotatedStart(this.destructionRoundRobinCursor++, jobCount);
        for (int offset = 0; offset < jobCount; offset++) {
            HbmNuclearExplosionJob job = this.jobs.get((destructionStart + offset) % jobCount);
            if (blockBudget <= 0 || System.nanoTime() >= destructionDeadline) {
                break;
            }
            if (job.isDestroying()) {
                int fairShare = Math.max(64, blockBudget / Math.max(1, remainingDestructionJobs));
                int used = job.destroy(level, Math.min(blockBudget, fairShare), destructionDeadline,
                        mutationChunkLoadBudget);
                blockBudget -= used;
                remainingDestructionJobs--;
                changed |= used > 0;
            }
        }

        for (Iterator<HbmNuclearExplosionJob> iterator = this.jobs.iterator(); iterator.hasNext();) {
            HbmNuclearExplosionJob job = iterator.next();
            if (job.isComplete()) {
                enqueueFallout(job.createFalloutRainJob());
                iterator.remove();
                changed = true;
            }
        }

        // Water starts only after its matching crater is excavated. Its ticket lane remains globally bounded
        // and cannot starve planning, terrain mutation, or fallout work from other queued explosions.
        if (hasReadyWaterJob && timeAllocation.water() > 0) {
            int waterBudget = HbmConfig.BOMBS.nuclearWaterVaporBlockWorkPerTick.get();
            int waterLaneMillis = Math.min(
                    HbmConfig.BOMBS.nuclearWaterVaporTimeBudgetMs.get(),
                    timeAllocation.water()
            );
            long waterDeadline = Math.min(schedulerDeadline, deadlineAfterMillis(waterLaneMillis));
            int waterCount = this.waterVaporizationJobs.size();
            int waterStart = rotatedStart(this.waterRoundRobinCursor++, waterCount);
            for (int offset = 0; offset < waterCount; offset++) {
                if (waterBudget <= 0 || System.nanoTime() >= waterDeadline) {
                    break;
                }
                HbmWaterVaporizationJob job = this.waterVaporizationJobs.get((waterStart + offset) % waterCount);
                if (!isWaterJobReady(job)) {
                    continue;
                }
                int used = job.process(level, waterBudget, waterDeadline, waterChunkLoadBudget);
                waterBudget -= used;
                changed |= used > 0;
            }
            for (Iterator<HbmWaterVaporizationJob> iterator = this.waterVaporizationJobs.iterator(); iterator.hasNext();) {
                if (iterator.next().isComplete()) {
                    iterator.remove();
                    changed = true;
                }
            }
        }

        int falloutBudget = HbmConfig.FALLOUT.nuclearFalloutColumnsPerTick.get();
        long falloutDeadline = laneDeadline(schedulerDeadline, timeAllocation.fallout());
        int falloutCount = this.falloutJobs.size();
        int falloutStart = rotatedStart(this.falloutRoundRobinCursor++, falloutCount);
        for (int offset = 0; offset < falloutCount; offset++) {
            if (falloutBudget <= 0 || System.nanoTime() >= falloutDeadline) {
                break;
            }
            HbmFalloutRainJob falloutJob = this.falloutJobs.get((falloutStart + offset) % falloutCount);
            int used = falloutJob.process(level, falloutBudget, falloutDeadline, falloutChunkLoadBudget);
            falloutBudget -= used;
            changed |= used > 0;
        }
        for (Iterator<HbmFalloutRainJob> iterator = this.falloutJobs.iterator(); iterator.hasNext();) {
            if (iterator.next().isComplete()) {
                iterator.remove();
                changed = true;
            }
        }

        if (level.getGameTime() % 10L == 0L) {
            syncProgress(level);
        }

        if (changed) {
            setDirty();
        }
    }

    Set<UUID> activeTicketOwners() {
        Set<UUID> owners = new HashSet<>();
        for (HbmNuclearExplosionJob job : this.jobs) {
            job.collectTicketOwners(owners);
        }
        for (HbmWaterVaporizationJob job : this.waterVaporizationJobs) {
            owners.add(job.ticketOwner());
        }
        for (HbmFalloutRainJob job : this.falloutJobs) {
            owners.add(job.ticketOwner());
        }
        return owners;
    }

    private static long deadlineAfterMillis(int milliseconds) {
        return System.nanoTime() + milliseconds * 1_000_000L;
    }

    private static long laneDeadline(long schedulerDeadline, int milliseconds) {
        return Math.min(schedulerDeadline, deadlineAfterMillis(Math.max(0, milliseconds)));
    }

    private static int rotatedStart(int cursor, int size) {
        return size == 0 ? 0 : Math.floorMod(cursor, size);
    }

    private boolean isWaterJobReady(HbmWaterVaporizationJob waterJob) {
        return this.jobs.stream().noneMatch(job -> job.origin().equals(waterJob.origin())
                && !job.isCraterExcavationComplete());
    }

    private void syncProgress(ServerLevel level) {
        for (HbmNuclearExplosionJob job : this.jobs) {
            NuclearProgressPayload.Stage stage = switch (job.progressStage()) {
                case 0 -> NuclearProgressPayload.Stage.CALCULATING;
                case 3 -> NuclearProgressPayload.Stage.CONVERTING;
                default -> NuclearProgressPayload.Stage.EXCAVATING;
            };
            PacketDistributor.sendToPlayersNear(
                    level,
                    null,
                    job.origin().getX() + 0.5D,
                    job.origin().getY() + 0.5D,
                    job.origin().getZ() + 0.5D,
                    Math.max(64D, job.profile().terrainStrength() * 8D),
                    new NuclearProgressPayload(
                            job.origin().asLong(),
                            stage,
                            job.progressPercent(),
                            Math.max(0, Math.round(job.profile().maxTerrainDistance()))
                    )
            );
        }
        for (HbmFalloutRainJob job : this.falloutJobs) {
            PacketDistributor.sendToPlayersNear(
                    level,
                    null,
                    job.origin().getX() + 0.5D,
                    job.origin().getY() + 0.5D,
                    job.origin().getZ() + 0.5D,
                    Math.max(64D, job.profile().radius() * 2D),
                    new NuclearProgressPayload(
                            job.origin().asLong(),
                            NuclearProgressPayload.Stage.FALLOUT,
                            job.progressPercent(),
                            job.profile().radius()
                    )
            );
        }
        for (HbmWaterVaporizationJob job : this.waterVaporizationJobs) {
            if (!isWaterJobReady(job)) {
                continue;
            }
            PacketDistributor.sendToPlayersNear(
                    level,
                    null,
                    job.origin().getX() + 0.5D,
                    job.origin().getY() + 0.5D,
                    job.origin().getZ() + 0.5D,
                    Math.max(64D, job.radius() * 2D),
                    new NuclearProgressPayload(
                            job.origin().asLong(),
                            NuclearProgressPayload.Stage.VAPORIZING,
                            job.progressPercent(),
                            job.radius()
                    )
            );
        }
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider provider) {
        ListTag jobs = new ListTag();
        for (HbmNuclearExplosionJob job : this.jobs) {
            jobs.add(job.save());
        }
        tag.put("Jobs", jobs);
        ListTag waterJobs = new ListTag();
        for (HbmWaterVaporizationJob waterJob : this.waterVaporizationJobs) {
            waterJobs.add(waterJob.save());
        }
        tag.put("WaterVaporizationJobs", waterJobs);
        ListTag falloutJobs = new ListTag();
        for (HbmFalloutRainJob falloutJob : this.falloutJobs) {
            falloutJobs.add(falloutJob.save());
        }
        tag.put("FalloutJobs", falloutJobs);
        return tag;
    }
}
