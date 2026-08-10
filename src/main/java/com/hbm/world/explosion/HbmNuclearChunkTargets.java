package com.hbm.world.explosion;

import com.hbm.block.HbmTntBlock;
import com.hbm.registry.HbmBlocks;
import java.util.BitSet;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Compact, deduplicated target storage for a single real chunk.
 *
 * <p>One bit represents one local block position. This is both substantially
 * cheaper than storing boxed {@link BlockPos} values and compatible with saved
 * nuclear work.</p>
 */
final class HbmNuclearChunkTargets {
    private final long chunkKey;
    private final int minBuildHeight;
    private final int buildHeight;
    private final BitSet targets;
    private int destructionCursor;

    HbmNuclearChunkTargets(long chunkKey, int minBuildHeight, int buildHeight) {
        this(chunkKey, minBuildHeight, buildHeight, new BitSet(buildHeight << 8), 0);
    }

    private HbmNuclearChunkTargets(long chunkKey, int minBuildHeight, int buildHeight, BitSet targets,
            int destructionCursor) {
        this.chunkKey = chunkKey;
        this.minBuildHeight = minBuildHeight;
        this.buildHeight = buildHeight;
        this.targets = targets;
        this.destructionCursor = destructionCursor;
    }

    void add(BlockPos pos) {
        add(pos.getX(), pos.getY(), pos.getZ());
    }

    void add(int blockX, int blockY, int blockZ) {
        int y = blockY - this.minBuildHeight;
        if (y < 0 || y >= this.buildHeight) {
            return;
        }
        this.targets.set(HbmChunkSchedulerMath.localTargetBitIndex(blockX, y, blockZ));
    }

    boolean isComplete() {
        return this.targets.isEmpty();
    }

    int targetCount() {
        return this.targets.cardinality();
    }

    boolean contains(int blockX, int blockY, int blockZ) {
        int y = blockY - this.minBuildHeight;
        return y >= 0 && y < this.buildHeight
                && this.targets.get(HbmChunkSchedulerMath.localTargetBitIndex(blockX, y, blockZ));
    }

    long[] targetWords() {
        return this.targets.toLongArray();
    }

    long chunkKey() {
        return this.chunkKey;
    }

    int destroy(ServerLevel level, int blockBudget, long deadlineNanos) {
        int used = 0;
        int bit = this.targets.nextSetBit(this.destructionCursor);
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        int chunkX = ChunkPos.getX(this.chunkKey);
        int chunkZ = ChunkPos.getZ(this.chunkKey);

        while (bit >= 0 && used < blockBudget && System.nanoTime() < deadlineNanos) {
            int y = this.minBuildHeight + (bit >>> 8);
            int x = (chunkX << 4) | ((bit >>> 4) & 15);
            int z = (chunkZ << 4) | (bit & 15);
            pos.set(x, y, z);
            destroyTarget(level, pos);
            this.targets.clear(bit);
            this.destructionCursor = bit + 1;
            bit = this.targets.nextSetBit(this.destructionCursor);
            used++;
        }

        if (bit < 0) {
            this.destructionCursor = 0;
        }
        return used;
    }

    CompoundTag save() {
        CompoundTag tag = new CompoundTag();
        tag.putLong("Chunk", this.chunkKey);
        tag.putInt("MinBuildHeight", this.minBuildHeight);
        tag.putInt("BuildHeight", this.buildHeight);
        tag.putLongArray("Targets", this.targets.toLongArray());
        tag.putInt("Cursor", this.destructionCursor);
        return tag;
    }

    static HbmNuclearChunkTargets load(CompoundTag tag) {
        return new HbmNuclearChunkTargets(
                tag.getLong("Chunk"),
                tag.getInt("MinBuildHeight"),
                tag.getInt("BuildHeight"),
                BitSet.valueOf(tag.getLongArray("Targets")),
                tag.getInt("Cursor")
        );
    }

    static HbmNuclearChunkTargets fromHybridMask(HbmHybridChunkMask mask) {
        return new HbmNuclearChunkTargets(
                mask.chunkKey(),
                mask.minBuildHeight(),
                mask.buildHeight(),
                BitSet.valueOf(mask.targetWords()),
                0
        );
    }

    void merge(HbmHybridChunkMask mask) {
        if (this.chunkKey != mask.chunkKey()
                || this.minBuildHeight != mask.minBuildHeight()
                || this.buildHeight != mask.buildHeight()) {
            throw new IllegalArgumentException("Incompatible nuclear chunk mask");
        }
        this.targets.or(BitSet.valueOf(mask.targetWords()));
    }

    private static void destroyTarget(ServerLevel level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        if (state.isAir()) {
            return;
        }
        if (state.is(HbmBlocks.TNT.get())) {
            HbmTntBlock.primeFromExplosion(level, pos, null);
        }
        BlockState replacement = state.getFluidState().createLegacyBlock();
        level.setBlock(
                pos,
                replacement,
                Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE | Block.UPDATE_SUPPRESS_DROPS
        );
    }
}
