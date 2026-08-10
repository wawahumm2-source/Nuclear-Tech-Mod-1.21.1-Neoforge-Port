package com.hbm.world.explosion;

import com.hbm.config.HbmConfig;
import com.hbm.registry.HbmBlocks;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.LongArrayTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.levelgen.Heightmap;

/**
 * Deterministic Sellafield/waste conversion beside MK5 excavation.
 *
 * <p>The source changes columns, not whole chunks. The port preserves that
 * block behavior while using chunks solely as the loading, persistence, and
 * scheduling boundary.</p>
 */
final class HbmBlastTerrainJob {
    private final BlockPos origin;
    private final HbmFalloutProfile profile;
    private final long seed;
    private final UUID ticketOwner;
    private final List<Long> chunks;
    private final Set<Long> activeTickets = new LinkedHashSet<>();
    private int chunkIndex;
    private int prefetchIndex;
    private int columnIndex;
    private Phase phase;
    private boolean restartTickets;

    HbmBlastTerrainJob(BlockPos origin, HbmFalloutProfile profile, long seed) {
        this(origin, profile, seed, UUID.randomUUID(), 0, 0, 0, Phase.READY);
    }

    private HbmBlastTerrainJob(BlockPos origin, HbmFalloutProfile profile, long seed, UUID ticketOwner, int chunkIndex,
            int prefetchIndex, int columnIndex, Phase phase) {
        this.origin = origin.immutable();
        this.profile = profile;
        this.seed = seed;
        this.ticketOwner = ticketOwner;
        this.chunks = HbmNuclearChunkMath.orderedCircle(this.origin, profile.radius());
        this.chunkIndex = chunkIndex;
        this.prefetchIndex = Math.max(prefetchIndex, chunkIndex);
        this.columnIndex = columnIndex;
        this.phase = phase;
    }

    /** Target planning is now the deterministic chunk list, built at construction without world access. */
    int plan(int ignoredBudget) {
        return 0;
    }

    void beginApplying() {
        if (this.phase == Phase.READY) {
            this.phase = Phase.APPLYING;
        }
    }

    int apply(ServerLevel level, int columnBudget, long deadlineNanos, HbmNuclearChunkLoadBudget ticketBudget) {
        if (this.phase != Phase.APPLYING || columnBudget <= 0) {
            return 0;
        }

        repairTicketWindow(level);
        prefetch(level, ticketBudget);
        int used = 0;
        while (this.phase == Phase.APPLYING && used < columnBudget && System.nanoTime() < deadlineNanos) {
            if (this.chunkIndex >= this.chunks.size()) {
                finish(level);
                break;
            }

            long chunkKey = this.chunks.get(this.chunkIndex);
            if (HbmConfig.BOMBS.forceLoadNuclearWork.get() && !this.activeTickets.contains(chunkKey)) {
                break;
            }
            int chunkX = ChunkPos.getX(chunkKey);
            int chunkZ = ChunkPos.getZ(chunkKey);
            if (!level.hasChunk(chunkX, chunkZ)) {
                if (!HbmConfig.BOMBS.forceLoadNuclearWork.get()) {
                    advanceChunk(level, chunkKey);
                    continue;
                }
                break;
            }
            LevelChunk chunk = level.getChunk(chunkX, chunkZ);

            while (this.columnIndex < 256 && used < columnBudget && System.nanoTime() < deadlineNanos) {
                int x = (chunkX << 4) + (this.columnIndex >>> 4);
                int z = (chunkZ << 4) + (this.columnIndex & 15);
                this.columnIndex++;
                int offsetX = x - this.origin.getX();
                int offsetZ = z - this.origin.getZ();
                if (HbmFalloutMath.isInsideRadius(offsetX, offsetZ, this.profile.radius())) {
                    processColumn(level, chunk, x, z);
                }
                used++;
            }
            if (this.columnIndex >= 256) {
                advanceChunk(level, chunkKey);
                prefetch(level, ticketBudget);
            }
        }
        return used;
    }

    boolean isPlanning() {
        return false;
    }

    boolean isReady() {
        return this.phase == Phase.READY;
    }

    boolean isComplete() {
        return this.phase == Phase.COMPLETE;
    }

    UUID ticketOwner() {
        return this.ticketOwner;
    }

    int progressPercent() {
        if (this.phase == Phase.COMPLETE) {
            return 100;
        }
        int completedColumns = this.chunkIndex * 256 + this.columnIndex;
        return Math.clamp((int) Math.floor((double) completedColumns * 100D / Math.max(1, this.chunks.size() * 256)), 0, 99);
    }

    CompoundTag save() {
        CompoundTag tag = new CompoundTag();
        tag.putUUID("TicketOwner", this.ticketOwner);
        tag.putInt("ChunkIndex", this.chunkIndex);
        tag.putInt("PrefetchIndex", this.prefetchIndex);
        tag.putInt("ColumnIndex", this.columnIndex);
        tag.putString("Phase", this.phase.name());
        tag.put("ActiveTickets", new LongArrayTag(this.activeTickets.stream().mapToLong(Long::longValue).toArray()));
        return tag;
    }

    static HbmBlastTerrainJob load(BlockPos origin, HbmFalloutProfile profile, long seed, CompoundTag tag) {
        String storedPhase = tag.getString("Phase");
        Phase phase;
        try {
            phase = storedPhase.isBlank() ? Phase.READY : Phase.valueOf(storedPhase);
        } catch (IllegalArgumentException ignored) {
            phase = Phase.READY;
        }
        HbmBlastTerrainJob job = new HbmBlastTerrainJob(
                origin,
                profile,
                seed,
                tag.hasUUID("TicketOwner") ? tag.getUUID("TicketOwner") : UUID.randomUUID(),
                tag.getInt("ChunkIndex"),
                tag.getInt("PrefetchIndex"),
                tag.getInt("ColumnIndex"),
                phase
        );
        for (long ticket : tag.getLongArray("ActiveTickets")) {
            job.activeTickets.add(ticket);
        }
        job.restartTickets = phase != Phase.COMPLETE;
        return job;
    }

    private void prefetch(ServerLevel level, HbmNuclearChunkLoadBudget ticketBudget) {
        if (!HbmConfig.BOMBS.forceLoadNuclearWork.get()) {
            return;
        }
        this.prefetchIndex = HbmNuclearTicketWindow.prefetch(
                this.chunks,
                this.activeTickets,
                this.prefetchIndex,
                HbmConfig.BOMBS.nuclearActiveChunkWindow.get(),
                chunkKey -> ticketBudget.ensureForced(level, this.ticketOwner, chunkKey, this.activeTickets)
        );
    }

    private void repairTicketWindow(ServerLevel level) {
        if (!HbmConfig.BOMBS.forceLoadNuclearWork.get()) {
            this.restartTickets = false;
            return;
        }
        boolean skippedCurrent = HbmNuclearTicketWindow.hasSkippedCurrentChunk(
                this.chunks, this.activeTickets, this.chunkIndex, this.prefetchIndex
        );
        if (!this.restartTickets && !skippedCurrent) {
            return;
        }
        releaseTicketWindow(level);
        this.prefetchIndex = this.chunkIndex;
        this.restartTickets = false;
    }

    private void advanceChunk(ServerLevel level, long chunkKey) {
        if (this.activeTickets.remove(chunkKey)) {
            HbmNuclearChunkTickets.release(level, this.ticketOwner, chunkKey);
        }
        this.chunkIndex++;
        this.columnIndex = 0;
        if (this.chunkIndex >= this.chunks.size()) {
            finish(level);
        }
    }

    private void finish(ServerLevel level) {
        releaseTicketWindow(level);
        this.phase = Phase.COMPLETE;
    }

    private void releaseTicketWindow(ServerLevel level) {
        for (long chunkKey : this.activeTickets) {
            HbmNuclearChunkTickets.release(level, this.ticketOwner, chunkKey);
        }
        this.activeTickets.clear();
    }

    private void processColumn(ServerLevel level, LevelChunk chunk, int x, int z) {
        int offsetX = x - this.origin.getX();
        int offsetZ = z - this.origin.getZ();
        double distancePercent = HbmFalloutMath.distancePercent(offsetX, offsetZ, this.profile.radius());
        int depth = 0;
        int surfaceY = chunk.getHeight(Heightmap.Types.WORLD_SURFACE, x, z);
        int minBuildHeight = level.getMinBuildHeight();
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos(x, surfaceY, z);

        for (int y = surfaceY; y >= minBuildHeight && depth < this.profile.processedDepth(); y--) {
            pos.setY(y);
            BlockState source = chunk.getBlockState(pos);
            if (source.isAir() || source.is(HbmBlocks.FALLOUT.get()) || source.is(HbmBlocks.NUCLEAR_FIRE.get())) {
                continue;
            }
            HbmFalloutMapper.Mapping mapping = HbmFalloutMapper.map(
                    source, pos, distancePercent, this.seed, this.profile.woodEffectPercent()
            );
            BlockState result = mapping.state();
            if (y == minBuildHeight && !result.is(HbmBlocks.SELLAFIELD_BEDROCK.get())) {
                result = source;
            }
            boolean changed = !result.equals(source);
            if (changed) {
                level.setBlock(pos, result,
                        Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE | Block.UPDATE_SUPPRESS_DROPS);
            }
            if (changed && mapping.consumesDepth()) {
                depth++;
            } else if (!changed && source.isCollisionShapeFullBlock(level, pos)) {
                depth++;
            }
        }
    }

    private enum Phase {
        READY,
        APPLYING,
        COMPLETE
    }
}
