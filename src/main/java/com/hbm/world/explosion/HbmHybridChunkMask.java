package com.hbm.world.explosion;

import java.util.BitSet;

/** Primitive-only worker result for one chunk. */
final class HbmHybridChunkMask {
    private final long chunkKey;
    private final int minBuildHeight;
    private final int buildHeight;
    private final BitSet targets;

    HbmHybridChunkMask(long chunkKey, int minBuildHeight, int buildHeight) {
        this.chunkKey = chunkKey;
        this.minBuildHeight = minBuildHeight;
        this.buildHeight = buildHeight;
        this.targets = new BitSet(buildHeight << 8);
    }

    void add(int blockX, int blockY, int blockZ) {
        int y = blockY - this.minBuildHeight;
        if (y >= 0 && y < this.buildHeight) {
            this.targets.set(HbmChunkSchedulerMath.localTargetBitIndex(blockX, y, blockZ));
        }
    }

    long chunkKey() { return this.chunkKey; }
    int minBuildHeight() { return this.minBuildHeight; }
    int buildHeight() { return this.buildHeight; }
    int targetCount() { return this.targets.cardinality(); }

    boolean contains(int blockX, int blockY, int blockZ) {
        int y = blockY - this.minBuildHeight;
        return y >= 0 && y < this.buildHeight
                && this.targets.get(HbmChunkSchedulerMath.localTargetBitIndex(blockX, y, blockZ));
    }

    long[] targetWords() {
        return this.targets.toLongArray();
    }
}
