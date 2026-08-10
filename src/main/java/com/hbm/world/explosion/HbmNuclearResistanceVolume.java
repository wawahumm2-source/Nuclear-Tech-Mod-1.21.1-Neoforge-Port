package com.hbm.world.explosion;

/** Immutable primitive-only resistance volume safe for CPU worker threads. */
final class HbmNuclearResistanceVolume {
    static final char AIR = 0;
    static final char FLUID = 1;
    private static final float RESISTANCE_SCALE = 16F;
    private static final int MAX_RESISTANCE_CODE = Character.MAX_VALUE - 2;

    private final int originX;
    private final int originY;
    private final int originZ;
    private final int radius;
    private final int minChunkX;
    private final int minChunkZ;
    private final int chunkWidth;
    private final int chunkDepth;
    private final int minSectionY;
    private final int sectionCount;
    private final int minBuildHeight;
    private final int buildHeight;
    private final char[][] sections;

    HbmNuclearResistanceVolume(int originX, int originY, int originZ, int radius,
            int minChunkX, int minChunkZ, int chunkWidth, int chunkDepth,
            int minSectionY, int sectionCount, int minBuildHeight, int buildHeight, char[][] sections) {
        this.originX = originX;
        this.originY = originY;
        this.originZ = originZ;
        this.radius = radius;
        this.minChunkX = minChunkX;
        this.minChunkZ = minChunkZ;
        this.chunkWidth = chunkWidth;
        this.chunkDepth = chunkDepth;
        this.minSectionY = minSectionY;
        this.sectionCount = sectionCount;
        this.minBuildHeight = minBuildHeight;
        this.buildHeight = buildHeight;
        this.sections = sections;
    }

    int originX() { return this.originX; }
    int originY() { return this.originY; }
    int originZ() { return this.originZ; }
    int radius() { return this.radius; }
    int minBuildHeight() { return this.minBuildHeight; }
    int buildHeight() { return this.buildHeight; }
    int sectionArrayLength() { return this.sections.length; }
    char[] sectionAt(int index) { return this.sections[index]; }

    int allocatedSectionCount() {
        int count = 0;
        for (char[] section : this.sections) {
            if (section != null) {
                count++;
            }
        }
        return count;
    }

    long estimatedBytes() {
        return 64L + this.sections.length * 8L + allocatedSectionCount() * (16L + 4096L * Character.BYTES);
    }

    int chunkXForSectionIndex(int index) {
        int chunkPlane = this.chunkDepth * this.sectionCount;
        return this.minChunkX + index / chunkPlane;
    }

    int chunkZForSectionIndex(int index) {
        int chunkPlane = this.chunkDepth * this.sectionCount;
        return this.minChunkZ + index % chunkPlane / this.sectionCount;
    }

    int sectionYForIndex(int index) {
        return this.minSectionY + index % this.sectionCount;
    }

    char encodedAt(int blockX, int blockY, int blockZ) {
        if (blockY < this.minBuildHeight || blockY >= this.minBuildHeight + this.buildHeight) {
            return AIR;
        }
        int chunkXIndex = (blockX >> 4) - this.minChunkX;
        int chunkZIndex = (blockZ >> 4) - this.minChunkZ;
        int sectionYIndex = (blockY >> 4) - this.minSectionY;
        if (chunkXIndex < 0 || chunkXIndex >= this.chunkWidth
                || chunkZIndex < 0 || chunkZIndex >= this.chunkDepth
                || sectionYIndex < 0 || sectionYIndex >= this.sectionCount) {
            return AIR;
        }
        char[] section = this.sections[(chunkXIndex * this.chunkDepth + chunkZIndex) * this.sectionCount + sectionYIndex];
        return section == null ? AIR : section[localIndex(blockX, blockY, blockZ)];
    }

    static float decodeResistance(char encoded) {
        return encoded <= FLUID ? 0F : (encoded - 2) / RESISTANCE_SCALE;
    }

    static char encodeResistance(float resistance) {
        int quantized = Math.clamp(Math.round(Math.max(0F, resistance) * RESISTANCE_SCALE), 0, MAX_RESISTANCE_CODE);
        return (char) (quantized + 2);
    }

    static int localIndex(int blockX, int blockY, int blockZ) {
        return (blockY & 15) << 8 | (blockZ & 15) << 4 | blockX & 15;
    }
}
