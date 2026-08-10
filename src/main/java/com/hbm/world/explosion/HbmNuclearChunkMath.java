package com.hbm.world.explosion;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.ChunkPos;

/** Deterministic real-chunk coverage and source-style center-outward ordering. */
final class HbmNuclearChunkMath {
    private HbmNuclearChunkMath() {
    }

    static List<Long> orderedCircle(BlockPos origin, int radius) {
        int safeRadius = Math.max(0, radius);
        int minChunkX = Math.floorDiv(origin.getX() - safeRadius, 16);
        int maxChunkX = Math.floorDiv(origin.getX() + safeRadius, 16);
        int minChunkZ = Math.floorDiv(origin.getZ() - safeRadius, 16);
        int maxChunkZ = Math.floorDiv(origin.getZ() + safeRadius, 16);
        double centerX = origin.getX() + 0.5D;
        double centerZ = origin.getZ() + 0.5D;
        long radiusSquared = (long) safeRadius * safeRadius;

        List<Long> chunks = new ArrayList<>();
        for (int chunkX = minChunkX; chunkX <= maxChunkX; chunkX++) {
            for (int chunkZ = minChunkZ; chunkZ <= maxChunkZ; chunkZ++) {
                double nearestX = clamp(centerX, chunkX << 4, (chunkX << 4) + 15.999999D);
                double nearestZ = clamp(centerZ, chunkZ << 4, (chunkZ << 4) + 15.999999D);
                double dx = centerX - nearestX;
                double dz = centerZ - nearestZ;
                if (dx * dx + dz * dz <= radiusSquared) {
                    chunks.add(ChunkPos.asLong(chunkX, chunkZ));
                }
            }
        }

        chunks.sort(centerOutComparator(origin));
        return chunks;
    }

    static Comparator<Long> centerOutComparator(BlockPos origin) {
        int centerChunkX = origin.getX() >> 4;
        int centerChunkZ = origin.getZ() >> 4;
        return Comparator
                .comparingInt((Long key) -> Math.abs(ChunkPos.getX(key) - centerChunkX)
                        + Math.abs(ChunkPos.getZ(key) - centerChunkZ))
                .thenComparingInt(ChunkPos::getX)
                .thenComparingInt(ChunkPos::getZ);
    }

    static boolean isInsideHorizontalRadius(BlockPos origin, int x, int z, int radius) {
        return HbmChunkSchedulerMath.isInsideHorizontalRadius(origin.getX(), origin.getZ(), x, z, radius);
    }

    private static double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }
}
