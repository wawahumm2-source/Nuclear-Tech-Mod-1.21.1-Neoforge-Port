package com.hbm.world.explosion;

import com.hbm.HbmNuclearTech;
import com.hbm.config.HbmConfig;
import com.hbm.registry.HbmBlocks;
import com.hbm.registry.HbmFluids;
import it.unimi.dsi.fastutil.longs.LongArrayList;
import it.unimi.dsi.fastutil.longs.LongIterator;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.LongArrayTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.LevelChunkSection;
import net.minecraft.world.level.levelgen.Heightmap;

/**
 * Persisted, globally bounded water-body resolution for a nuclear blast.
 *
 * <p>No water is changed during survey or classification. All water-tagged
 * fluid states participate in connectivity, including flowing water and
 * waterlogged hosts. A connected body is evaporated only when its complete
 * in-radius component is proven enclosed and below the configured volume cap.
 * Boundary-connected or oversized bodies are converted to contaminated water
 * and refill the excavated crater to their captured source surface.</p>
 */
final class HbmWaterVaporizationJob {
    private static final int SAVE_VERSION = 3;
    private static final int UNINITIALIZED_SECTION = Integer.MAX_VALUE;
    private static final int UNINITIALIZED_REFILL_Y = Integer.MIN_VALUE;
    private static final int NO_REFILL_LEVEL = Integer.MIN_VALUE;
    private static final int SURVEY_RING_BLOCKS = 2;
    private static final int[][] NEIGHBORS = {
            {1, 0, 0}, {-1, 0, 0},
            {0, 1, 0}, {0, -1, 0},
            {0, 0, 1}, {0, 0, -1}
    };
    private static final int[][] HORIZONTAL_NEIGHBORS = {
            {1, 0, 0}, {-1, 0, 0},
            {0, 0, 1}, {0, 0, -1}
    };

    private enum Phase {
        SURVEY,
        CLASSIFY,
        MUTATE,
        REFILL,
        COMPLETE
    }

    private final BlockPos origin;
    private final int fullRadius;
    private final int craterRadius;
    private final int transitionBlocks;
    private final int surveyRadius;
    private final UUID ticketOwner;
    private final List<Long> surveyChunks;
    private final List<Long> refillChunks;
    private final Set<Long> activeTickets = new LinkedHashSet<>();

    private Phase phase;
    private int chunkIndex;
    private int prefetchIndex;
    private int sectionIndex;
    private int cellIndex;
    private long inspected;

    private LongOpenHashSet surveyedWater = new LongOpenHashSet();
    private LongOpenHashSet remainingWater;
    private final LongArrayList componentStack = new LongArrayList();
    private final LongArrayList componentInside = new LongArrayList();
    private boolean componentPersistent;
    private int classificationTotal;

    private final Map<Long, LongArrayList> evaporationBuilders = new HashMap<>();
    private final Map<Long, LongArrayList> contaminationBuilders = new HashMap<>();
    private List<MutationChunk> mutationChunks = new ArrayList<>();
    private int mutationChunkIndex;
    private int mutationPrefetchIndex;
    private int refillWaterLevel = NO_REFILL_LEVEL;
    private int refillChunkIndex;
    private int refillPrefetchIndex;
    private int refillColumnIndex;
    private int refillY = UNINITIALIZED_REFILL_Y;

    private int evaporated;
    private int contaminated;
    private int refilled;
    private boolean restartTickets;

    HbmWaterVaporizationJob(BlockPos origin, int fullRadius, int transitionBlocks) {
        this(origin, fullRadius, fullRadius, transitionBlocks);
    }

    HbmWaterVaporizationJob(BlockPos origin, int fullRadius, int craterRadius, int transitionBlocks) {
        this(origin, fullRadius, craterRadius, transitionBlocks, UUID.randomUUID(), Phase.SURVEY);
    }

    private HbmWaterVaporizationJob(BlockPos origin, int fullRadius, int craterRadius, int transitionBlocks,
            UUID ticketOwner, Phase phase) {
        this.origin = origin.immutable();
        this.fullRadius = Math.max(0, fullRadius);
        this.craterRadius = Math.max(0, Math.min(this.fullRadius, craterRadius));
        this.transitionBlocks = Math.max(0, transitionBlocks);
        this.surveyRadius = HbmWaterVaporizationMath.surveyRadius(this.fullRadius, SURVEY_RING_BLOCKS);
        this.ticketOwner = ticketOwner;
        this.surveyChunks = List.copyOf(HbmNuclearChunkMath.orderedCircle(this.origin, this.surveyRadius));
        this.refillChunks = List.copyOf(HbmNuclearChunkMath.orderedCircle(this.origin, this.craterRadius));
        this.phase = this.fullRadius <= 0 ? Phase.COMPLETE : phase;
    }

    int process(ServerLevel level, int candidateBudget, long deadlineNanos, HbmNuclearChunkLoadBudget ticketBudget) {
        if (this.phase == Phase.COMPLETE || candidateBudget <= 0) {
            return 0;
        }

        return switch (this.phase) {
            case SURVEY -> processSurvey(level, candidateBudget, deadlineNanos, ticketBudget);
            case CLASSIFY -> processClassification(candidateBudget, deadlineNanos);
            case MUTATE -> processMutations(level, candidateBudget, deadlineNanos, ticketBudget);
            case REFILL -> processRefill(level, candidateBudget, deadlineNanos, ticketBudget);
            case COMPLETE -> 0;
        };
    }

    boolean isComplete() {
        return this.phase == Phase.COMPLETE;
    }

    BlockPos origin() {
        return this.origin;
    }

    int radius() {
        return this.fullRadius;
    }

    int progressPercent() {
        return switch (this.phase) {
            case SURVEY -> scaledProgress(this.chunkIndex, this.surveyChunks.size(), 0, 35);
            case CLASSIFY -> {
                int remaining = this.remainingWater == null ? this.classificationTotal : this.remainingWater.size();
                yield scaledProgress(this.classificationTotal - remaining, this.classificationTotal, 35, 55);
            }
            case MUTATE -> scaledProgress(this.mutationChunkIndex, this.mutationChunks.size(), 55, 80);
            case REFILL -> scaledProgress(this.refillChunkIndex, this.refillChunks.size(), 80, 99);
            case COMPLETE -> 100;
        };
    }

    UUID ticketOwner() {
        return this.ticketOwner;
    }

    CompoundTag save() {
        CompoundTag tag = new CompoundTag();
        tag.putInt("Version", SAVE_VERSION);
        tag.putLong("Origin", this.origin.asLong());
        tag.putInt("FullRadius", this.fullRadius);
        tag.putInt("Radius", this.fullRadius);
        tag.putInt("CraterRadius", this.craterRadius);
        tag.putInt("TransitionBlocks", this.transitionBlocks);
        tag.putUUID("TicketOwner", this.ticketOwner);
        tag.putString("Phase", persistedPhase().name());
        tag.putLong("Inspected", this.inspected);
        tag.putInt("Evaporated", this.evaporated);
        tag.putInt("Contaminated", this.contaminated);
        tag.putInt("Refilled", this.refilled);
        tag.putInt("RefillWaterLevel", this.refillWaterLevel);
        tag.putInt("MutationChunkIndex", this.mutationChunkIndex);
        tag.putInt("MutationPrefetchIndex", this.mutationPrefetchIndex);
        tag.putInt("RefillChunkIndex", this.refillChunkIndex);
        tag.putInt("RefillPrefetchIndex", this.refillPrefetchIndex);
        tag.putInt("RefillColumnIndex", this.refillColumnIndex);
        tag.putInt("RefillY", this.refillY);
        tag.putBoolean("Complete", this.phase == Phase.COMPLETE);
        tag.put("ActiveTickets", new LongArrayTag(this.activeTickets.stream().mapToLong(Long::longValue).toArray()));

        ListTag mutationTags = new ListTag();
        for (MutationChunk chunk : this.mutationChunks) {
            mutationTags.add(chunk.save());
        }
        tag.put("MutationChunks", mutationTags);
        return tag;
    }

    static HbmWaterVaporizationJob load(CompoundTag tag) {
        int fullRadius = tag.contains("FullRadius") ? tag.getInt("FullRadius") : tag.getInt("Radius");
        int craterRadius = tag.contains("CraterRadius") ? tag.getInt("CraterRadius") : fullRadius;
        int transitionBlocks = tag.contains("TransitionBlocks") ? tag.getInt("TransitionBlocks") : 0;
        UUID owner = tag.hasUUID("TicketOwner") ? tag.getUUID("TicketOwner") : UUID.randomUUID();

        Phase savedPhase = readPhase(tag);
        // Survey state is intentionally not serialized: it has not changed the world, so a reload safely restarts it.
        if (tag.getInt("Version") < SAVE_VERSION || savedPhase == Phase.SURVEY || savedPhase == Phase.CLASSIFY) {
            HbmWaterVaporizationJob reset = new HbmWaterVaporizationJob(
                    BlockPos.of(tag.getLong("Origin")), fullRadius, craterRadius, transitionBlocks, owner, Phase.SURVEY
            );
            for (long ticket : tag.getLongArray("ActiveTickets")) {
                reset.activeTickets.add(ticket);
            }
            reset.restartTickets = !reset.isComplete();
            return reset;
        }

        HbmWaterVaporizationJob job = new HbmWaterVaporizationJob(
                BlockPos.of(tag.getLong("Origin")), fullRadius, craterRadius, transitionBlocks, owner, savedPhase
        );
        job.inspected = tag.getLong("Inspected");
        job.evaporated = tag.getInt("Evaporated");
        job.contaminated = tag.getInt("Contaminated");
        job.refilled = tag.getInt("Refilled");
        job.refillWaterLevel = tag.contains("RefillWaterLevel")
                ? tag.getInt("RefillWaterLevel") : NO_REFILL_LEVEL;
        job.mutationChunkIndex = tag.getInt("MutationChunkIndex");
        job.mutationPrefetchIndex = tag.getInt("MutationPrefetchIndex");
        job.refillChunkIndex = tag.getInt("RefillChunkIndex");
        job.refillPrefetchIndex = tag.getInt("RefillPrefetchIndex");
        job.refillColumnIndex = tag.getInt("RefillColumnIndex");
        job.refillY = tag.contains("RefillY") ? tag.getInt("RefillY") : UNINITIALIZED_REFILL_Y;
        ListTag mutationTags = tag.getList("MutationChunks", Tag.TAG_COMPOUND);
        for (int index = 0; index < mutationTags.size(); index++) {
            job.mutationChunks.add(MutationChunk.load(mutationTags.getCompound(index)));
        }
        for (long ticket : tag.getLongArray("ActiveTickets")) {
            job.activeTickets.add(ticket);
        }
        job.restartTickets = !job.isComplete();
        return job;
    }

    private int processSurvey(ServerLevel level, int candidateBudget, long deadlineNanos,
            HbmNuclearChunkLoadBudget ticketBudget) {
        repairTicketWindow(level, this.surveyChunks, this.chunkIndex, this.prefetchIndex);
        this.prefetchIndex = prefetch(level, ticketBudget, this.surveyChunks, this.prefetchIndex);
        int used = 0;
        while (this.phase == Phase.SURVEY && used < candidateBudget && System.nanoTime() < deadlineNanos) {
            if (this.chunkIndex >= this.surveyChunks.size()) {
                beginClassification(level);
                break;
            }

            long chunkKey = this.surveyChunks.get(this.chunkIndex);
            if (!isChunkReady(level, chunkKey)) {
                if (!HbmConfig.BOMBS.forceLoadNuclearWork.get()) {
                    advanceSurveyChunk(level, chunkKey);
                    continue;
                }
                break;
            }

            int chunkX = ChunkPos.getX(chunkKey);
            int chunkZ = ChunkPos.getZ(chunkKey);
            LevelChunk chunk = level.getChunk(chunkX, chunkZ);
            LevelChunkSection[] sections = chunk.getSections();
            if (this.sectionIndex == UNINITIALIZED_SECTION) {
                this.sectionIndex = sections.length - 1;
            }
            if (this.sectionIndex < 0) {
                advanceSurveyChunk(level, chunkKey);
                this.prefetchIndex = prefetch(level, ticketBudget, this.surveyChunks, this.prefetchIndex);
                continue;
            }

            LevelChunkSection section = sections[this.sectionIndex];
            if (!section.maybeHas(HbmWaterVaporizationJob::isSurveyWater)) {
                advanceSurveySection();
                continue;
            }

            int baseX = chunkX << 4;
            int baseY = level.getMinBuildHeight() + (this.sectionIndex << 4);
            int baseZ = chunkZ << 4;
            BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
            while (this.cellIndex < LevelChunkSection.SECTION_SIZE
                    && used < candidateBudget && System.nanoTime() < deadlineNanos) {
                int localY = this.cellIndex >>> 8;
                int localX = (this.cellIndex >>> 4) & 15;
                int localZ = this.cellIndex & 15;
                this.cellIndex++;
                this.inspected++;
                used++;

                int x = baseX + localX;
                int z = baseZ + localZ;
                if (!HbmNuclearChunkMath.isInsideHorizontalRadius(this.origin, x, z, this.surveyRadius)) {
                    continue;
                }
                BlockState state = section.getBlockState(localX, localY, localZ);
                if (!isSurveyWater(state)) {
                    continue;
                }

                pos.set(x, baseY + localY, z);
                long packed = pos.asLong();
                this.surveyedWater.add(packed);
            }
            if (this.cellIndex >= LevelChunkSection.SECTION_SIZE) {
                advanceSurveySection();
            }
        }
        return used;
    }

    private int processClassification(int candidateBudget, long deadlineNanos) {
        int used = 0;
        int volumeCap = HbmConfig.BOMBS.nukeBoyMaxContainedWaterBlocks.get();
        while (this.phase == Phase.CLASSIFY && used < candidateBudget && System.nanoTime() < deadlineNanos) {
            if (this.componentStack.isEmpty()) {
                if (!this.componentInside.isEmpty()) {
                    finishComponent(volumeCap);
                    continue;
                }
                if (this.remainingWater == null || this.remainingWater.isEmpty()) {
                    finishClassification();
                    break;
                }
                LongIterator iterator = this.remainingWater.iterator();
                long seed = iterator.nextLong();
                iterator.remove();
                this.componentStack.add(seed);
                resetComponentState();
            }

            long packed = this.componentStack.removeLong(this.componentStack.size() - 1);
            used++;
            int x = BlockPos.getX(packed);
            int y = BlockPos.getY(packed);
            int z = BlockPos.getZ(packed);
            boolean inside = HbmNuclearChunkMath.isInsideHorizontalRadius(this.origin, x, z, this.fullRadius);
            if (!inside) {
                this.componentPersistent = true;
            } else {
                this.componentInside.add(packed);
                if (this.componentInside.size() > volumeCap) {
                    this.componentPersistent = true;
                }
            }

            for (int[] offset : NEIGHBORS) {
                long neighbor = BlockPos.asLong(x + offset[0], y + offset[1], z + offset[2]);
                if (this.remainingWater.remove(neighbor)) {
                    this.componentStack.add(neighbor);
                }
            }
        }
        return used;
    }

    private int processMutations(ServerLevel level, int candidateBudget, long deadlineNanos,
            HbmNuclearChunkLoadBudget ticketBudget) {
        List<Long> chunks = mutationChunkKeys();
        repairTicketWindow(level, chunks, this.mutationChunkIndex, this.mutationPrefetchIndex);
        this.mutationPrefetchIndex = prefetch(level, ticketBudget, chunks, this.mutationPrefetchIndex);
        int used = 0;
        while (this.phase == Phase.MUTATE && used < candidateBudget && System.nanoTime() < deadlineNanos) {
            if (this.mutationChunkIndex >= this.mutationChunks.size()) {
                beginRefill(level);
                break;
            }
            MutationChunk chunk = this.mutationChunks.get(this.mutationChunkIndex);
            if (!isChunkReady(level, chunk.chunkKey)) {
                if (!HbmConfig.BOMBS.forceLoadNuclearWork.get()) {
                    advanceMutationChunk(level, chunk.chunkKey);
                    continue;
                }
                break;
            }

            MutationResult result = chunk.process(
                    level,
                    candidateBudget - used,
                    deadlineNanos,
                    this.origin,
                    this.craterRadius,
                    this.refillWaterLevel
            );
            used += result.used();
            this.evaporated += result.evaporated();
            this.contaminated += result.contaminated();
            this.refillWaterLevel = Math.max(this.refillWaterLevel, result.refillWaterLevel());
            if (chunk.isComplete()) {
                advanceMutationChunk(level, chunk.chunkKey);
                this.mutationPrefetchIndex = prefetch(level, ticketBudget, chunks, this.mutationPrefetchIndex);
            }
            if (result.used() == 0 && !chunk.isComplete()) {
                break;
            }
        }
        return used;
    }

    private int processRefill(ServerLevel level, int candidateBudget, long deadlineNanos,
            HbmNuclearChunkLoadBudget ticketBudget) {
        repairTicketWindow(level, this.refillChunks, this.refillChunkIndex, this.refillPrefetchIndex);
        this.refillPrefetchIndex = prefetch(level, ticketBudget, this.refillChunks, this.refillPrefetchIndex);
        int used = 0;
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        BlockState contaminatedWater = contaminatedWaterState();
        while (this.phase == Phase.REFILL && used < candidateBudget && System.nanoTime() < deadlineNanos) {
            if (this.refillChunkIndex >= this.refillChunks.size()) {
                finish(level);
                break;
            }

            long chunkKey = this.refillChunks.get(this.refillChunkIndex);
            if (!isChunkReady(level, chunkKey)) {
                if (!HbmConfig.BOMBS.forceLoadNuclearWork.get()) {
                    advanceRefillChunk(level, chunkKey);
                    continue;
                }
                break;
            }

            int chunkX = ChunkPos.getX(chunkKey);
            int chunkZ = ChunkPos.getZ(chunkKey);
            int localX = (this.refillColumnIndex >>> 4) & 15;
            int localZ = this.refillColumnIndex & 15;
            int x = (chunkX << 4) | localX;
            int z = (chunkZ << 4) | localZ;
            if (!HbmNuclearChunkMath.isInsideHorizontalRadius(this.origin, x, z, this.craterRadius)) {
                advanceRefillColumn(level, chunkKey, ticketBudget);
                continue;
            }

            if (this.refillY == UNINITIALIZED_REFILL_Y) {
                int floor = level.getHeight(Heightmap.Types.OCEAN_FLOOR, x, z);
                this.refillY = Math.max(level.getMinBuildHeight(), floor);
            }
            int top = Math.min(this.refillWaterLevel, level.getMaxBuildHeight() - 1);
            if (this.refillY > top) {
                advanceRefillColumn(level, chunkKey, ticketBudget);
                continue;
            }

            pos.set(x, this.refillY++, z);
            used++;
            BlockState state = level.getBlockState(pos);
            if (state.isAir() || isSurveyWater(state)) {
                level.setBlock(pos, contaminatedWater,
                        Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE | Block.UPDATE_SUPPRESS_DROPS);
                this.refilled++;
            }
        }
        return used;
    }

    private void beginClassification(ServerLevel level) {
        releaseTicketWindow(level);
        this.remainingWater = new LongOpenHashSet(this.surveyedWater);
        this.classificationTotal = this.remainingWater.size();
        this.surveyedWater.clear();
        this.phase = Phase.CLASSIFY;
        HbmNuclearTech.LOGGER.info(
                "Nuclear water survey at {} found {} connected-water candidates for classification",
                this.origin,
                this.classificationTotal
        );
    }

    private void resetComponentState() {
        this.componentInside.clear();
        this.componentPersistent = false;
    }

    private void finishComponent(int volumeCap) {
        boolean evaporates = HbmWaterVaporizationMath.shouldEvaporateComponent(
                this.componentPersistent,
                this.componentInside.size(),
                volumeCap
        );
        Map<Long, LongArrayList> destination = evaporates
                ? this.evaporationBuilders : this.contaminationBuilders;
        for (long packed : this.componentInside) {
            destination.computeIfAbsent(chunkKey(packed), ignored -> new LongArrayList()).add(packed);
        }
        this.componentInside.clear();
        this.componentPersistent = false;
    }

    private void finishClassification() {
        Set<Long> chunkKeys = new HashSet<>();
        chunkKeys.addAll(this.evaporationBuilders.keySet());
        chunkKeys.addAll(this.contaminationBuilders.keySet());
        List<Long> orderedKeys = new ArrayList<>(chunkKeys);
        orderedKeys.sort(HbmNuclearChunkMath.centerOutComparator(this.origin));
        for (long chunkKey : orderedKeys) {
            LongArrayList evaporate = this.evaporationBuilders.get(chunkKey);
            LongArrayList contaminate = this.contaminationBuilders.get(chunkKey);
            this.mutationChunks.add(new MutationChunk(
                    chunkKey,
                    evaporate == null ? new long[0] : evaporate.toLongArray(),
                    contaminate == null ? new long[0] : contaminate.toLongArray(),
                    0,
                    0
            ));
        }
        this.remainingWater = null;
        this.evaporationBuilders.clear();
        this.contaminationBuilders.clear();
        this.mutationPrefetchIndex = 0;
        this.phase = this.mutationChunks.isEmpty() ? Phase.REFILL : Phase.MUTATE;
        if (this.phase == Phase.REFILL && this.refillWaterLevel == NO_REFILL_LEVEL) {
            this.phase = Phase.COMPLETE;
        }
    }

    private void beginRefill(ServerLevel level) {
        releaseTicketWindow(level);
        this.refillChunkIndex = 0;
        this.refillPrefetchIndex = 0;
        this.refillColumnIndex = 0;
        this.refillY = UNINITIALIZED_REFILL_Y;
        if (this.refillWaterLevel == NO_REFILL_LEVEL || this.refillChunks.isEmpty()) {
            finish(level);
        } else {
            this.phase = Phase.REFILL;
        }
    }

    private void finish(ServerLevel level) {
        releaseTicketWindow(level);
        this.phase = Phase.COMPLETE;
        HbmNuclearTech.LOGGER.info(
                "Nuclear water resolution completed at {}: {} evaporated, {} contaminated, {} crater refill blocks",
                this.origin,
                this.evaporated,
                this.contaminated,
                this.refilled
        );
    }

    private int prefetch(ServerLevel level, HbmNuclearChunkLoadBudget ticketBudget, List<Long> chunks,
            int currentPrefetchIndex) {
        if (!HbmConfig.BOMBS.forceLoadNuclearWork.get()) {
            return currentPrefetchIndex;
        }
        return HbmNuclearTicketWindow.prefetch(
                chunks,
                this.activeTickets,
                currentPrefetchIndex,
                HbmConfig.BOMBS.nuclearActiveChunkWindow.get(),
                chunkKey -> ticketBudget.ensureForced(level, this.ticketOwner, chunkKey, this.activeTickets)
        );
    }

    private void repairTicketWindow(ServerLevel level, List<Long> chunks, int currentIndex, int currentPrefetchIndex) {
        if (!HbmConfig.BOMBS.forceLoadNuclearWork.get()) {
            this.restartTickets = false;
            return;
        }
        boolean skippedCurrent = HbmNuclearTicketWindow.hasSkippedCurrentChunk(
                chunks, this.activeTickets, currentIndex, currentPrefetchIndex
        );
        if (!this.restartTickets && !skippedCurrent) {
            return;
        }
        releaseTicketWindow(level);
        if (this.phase == Phase.SURVEY) {
            this.prefetchIndex = this.chunkIndex;
        } else if (this.phase == Phase.MUTATE) {
            this.mutationPrefetchIndex = this.mutationChunkIndex;
        } else if (this.phase == Phase.REFILL) {
            this.refillPrefetchIndex = this.refillChunkIndex;
        }
        this.restartTickets = false;
    }

    private boolean isChunkReady(ServerLevel level, long chunkKey) {
        int chunkX = ChunkPos.getX(chunkKey);
        int chunkZ = ChunkPos.getZ(chunkKey);
        return (!HbmConfig.BOMBS.forceLoadNuclearWork.get() || this.activeTickets.contains(chunkKey))
                && level.hasChunk(chunkX, chunkZ);
    }

    private void advanceSurveySection() {
        this.sectionIndex--;
        this.cellIndex = 0;
    }

    private void advanceSurveyChunk(ServerLevel level, long chunkKey) {
        releaseChunk(level, chunkKey);
        this.chunkIndex++;
        this.sectionIndex = UNINITIALIZED_SECTION;
        this.cellIndex = 0;
    }

    private void advanceMutationChunk(ServerLevel level, long chunkKey) {
        releaseChunk(level, chunkKey);
        this.mutationChunkIndex++;
    }

    private void advanceRefillColumn(ServerLevel level, long chunkKey, HbmNuclearChunkLoadBudget ticketBudget) {
        this.refillColumnIndex++;
        this.refillY = UNINITIALIZED_REFILL_Y;
        if (this.refillColumnIndex < 256) {
            return;
        }
        this.refillColumnIndex = 0;
        advanceRefillChunk(level, chunkKey);
        this.refillPrefetchIndex = prefetch(level, ticketBudget, this.refillChunks, this.refillPrefetchIndex);
    }

    private void advanceRefillChunk(ServerLevel level, long chunkKey) {
        releaseChunk(level, chunkKey);
        this.refillChunkIndex++;
        this.refillColumnIndex = 0;
        this.refillY = UNINITIALIZED_REFILL_Y;
    }

    private void releaseChunk(ServerLevel level, long chunkKey) {
        if (this.activeTickets.remove(chunkKey)) {
            HbmNuclearChunkTickets.release(level, this.ticketOwner, chunkKey);
        }
    }

    private void releaseTicketWindow(ServerLevel level) {
        for (long chunkKey : this.activeTickets) {
            HbmNuclearChunkTickets.release(level, this.ticketOwner, chunkKey);
        }
        this.activeTickets.clear();
    }

    private List<Long> mutationChunkKeys() {
        List<Long> keys = new ArrayList<>(this.mutationChunks.size());
        for (MutationChunk chunk : this.mutationChunks) {
            keys.add(chunk.chunkKey);
        }
        return keys;
    }

    private Phase persistedPhase() {
        return this.phase == Phase.CLASSIFY ? Phase.SURVEY : this.phase;
    }

    private static Phase readPhase(CompoundTag tag) {
        if (tag.getBoolean("Complete")) {
            return Phase.COMPLETE;
        }
        if (!tag.contains("Phase")) {
            return Phase.SURVEY;
        }
        try {
            return Phase.valueOf(tag.getString("Phase"));
        } catch (IllegalArgumentException ignored) {
            return Phase.SURVEY;
        }
    }

    private static boolean isSurveyWater(BlockState state) {
        return state.getFluidState().is(FluidTags.WATER);
    }

    private static BlockState drainedState(BlockState state) {
        if (state.hasProperty(BlockStateProperties.WATERLOGGED)
                && state.getValue(BlockStateProperties.WATERLOGGED)) {
            return state.setValue(BlockStateProperties.WATERLOGGED, false);
        }
        return Blocks.AIR.defaultBlockState();
    }

    private static BlockState contaminatedState(BlockState state, BlockState contaminatedWater) {
        if (state.hasProperty(BlockStateProperties.WATERLOGGED)
                && state.getValue(BlockStateProperties.WATERLOGGED)) {
            return state.setValue(BlockStateProperties.WATERLOGGED, false);
        }
        return contaminatedWater;
    }

    private static BlockState contaminatedWaterState() {
        return HbmFluids.CONTAMINATED_WATER.get().defaultFluidState().createLegacyBlock();
    }

    private static int connectedRefillSurface(ServerLevel level, BlockPos waterPos,
            BlockPos origin, int craterRadius, int knownRefillWaterLevel) {
        boolean insideCrater = HbmNuclearChunkMath.isInsideHorizontalRadius(
                origin, waterPos.getX(), waterPos.getZ(), craterRadius
        );
        boolean horizontalOpening = insideCrater
                && hasHorizontalAirOpening(level, waterPos, origin, craterRadius);
        if (!insideCrater || !horizontalOpening) {
            return NO_REFILL_LEVEL;
        }

        int worldSurface = level.getHeight(Heightmap.Types.WORLD_SURFACE,
                waterPos.getX(), waterPos.getZ()) - 1;
        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos(
                waterPos.getX(), worldSurface, waterPos.getZ()
        );
        if (worldSurface >= waterPos.getY() && isSurveyWater(level.getBlockState(cursor))) {
            if (worldSurface <= knownRefillWaterLevel) {
                return NO_REFILL_LEVEL;
            }
            return HbmWaterVaporizationMath.canRefillFromOpening(true, true,
                    hasCraterAirPath(level, waterPos, origin, craterRadius))
                    ? worldSurface : NO_REFILL_LEVEL;
        }

        cursor.set(waterPos);
        int surface = waterPos.getY();
        while (surface + 1 < level.getMaxBuildHeight()) {
            cursor.setY(surface + 1);
            if (!isSurveyWater(level.getBlockState(cursor))) {
                break;
            }
            surface++;
        }
        if (surface <= knownRefillWaterLevel) {
            return NO_REFILL_LEVEL;
        }
        return HbmWaterVaporizationMath.canRefillFromOpening(true, true,
                hasCraterAirPath(level, waterPos, origin, craterRadius))
                ? surface : NO_REFILL_LEVEL;
    }

    private static boolean hasHorizontalAirOpening(ServerLevel level, BlockPos waterPos,
            BlockPos origin, int craterRadius) {
        BlockPos.MutableBlockPos neighbor = new BlockPos.MutableBlockPos();
        for (int[] offset : HORIZONTAL_NEIGHBORS) {
            neighbor.set(
                    waterPos.getX() + offset[0],
                    waterPos.getY(),
                    waterPos.getZ() + offset[2]
            );
            if (HbmNuclearChunkMath.isInsideHorizontalRadius(
                    origin, neighbor.getX(), neighbor.getZ(), craterRadius
            ) && level.getBlockState(neighbor).isAir()) {
                return true;
            }
        }
        return false;
    }

    private static boolean hasCraterAirPath(ServerLevel level, BlockPos waterPos,
            BlockPos origin, int craterRadius) {
        BlockPos.MutableBlockPos opening = new BlockPos.MutableBlockPos();
        for (int[] offset : HORIZONTAL_NEIGHBORS) {
            opening.set(
                    waterPos.getX() + offset[0],
                    waterPos.getY(),
                    waterPos.getZ() + offset[2]
            );
            if (HbmNuclearChunkMath.isInsideHorizontalRadius(
                    origin, opening.getX(), opening.getZ(), craterRadius
            ) && level.getBlockState(opening).isAir()
                    && hasClearAirLine(level, opening, origin)) {
                return true;
            }
        }
        return false;
    }

    private static boolean hasClearAirLine(ServerLevel level, BlockPos start, BlockPos end) {
        double deltaX = end.getX() - start.getX();
        double deltaY = end.getY() - start.getY();
        double deltaZ = end.getZ() - start.getZ();
        int steps = Math.max(1, (int) Math.ceil(
                Math.sqrt(deltaX * deltaX + deltaY * deltaY + deltaZ * deltaZ) * 2D
        ));
        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
        long previous = Long.MIN_VALUE;
        for (int step = 0; step <= steps; step++) {
            double progress = (double) step / steps;
            cursor.set(
                    (int) Math.floor(start.getX() + 0.5D + deltaX * progress),
                    (int) Math.floor(start.getY() + 0.5D + deltaY * progress),
                    (int) Math.floor(start.getZ() + 0.5D + deltaZ * progress)
            );
            long packed = cursor.asLong();
            if (packed == previous) {
                continue;
            }
            previous = packed;
            if (!level.getBlockState(cursor).isAir()) {
                return false;
            }
        }
        return true;
    }

    private static long chunkKey(long packedPos) {
        return ChunkPos.asLong(BlockPos.getX(packedPos) >> 4, BlockPos.getZ(packedPos) >> 4);
    }

    private static int scaledProgress(int completed, int total, int start, int end) {
        if (total <= 0) {
            return end;
        }
        double progress = Math.clamp((double) completed / total, 0D, 1D);
        return start + (int) Math.floor(progress * (end - start));
    }

    private record MutationResult(int used, int evaporated, int contaminated, int refillWaterLevel) {
    }

    private static final class MutationChunk {
        private final long chunkKey;
        private final long[] evaporationTargets;
        private final long[] contaminationTargets;
        private int evaporationCursor;
        private int contaminationCursor;

        private MutationChunk(long chunkKey, long[] evaporationTargets, long[] contaminationTargets,
                int evaporationCursor, int contaminationCursor) {
            this.chunkKey = chunkKey;
            this.evaporationTargets = evaporationTargets;
            this.contaminationTargets = contaminationTargets;
            this.evaporationCursor = Math.clamp(evaporationCursor, 0, evaporationTargets.length);
            this.contaminationCursor = Math.clamp(contaminationCursor, 0, contaminationTargets.length);
        }

        private MutationResult process(ServerLevel level, int budget, long deadlineNanos,
                BlockPos origin, int craterRadius, int knownRefillWaterLevel) {
            int used = 0;
            int evaporated = 0;
            int contaminated = 0;
            int refillWaterLevel = knownRefillWaterLevel;
            while (this.evaporationCursor < this.evaporationTargets.length
                    && used < budget && System.nanoTime() < deadlineNanos) {
                BlockPos pos = BlockPos.of(this.evaporationTargets[this.evaporationCursor++]);
                BlockState state = level.getBlockState(pos);
                if (isSurveyWater(state)) {
                    level.setBlock(pos, drainedState(state),
                            Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE | Block.UPDATE_SUPPRESS_DROPS);
                    evaporated++;
                }
                used++;
            }

            BlockState contaminatedState = contaminatedWaterState();
            while (this.evaporationCursor >= this.evaporationTargets.length
                    && this.contaminationCursor < this.contaminationTargets.length
                    && used < budget && System.nanoTime() < deadlineNanos) {
                BlockPos pos = BlockPos.of(this.contaminationTargets[this.contaminationCursor++]);
                BlockState state = level.getBlockState(pos);
                if (isSurveyWater(state) && !state.is(HbmBlocks.CONTAMINATED_WATER.get())) {
                    level.setBlock(pos, contaminatedState(state, contaminatedState),
                            Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE | Block.UPDATE_SUPPRESS_DROPS);
                    contaminated++;
                }
                if (isSurveyWater(level.getBlockState(pos))) {
                    refillWaterLevel = Math.max(
                            refillWaterLevel,
                            connectedRefillSurface(
                                    level,
                                    pos,
                                    origin,
                                    craterRadius,
                                    refillWaterLevel
                            )
                    );
                }
                used++;
            }
            return new MutationResult(used, evaporated, contaminated, refillWaterLevel);
        }

        private boolean isComplete() {
            return this.evaporationCursor >= this.evaporationTargets.length
                    && this.contaminationCursor >= this.contaminationTargets.length;
        }

        private CompoundTag save() {
            CompoundTag tag = new CompoundTag();
            tag.putLong("Chunk", this.chunkKey);
            tag.putLongArray("Evaporate", this.evaporationTargets);
            tag.putLongArray("Contaminate", this.contaminationTargets);
            tag.putInt("EvaporationCursor", this.evaporationCursor);
            tag.putInt("ContaminationCursor", this.contaminationCursor);
            return tag;
        }

        private static MutationChunk load(CompoundTag tag) {
            return new MutationChunk(
                    tag.getLong("Chunk"),
                    tag.getLongArray("Evaporate"),
                    tag.getLongArray("Contaminate"),
                    tag.getInt("EvaporationCursor"),
                    tag.getInt("ContaminationCursor")
            );
        }
    }
}
