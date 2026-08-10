package com.hbm.world.explosion;

import com.hbm.config.HbmConfig;
import com.hbm.registry.HbmBlocks;
import com.hbm.world.radiation.ChunkRadiationService;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.LongArrayTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;

/**
 * Persisted, source-shaped Fallout Rain work grouped by actual world chunks.
 *
 * <p>Every horizontal column is still processed independently, as in the
 * source. Chunk grouping only controls force-loading, ordering, persistence,
 * and ticket release.</p>
 */
final class HbmFalloutRainJob {
    private final BlockPos origin;
    private final HbmFalloutProfile profile;
    private final long seed;
    private final UUID ticketOwner;
    private final List<Long> chunks;
    private final Set<Long> activeTickets = new LinkedHashSet<>();
    private int chunkIndex;
    private int prefetchIndex;
    private int columnIndex;
    private boolean complete;
    private boolean restartTickets;
    private final Set<Long> irradiatedChunks = new LinkedHashSet<>();

    HbmFalloutRainJob(BlockPos origin, HbmFalloutProfile profile, long seed) {
        this(origin, profile, seed, UUID.randomUUID(), 0, 0, 0, false);
    }

    private HbmFalloutRainJob(BlockPos origin, HbmFalloutProfile profile, long seed, UUID ticketOwner, int chunkIndex,
            int prefetchIndex, int columnIndex, boolean complete) {
        this.origin = origin.immutable();
        this.profile = profile;
        this.seed = seed;
        this.ticketOwner = ticketOwner;
        this.chunks = HbmNuclearChunkMath.orderedCircle(this.origin, profile.radius());
        this.chunkIndex = chunkIndex;
        this.prefetchIndex = Math.max(prefetchIndex, chunkIndex);
        this.columnIndex = columnIndex;
        this.complete = complete || this.chunks.isEmpty();
    }

    int process(ServerLevel level, int columnBudget, long deadlineNanos, HbmNuclearChunkLoadBudget ticketBudget) {
        if (this.complete || columnBudget <= 0) {
            return 0;
        }

        repairTicketWindow(level);
        prefetch(level, ticketBudget);
        int used = 0;
        while (!this.complete && used < columnBudget && System.nanoTime() < deadlineNanos) {
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

            seedCraterRadiation(level, chunkX, chunkZ);
            while (this.columnIndex < 256 && used < columnBudget && System.nanoTime() < deadlineNanos) {
                int x = (chunkX << 4) + (this.columnIndex >>> 4);
                int z = (chunkZ << 4) + (this.columnIndex & 15);
                this.columnIndex++;
                int offsetX = x - this.origin.getX();
                int offsetZ = z - this.origin.getZ();
                if (HbmFalloutMath.isInsideRadius(offsetX, offsetZ, this.profile.radius())) {
                    processColumn(level, x, z, offsetX, offsetZ);
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

    boolean isComplete() {
        return this.complete;
    }

    BlockPos origin() {
        return this.origin;
    }

    HbmFalloutProfile profile() {
        return this.profile;
    }

    UUID ticketOwner() {
        return this.ticketOwner;
    }

    int progressPercent() {
        if (this.complete) {
            return 100;
        }
        int completedColumns = this.chunkIndex * 256 + this.columnIndex;
        return Math.clamp((int) Math.floor((double) completedColumns * 100D / Math.max(1, this.chunks.size() * 256)), 0, 99);
    }

    CompoundTag save() {
        CompoundTag tag = new CompoundTag();
        tag.putLong("Origin", this.origin.asLong());
        tag.putLong("Seed", this.seed);
        tag.putUUID("TicketOwner", this.ticketOwner);
        tag.putInt("ChunkIndex", this.chunkIndex);
        tag.putInt("PrefetchIndex", this.prefetchIndex);
        tag.putInt("ColumnIndex", this.columnIndex);
        tag.putBoolean("Complete", this.complete);
        tag.put("IrradiatedChunks", new LongArrayTag(this.irradiatedChunks.stream().mapToLong(Long::longValue).toArray()));
        tag.put("ActiveTickets", new LongArrayTag(this.activeTickets.stream().mapToLong(Long::longValue).toArray()));
        this.profile.save(tag);
        return tag;
    }

    static HbmFalloutRainJob load(CompoundTag tag) {
        HbmFalloutRainJob job = new HbmFalloutRainJob(
                BlockPos.of(tag.getLong("Origin")),
                HbmFalloutProfile.load(tag),
                tag.getLong("Seed"),
                tag.hasUUID("TicketOwner") ? tag.getUUID("TicketOwner") : UUID.randomUUID(),
                tag.getInt("ChunkIndex"),
                tag.getInt("PrefetchIndex"),
                tag.getInt("ColumnIndex"),
                tag.getBoolean("Complete")
        );
        for (long chunk : tag.getLongArray("IrradiatedChunks")) {
            job.irradiatedChunks.add(chunk);
        }
        for (long ticket : tag.getLongArray("ActiveTickets")) {
            job.activeTickets.add(ticket);
        }
        job.restartTickets = !job.complete;
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
        this.complete = true;
    }

    private void releaseTicketWindow(ServerLevel level) {
        for (long chunkKey : this.activeTickets) {
            HbmNuclearChunkTickets.release(level, this.ticketOwner, chunkKey);
        }
        this.activeTickets.clear();
    }

    private void processColumn(ServerLevel level, int x, int z, int offsetX, int offsetZ) {
        double distancePercent = HbmFalloutMath.distancePercent(offsetX, offsetZ, this.profile.radius());
        int depth = 0;
        int surfaceY = level.getHeight(Heightmap.Types.WORLD_SURFACE, x, z) - 1;

        for (int y = surfaceY; y >= level.getMinBuildHeight() && depth < this.profile.processedDepth(); y--) {
            BlockPos pos = new BlockPos(x, y, z);
            BlockState source = level.getBlockState(pos);
            if (source.isAir() || source.is(HbmBlocks.FALLOUT.get()) || source.is(HbmBlocks.NUCLEAR_FIRE.get())) {
                continue;
            }

            if (depth == 0 && this.profile.falloutDeposits()) {
                placeFalloutDeposit(level, pos, distancePercent);
            }
            if (this.profile.firePlacement()) {
                placeSourceStyleFire(level, pos, source, distancePercent);
            }
            if (source.isCollisionShapeFullBlock(level, pos)) {
                depth++;
            }
        }
    }

    private void seedCraterRadiation(ServerLevel level, int chunkX, int chunkZ) {
        long chunkKey = ChunkPos.asLong(chunkX, chunkZ);
        if (!this.irradiatedChunks.add(chunkKey)) {
            return;
        }

        int minX = chunkX << 4;
        int maxX = minX + 15;
        int minZ = chunkZ << 4;
        int maxZ = minZ + 15;
        int originX = this.origin.getX();
        int originZ = this.origin.getZ();
        double deltaX = originX < minX ? minX - originX : originX > maxX ? originX - maxX : 0D;
        double deltaZ = originZ < minZ ? minZ - originZ : originZ > maxZ ? originZ - maxZ : 0D;
        double distancePercent = Math.hypot(deltaX, deltaZ) * 100D / Math.max(1, this.profile.radius());
        double rate = HbmFalloutMath.craterRadiationRate(
                distancePercent,
                this.profile.radius(),
                this.profile.innerRadiationRate(),
                this.profile.craterRadiationRate(),
                this.profile.outerRadiationRate()
        );
        if (rate <= 0D) {
            return;
        }
        ChunkPos chunkPos = new ChunkPos(chunkX, chunkZ);
        if (ChunkRadiationService.getRadiation(level, chunkPos) < rate) {
            ChunkRadiationService.setRadiation(level, chunkPos, rate);
        }
    }

    private void placeFalloutDeposit(ServerLevel level, BlockPos ground, double distancePercent) {
        if (HbmFalloutMath.deterministicUnit(this.seed, ground.getX(), ground.getY(), ground.getZ(), 17)
                >= HbmFalloutMath.falloutDepositChance(distancePercent)) {
            return;
        }
        BlockPos depositPos = ground.above();
        BlockState deposit = HbmBlocks.FALLOUT.get().defaultBlockState();
        if (level.getBlockState(depositPos).isAir() && deposit.canSurvive(level, depositPos)) {
            level.setBlock(depositPos, deposit, 3);
        }
    }

    private void placeSourceStyleFire(ServerLevel level, BlockPos pos, BlockState source, double distancePercent) {
        if (distancePercent >= this.profile.woodEffectPercent()
                || !source.isFlammable(level, pos, Direction.UP)
                || !HbmFalloutMath.shouldPlaceFire(distancePercent,
                        HbmFalloutMath.deterministicRoll(this.seed, pos.getX(), pos.getY(), pos.getZ(), 23,
                                this.profile.fireChanceDenominator()))) {
            return;
        }
        BlockPos firePos = pos.above();
        if (level.getBlockState(firePos).isAir()) {
            level.setBlock(firePos, HbmBlocks.NUCLEAR_FIRE.get().defaultBlockState(), 3);
        }
    }
}
