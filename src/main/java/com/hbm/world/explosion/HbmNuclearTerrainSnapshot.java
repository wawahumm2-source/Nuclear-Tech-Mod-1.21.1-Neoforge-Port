package com.hbm.world.explosion;

import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.LevelChunkSection;

/** Server-thread builder that copies loaded terrain into a primitive worker volume. */
final class HbmNuclearTerrainSnapshot {
    static Builder builder(ServerLevel level, BlockPos origin, int radius, List<Long> craterChunks) {
        return new Builder(level, origin, radius, craterChunks);
    }

    static final class Builder {
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
        private final List<Long> craterChunks;
        private final char[][] sections;
        private final Map<BlockState, Character> vanillaResistanceCodes = new IdentityHashMap<>();
        private final BlockPos.MutableBlockPos position = new BlockPos.MutableBlockPos();
        private final float stoneResistance;
        private int cursor;

        private Builder(ServerLevel level, BlockPos origin, int radius, List<Long> craterChunks) {
            this.originX = origin.getX();
            this.originY = origin.getY();
            this.originZ = origin.getZ();
            this.radius = radius;
            this.minChunkX = (this.originX - radius) >> 4;
            int maxChunkX = (this.originX + radius) >> 4;
            this.minChunkZ = (this.originZ - radius) >> 4;
            int maxChunkZ = (this.originZ + radius) >> 4;
            this.chunkWidth = maxChunkX - this.minChunkX + 1;
            this.chunkDepth = maxChunkZ - this.minChunkZ + 1;
            this.minBuildHeight = level.getMinBuildHeight();
            this.buildHeight = level.getHeight();
            int minY = Math.max(this.minBuildHeight, this.originY - radius);
            int maxY = Math.min(level.getMaxBuildHeight() - 1, this.originY + radius);
            this.minSectionY = minY >> 4;
            int maxSectionY = maxY >> 4;
            this.sectionCount = maxSectionY - this.minSectionY + 1;
            this.craterChunks = List.copyOf(craterChunks);
            this.sections = new char[this.chunkWidth * this.chunkDepth * this.sectionCount][];
            this.stoneResistance = Blocks.STONE.defaultBlockState().getExplosionResistance(level, origin, null);
        }

        int capture(ServerLevel level, int sectionBudget, long deadlineNanos) {
            int total = totalSectionAddresses();
            int used = 0;
            while (this.cursor < total && used < sectionBudget && System.nanoTime() < deadlineNanos) {
                int chunkIndex = this.cursor / this.sectionCount;
                int sectionOffset = this.cursor % this.sectionCount;
                long chunkKey = this.craterChunks.get(chunkIndex);
                if (!level.hasChunk(ChunkPos.getX(chunkKey), ChunkPos.getZ(chunkKey))) {
                    break;
                }
                captureSection(level, chunkKey, this.minSectionY + sectionOffset);
                this.cursor++;
                used++;
            }
            return used;
        }

        int completedChunkCount() {
            return Math.min(this.craterChunks.size(), this.cursor / this.sectionCount);
        }

        boolean isComplete() {
            return this.cursor >= totalSectionAddresses();
        }

        int progressPercent() {
            int total = totalSectionAddresses();
            return total == 0 ? 100 : Math.clamp(this.cursor * 100 / total, 0, 100);
        }

        HbmNuclearResistanceVolume build() {
            if (!isComplete()) {
                throw new IllegalStateException("Nuclear terrain snapshot is incomplete");
            }
            return new HbmNuclearResistanceVolume(
                    this.originX, this.originY, this.originZ, this.radius,
                    this.minChunkX, this.minChunkZ, this.chunkWidth, this.chunkDepth,
                    this.minSectionY, this.sectionCount, this.minBuildHeight, this.buildHeight, this.sections
            );
        }

        private void captureSection(ServerLevel level, long chunkKey, int sectionY) {
            int chunkX = ChunkPos.getX(chunkKey);
            int chunkZ = ChunkPos.getZ(chunkKey);
            if (chunkX < this.minChunkX || chunkX >= this.minChunkX + this.chunkWidth
                    || chunkZ < this.minChunkZ || chunkZ >= this.minChunkZ + this.chunkDepth) {
                return;
            }
            LevelChunk chunk = level.getChunk(chunkX, chunkZ);
            int levelSectionIndex = sectionY - (this.minBuildHeight >> 4);
            LevelChunkSection[] chunkSections = chunk.getSections();
            if (levelSectionIndex < 0 || levelSectionIndex >= chunkSections.length) {
                return;
            }
            LevelChunkSection source = chunkSections[levelSectionIndex];
            if (source.hasOnlyAir()) {
                return;
            }

            char[] target = new char[4096];
            boolean populated = false;
            int baseX = chunkX << 4;
            int baseY = sectionY << 4;
            int baseZ = chunkZ << 4;
            int radiusSquared = this.radius * this.radius;
            for (int localY = 0; localY < 16; localY++) {
                int blockY = baseY + localY;
                int dy = blockY - this.originY;
                for (int localZ = 0; localZ < 16; localZ++) {
                    int blockZ = baseZ + localZ;
                    int dz = blockZ - this.originZ;
                    for (int localX = 0; localX < 16; localX++) {
                        int blockX = baseX + localX;
                        int dx = blockX - this.originX;
                        if (dx * dx + dy * dy + dz * dz > radiusSquared) {
                            continue;
                        }
                        BlockState state = source.getBlockState(localX, localY, localZ);
                        if (state.isAir()) {
                            continue;
                        }
                        char encoded;
                        if (!state.getFluidState().isEmpty()) {
                            encoded = HbmNuclearResistanceVolume.FLUID;
                        } else {
                            this.position.set(blockX, blockY, blockZ);
                            encoded = resistanceCode(level, state);
                        }
                        target[localY << 8 | localZ << 4 | localX] = encoded;
                        populated = true;
                    }
                }
            }
            if (populated) {
                int xIndex = chunkX - this.minChunkX;
                int zIndex = chunkZ - this.minChunkZ;
                int yIndex = sectionY - this.minSectionY;
                this.sections[(xIndex * this.chunkDepth + zIndex) * this.sectionCount + yIndex] = target;
            }
        }

        private char resistanceCode(ServerLevel level, BlockState state) {
            boolean vanilla = "minecraft".equals(BuiltInRegistries.BLOCK.getKey(state.getBlock()).getNamespace());
            if (vanilla) {
                Character cached = this.vanillaResistanceCodes.get(state);
                if (cached != null) {
                    return cached;
                }
            }
            float resistance = state.getExplosionResistance(level, this.position, null);
            float adjusted = HbmExplosionMath.mk5MasqueradeResistance(
                    resistance,
                    state.is(Blocks.SANDSTONE),
                    state.is(Blocks.OBSIDIAN),
                    this.stoneResistance
            );
            char encoded = HbmNuclearResistanceVolume.encodeResistance(adjusted);
            if (vanilla) {
                this.vanillaResistanceCodes.put(state, encoded);
            }
            return encoded;
        }

        private int totalSectionAddresses() {
            return this.craterChunks.size() * this.sectionCount;
        }
    }
}
